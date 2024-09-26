package org.phdezann.cn.core.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class EventDateOrTime {
    private final boolean dateValue;
    private final LocalDate date;
    private final ZonedDateTime time;
}
