package com.abchr.platform.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FieldCryptoTest {

    private static final String KEY = "a-test-key-that-is-longer-than-32-characters";

    @Test
    void roundTripsValues() {
        FieldCrypto crypto = new FieldCrypto(KEY);
        String stored = crypto.encrypt("123456789012");
        assertNotEquals("123456789012", stored);
        assertEquals("123456789012", crypto.decrypt(stored));
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        FieldCrypto crypto = new FieldCrypto(KEY);
        assertNotEquals(crypto.encrypt("ABCDE1234F"), crypto.encrypt("ABCDE1234F"));
    }

    @Test
    void emptyValuesStayEmpty() {
        FieldCrypto crypto = new FieldCrypto(KEY);
        assertNull(crypto.encrypt(null));
        assertNull(crypto.encrypt(""));
        assertNull(crypto.decrypt(null));
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String stored = new FieldCrypto(KEY).encrypt("secret");
        FieldCrypto other = new FieldCrypto("another-key-that-is-longer-than-32-chars");
        assertThrows(IllegalStateException.class, () -> other.decrypt(stored));
    }

    @Test
    void rejectsShortKey() {
        assertThrows(IllegalStateException.class, () -> new FieldCrypto("short"));
        assertThrows(IllegalStateException.class, () -> new FieldCrypto(""));
    }

    @Test
    void masksAllButLastFour() {
        assertEquals("********9012", FieldCrypto.mask("123456789012"));
        assertEquals("***", FieldCrypto.mask("123"));
        assertNull(FieldCrypto.mask(null));
    }
}
