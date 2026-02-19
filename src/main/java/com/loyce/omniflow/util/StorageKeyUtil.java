package com.loyce.omniflow.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class StorageKeyUtil {

    public static String generate(Long libraryId) {
        String datePath = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("lib/%d/%s/%s", libraryId, datePath, uuid);
    }

    private StorageKeyUtil() {}
}

