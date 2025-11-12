package com.example.bankcards.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EncryptionUtilTest {

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Test
    void shouldEncryptAndDecryptText() {
        String original = "1234567812345678";
        String encrypted = encryptionUtil.encrypt(original);
        String decrypted = encryptionUtil.decrypt(encrypted);

        assertEquals(original, decrypted);
    }
}