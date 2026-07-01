package com.waterai.consultant.storage;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

final class StorageSupport {

    private StorageSupport() {
    }

    static String sha256(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) > -1) {
            digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
