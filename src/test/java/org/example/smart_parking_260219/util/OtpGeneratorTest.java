package org.example.smart_parking_260219.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpGeneratorTest {

    @Test
    void generateSixDigitCodeReturnsSixNumericCharacters() {
        for (int i = 0; i < 1_000; i++) {
            String otp = OtpGenerator.generateSixDigitCode();

            assertTrue(otp.matches("\\d{6}"));
        }
    }
}
