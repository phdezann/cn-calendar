package org.phdezann.cn.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppArgs {

    @Parameter(names = "--cache-dir", required = true)
    private File cacheDirectory;

    @Parameter(names = "--config-file", required = true)
    private List<File> configFiles = new ArrayList<>();

    @Parameter(names = "--credentials-json-file", required = true)
    private File credentialsJsonFile;

    @Parameter(names = "--token-folder", required = true)
    private File tokenFolder;

    @Parameter(names = "--event-dir")
    private File eventDir;

    @Parameter(names = "--console")
    private boolean console;

    @Parameter(names = "--trace")
    private boolean trace;

    @Parameter(names = "--init-tokens")
    private boolean initTokens;

    @Parameter(names = "--clear-channels")
    private boolean clearChannels;

}
