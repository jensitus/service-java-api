package org.service.b.crypto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

/**
 * Symmetric encryption for secrets stored at rest (e.g. third-party API keys).
 * Uses Spring Security's AES-based text encryptor. The password and hex salt
 * come from configuration and must be overridden in production.
 */
@Service
public class EncryptionService {

    private final TextEncryptor encryptor;

    public EncryptionService(
        @Value("${service.b.org.app.encryptionPassword}") String password,
        @Value("${service.b.org.app.encryptionSalt}") String salt
    ) {
        this.encryptor = Encryptors.text(password, salt);
    }

    public String encrypt(String plaintext) {
        return encryptor.encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
}
