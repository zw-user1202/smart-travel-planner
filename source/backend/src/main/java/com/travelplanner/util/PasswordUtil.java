package com.travelplanner.util;

import cn.hutool.crypto.digest.DigestUtil;

public final class PasswordUtil {

    private static final String SALT = "travel-planner-salt";

    private PasswordUtil() {
    }

    public static String hash(String raw) {
        return DigestUtil.sha256Hex(SALT + raw);
    }

    public static boolean matches(String raw, String hashed) {
        return hashed != null && hashed.equals(hash(raw));
    }
}
