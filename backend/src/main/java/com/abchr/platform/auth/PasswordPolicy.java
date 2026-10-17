package com.abchr.platform.auth;

import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static void validate(String password, String email) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw ApiException.rule(ErrorCodes.PASSWORD_POLICY,
                    "Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
        }
        int classes = 0;
        if (password.chars().anyMatch(Character::isLowerCase)) {
            classes++;
        }
        if (password.chars().anyMatch(Character::isUpperCase)) {
            classes++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            classes++;
        }
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            classes++;
        }
        if (classes < 3) {
            throw ApiException.rule(ErrorCodes.PASSWORD_POLICY,
                    "Password must mix at least three of: lowercase, uppercase, digits, symbols");
        }
        if (email != null && password.equalsIgnoreCase(email)) {
            throw ApiException.rule(ErrorCodes.PASSWORD_POLICY, "Password must not be the same as the email");
        }
    }
}
