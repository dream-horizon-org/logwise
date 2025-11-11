package com.logwise.orchestrator.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

@Slf4j
@UtilityClass
public class Encryption {
  private final String UNICODE_FORMAT = "UTF-8";
  private final String ALGORITHM = "AES/CBC/PKCS5PADDING";
  private final String MD5 = "MD5";
  private byte[] digest;

  public static void main(String[] args) {
    if (args.length < 1) {
      log.error("Path to the file containing the text to be encrypted is required.");
      System.exit(1);
    }

    if (System.getenv("ENCRYPTION_KEY") == null
        || System.getenv("ENCRYPTION_IV") == null
        || System.getenv("ENCRYPTION_KEY").isBlank()
        || System.getenv("ENCRYPTION_IV").isBlank()) {
      log.error("Non Empty ENCRYPTION_KEY and ENCRYPTION_IV environment variables must be set.");
      System.exit(1);
    }

    try {
      String plainText = Files.readString(Path.of(args[0]));
      digest = getDigest();
      String encryptedText = encrypt(plainText);
      log.info("Encrypted text:\n\n{}", encryptedText);
    } catch (Exception e) {
      log.error("Error in reading the file: ", e);
      System.exit(1);
    }
  }

  @SneakyThrows
  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) {
      return plainText;
    }
    byte[] input = plainText.trim().getBytes(UNICODE_FORMAT);
    Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
    byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
    int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
    cipher.doFinal(cipherText, ctLength);
    return new String(Base64.encodeBase64(cipherText));
  }

  @SneakyThrows
  public String decrypt(String encrypted) {
    if (encrypted == null || encrypted.isBlank()) {
      return encrypted;
    }
    Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
    byte[] bytes = Base64.decodeBase64(encrypted.getBytes());
    byte[] decrypted = cipher.doFinal(bytes);
    return new String(decrypted, UNICODE_FORMAT);
  }

  @SneakyThrows
  private byte[] getDigest() {
    return MessageDigest.getInstance(MD5)
        .digest(System.getenv("ENCRYPTION_KEY").getBytes(UNICODE_FORMAT));
  }

  @SneakyThrows
  private Cipher getCipher(int mode) {
    SecretKeySpec skc = new SecretKeySpec(digest, "AES");
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    System.out.println("Block size: " + cipher.getBlockSize());
    IvParameterSpec iv =
        new IvParameterSpec(
            Arrays.copyOf(System.getenv("ENCRYPTION_IV").getBytes(), cipher.getBlockSize()));
    cipher.init(mode, skc, iv);
    return cipher;
  }
}
