package org.phdezann.cn.core;

import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class GoogleClient {

    public static final String APPLICATION_NAME = "cn-calendar";

    private final AppArgs appArgs;
    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

    private List<String> scopes() {
        var scopes = new ArrayList<String>();
        scopes.add(CalendarScopes.CALENDAR);
        return scopes;
    }

    public Credential getCredentials() {
        try {
            GoogleClientSecrets clientSecrets = loadSecrets();

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow //
                    .Builder(newTrustedTransport(), gsonFactory, clientSecrets, scopes()) //
                    .setDataStoreFactory(new FileDataStoreFactory(appArgs.getTokenDir())) //
                    .setAccessType("offline") //
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder() //
                    .setPort(8888) //
                    .build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private GoogleClientSecrets loadSecrets() {
        try {
            return GoogleClientSecrets.load(gsonFactory, new FileReader(appArgs.getCredentialsJsonFile()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void renewTokens() {
        FileUtils.deleteQuietly(appArgs.getTokenDir());
        new GoogleClient(appArgs).getCredentials();

        var secrets = loadSecrets().getInstalled();
        var clientId = secrets.getClientId();
        var projectId = secrets.getUnknownKeys().get("project_id");

        var consoleUrl = String.format("https://console.cloud.google.com/apis/credentials/oauthclient/%s?project=%s",
                clientId, projectId);
        log.info("Go to {} to manage oauth client, download credentials.json, and more.", consoleUrl);
    }

}
