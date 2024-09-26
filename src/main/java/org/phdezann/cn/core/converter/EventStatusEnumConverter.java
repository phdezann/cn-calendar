package org.phdezann.cn.core.converter;

import static org.phdezann.cn.core.model.EventStatusEnum.CANCELLED;
import static org.phdezann.cn.core.model.EventStatusEnum.CONFIRMED;
import static org.phdezann.cn.core.model.EventStatusEnum.OTHER;

import org.phdezann.cn.core.model.EventStatusEnum;

public class EventStatusEnumConverter {

    public EventStatusEnum convert(String status) {
        return switch (status) {
        case "confirmed" -> CONFIRMED;
        case "cancelled" -> CANCELLED;
        default -> OTHER;
        };
    }

}
