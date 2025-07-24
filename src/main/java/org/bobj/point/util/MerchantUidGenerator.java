package org.bobj.point.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MerchantUidGenerator {

    public static String generate(Long userId) {
        return "point_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
            "_" + userId +
            "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
