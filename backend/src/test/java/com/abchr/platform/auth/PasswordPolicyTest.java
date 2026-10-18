package com.abchr.platform.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.abchr.platform.auth.PasswordPolicy;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Str0ng-Passw0rd", "user@example.com"));
    }

    @Test
    void rejectsShortPassword() {
        ApiException ex = assertThrows(ApiException.class, () -> PasswordPolicy.validate("Ab1!", "u@example.com"));
        assertEquals(ErrorCodes.PASSWORD_POLICY, ex.getCode());
    }

    @Test
    void rejectsNullPassword() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate(null, "u@example.com"));
    }

    @Test
    void rejectsTooLongPassword() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("Aa1!".repeat(40), "u@example.com"));
    }

    @Test
    void rejectsFewerThanThreeCharacterClasses() {
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("alllowercaseletters", "u@example.com"));
        assertThrows(ApiException.class, () -> PasswordPolicy.validate("lowercase12345678", "u@example.com"));
    }

    @Test
    void rejectsPasswordEqualToEmail() {
        assertThrows(ApiException.class,
                () -> PasswordPolicy.validate("Name.Surname@Example1.com", "name.surname@example1.com"));
    }
}
