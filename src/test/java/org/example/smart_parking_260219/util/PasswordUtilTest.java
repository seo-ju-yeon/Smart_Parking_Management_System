package org.example.smart_parking_260219.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashPassword_generatesBCryptHash() {
        String plainPassword = "test1234";

        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        assertNotNull(hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
        assertEquals(12, PasswordUtil.getWorkFactor(hashedPassword));
    }

    @Test
    void checkPassword_returnsTrue_whenPasswordMatches() {
        String plainPassword = "test1234";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        boolean result = PasswordUtil.checkPassword(plainPassword, hashedPassword);

        assertTrue(result);
    }

    @Test
    void checkPassword_returnsFalse_whenPasswordDoesNotMatch() {
        String plainPassword = "test1234";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        boolean result = PasswordUtil.checkPassword("wrong1234", hashedPassword);

        assertFalse(result);
    }

    @Test
    void hashPassword_generatesDifferentHashForSamePassword() {
        String plainPassword = "test1234";

        String hash1 = PasswordUtil.hashPassword(plainPassword);
        String hash2 = PasswordUtil.hashPassword(plainPassword);

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtil.checkPassword(plainPassword, hash1));
        assertTrue(PasswordUtil.checkPassword(plainPassword, hash2));
    }

    @Test
    void hashPassword_throwsException_whenPasswordIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword("   "));
    }

    @Test
    void checkPassword_returnsFalse_whenInputIsInvalid() {
        assertFalse(PasswordUtil.checkPassword(null, "$2a$12$dummy"));
        assertFalse(PasswordUtil.checkPassword("", "$2a$12$dummy"));
        assertFalse(PasswordUtil.checkPassword("test1234", null));
        assertFalse(PasswordUtil.checkPassword("test1234", ""));
    }
}
