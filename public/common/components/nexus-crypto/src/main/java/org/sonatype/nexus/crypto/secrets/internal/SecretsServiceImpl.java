/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.crypto.secrets.internal;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.crypto.LegacyCipherFactory;
import org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.PhraseService;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.maven.MavenCipher;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component("default")
@Primary
@ManagedLifecycle(phase = SERVICES)
public class SecretsServiceImpl
    implements SecretsFactory, SecretsService, EventAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final Base64Variant BASE_64 = Base64Variants.getDefaultVariant();

  private static final String UNDERSCORE = "_";

  private static final String UNDERSCORE_ID = UNDERSCORE + "%d";

  private static final String DEFAULT_PASSPHRASE = "CMMDwoV";

  /**
   * @deprecated this is used to decrypt legacy stored values, or encrypt them until the system has migrated
   */
  @Deprecated
  private final PbeCipher legacyCipher;

  /**
   * @deprecated this is used to decrypt legacy stored values that were previously encrypted using {@link MavenCipher}
   */
  @Deprecated
  private final MavenCipher mavenCipher;

  /**
   * @deprecated this is used to get the right passphrase to decrypt a legacy secret, used by {@link MavenCipher}
   */
  @Deprecated
  private final PhraseService phraseService;

  private final PbeCipherFactory cipherFactory;

  private final SecretsStore secretsStore;

  private final EncryptionKeySource encryptionKeySource;

  private final DatabaseCheck databaseCheck;

  private final SecretEncryptionKey defaultKey;

  @Autowired
  public SecretsServiceImpl(
      final LegacyCipherFactory legacyCipherFactory,
      final MavenCipher mavenCipher,
      final PhraseService phraseService,
      final PbeCipherFactory pbeCipherFactory,
      final SecretsStore secretsStore,
      final EncryptionKeySource encryptionKeySource,
      final DatabaseCheck databaseCheck,
      @Value("${nexus.mybatis.cipher.password:changeme}") final String legacyPassword,
      @Value("${nexus.mybatis.cipher.salt:changeme}") final String legacySalt,
      @Value("${nexus.mybatis.cipher.iv:0123456789ABCDEF}") final String legacyIv,
      @Value(FeatureFlags.NEXUS_SECURITY_FIPS_ENABLED_NAMED_VALUE) final boolean fipsEnabled)
  {
    if (fipsEnabled) {
      if (!databaseCheck.isAtLeast(SECRETS_MIGRATION_VERSION)) {
        throw new IllegalStateException("FIPS mode requires migration to the new secrets service");
      }
      this.legacyCipher = null; // FIPS mode does not support legacy ciphers
    }
    else {
      this.legacyCipher = checkNotNull(legacyCipherFactory).create(legacyPassword, legacySalt, legacyIv);// NOSONAR
    }

    this.mavenCipher = checkNotNull(mavenCipher);// NOSONAR
    this.phraseService = checkNotNull(phraseService);// NOSONAR
    this.cipherFactory = checkNotNull(pbeCipherFactory);
    this.secretsStore = checkNotNull(secretsStore);
    this.encryptionKeySource = checkNotNull(encryptionKeySource);
    this.databaseCheck = checkNotNull(databaseCheck);
    this.defaultKey = new SecretEncryptionKey(null, legacyPassword);
  }

  @VisibleForTesting
  SecretsServiceImpl(
      final LegacyCipherFactory legacyCipherFactory,
      final MavenCipher mavenCipher,
      final PhraseService phraseService,
      final PbeCipherFactory pbeCipherFactory,
      final SecretsStore secretsStore,
      final EncryptionKeySource encryptionKeySource,
      final DatabaseCheck databaseCheck,
      final boolean fipsEnabled) throws CipherException
  {
    this(legacyCipherFactory, mavenCipher, phraseService, pbeCipherFactory, secretsStore, encryptionKeySource,
        databaseCheck, "changeme", "changeme", "0123456789ABCDEF", fipsEnabled);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ActiveKeyChangeEvent event) {
    log.debug("Received a secret key change request");
    encryptionKeySource.setActiveKey(event.getNewKeyId());
  }

  @Override
  public Secret from(final String token) {
    return new SecretImpl(token);
  }

  @Override
  public Secret encrypt(
      final String purpose,
      final char[] secret,
      @Nullable final String userId) throws CipherException
  {
    return this.encrypt(purpose, secret, this::encryptWithLegacyPBE, userId);
  }

  @Override
  public Secret encryptMaven(
      final String purpose,
      final char[] secret,
      @Nullable final String userid) throws CipherException
  {
    return this.encrypt(purpose, secret, this::encryptWithMavenCipher, userid);
  }

  private Secret encrypt(
      final String purpose,
      final char[] secret,
      final Function<char[], String> legacyEncryption,
      final String userId) throws CipherException
  {
    if (!databaseCheck.isAtLeast(SECRETS_MIGRATION_VERSION)) {
      return new SecretImpl(legacyEncryption.apply(secret));
    }

    Optional<SecretEncryptionKey> customKey = encryptionKeySource.getActiveKey();

    // defaulting key_id as NULL, since NULL means legacy encryption
    String activeKeyId = null;

    if (customKey.isPresent()) {
      activeKeyId = customKey.get().getId();
    }

    int tokenId = secretsStore.create(purpose, activeKeyId, doEncrypt(secret, customKey), userId);

    return new SecretImpl(format(UNDERSCORE_ID, tokenId));
  }

  @Override
  public void remove(final Secret secret) {
    checkNotNull(secret);

    if (isLegacyToken(secret.getId())) {
      log.debug("legacy tokens are not stored, deletion not needed.");
      return;
    }

    secretsStore.delete(parseToken(secret.getId()));
  }

  private String doEncrypt(final char[] secret, final Optional<SecretEncryptionKey> customKey) throws CipherException {
    SecretEncryptionKey keyToUse = customKey.orElse(defaultKey);
    PbeCipherFactory.PbeCipher pbeCipher = cipherFactory.create(keyToUse);
    EncryptedSecret encryptedSecret = pbeCipher.encrypt(toBytes(secret));

    return encryptedSecret.toPhcString();
  }

  @Override
  public boolean isReEncryptRequired() {
    return encryptionKeySource.getActiveKey()
        .map(SecretEncryptionKey::getId)
        .map(secretsStore::existWithDifferentKeyId)
        .orElse(false);
  }

  @Override
  public void reEncrypt(SecretData secretData, String keyId) throws CipherException {
    Integer secretId = secretData.getId();
    String currentSecret = secretData.getSecret();
    char[] decrypted = this.doDecrypt(secretData);
    String reEncrypted = this.doEncrypt(decrypted, encryptionKeySource.getKey(keyId));
    secretsStore.update(secretId, currentSecret, keyId, reEncrypted);
    log.trace("Secret id: {} successfully re-encrypted", secretId);
  }

  @Override
  public String exportEncrypted(final String secretId) {
    if (secretId == null) {
      return null;
    }

    // If it's a legacy token (doesn't start with _), just return it as-is
    if (isLegacyToken(secretId)) {
      log.debug("Exporting legacy encrypted secret");
      return secretId;
    }

    // For modern secrets, look up the encrypted value from the store
    try {
      SecretData data = secretsStore.read(parseToken(secretId))
          .orElse(null);
      if (data == null) {
        log.warn("Unable to find secret for token {} during export", secretId);
        return null;
      }
      return data.getSecret();
    }
    catch (Exception e) {
      log.warn("Error exporting encrypted secret for token {}", secretId, e);
      return null;
    }
  }

  @Override
  public Secret importEncrypted(
      final String purpose,
      final String encryptedValue,
      final String userId) throws CipherException
  {
    return importEncrypted(purpose, encryptedValue, userId, null);
  }

  @Override
  public Secret importEncrypted(
      final String purpose,
      final String encryptedValue,
      final String userId,
      final String decryptionCipherPassword) throws CipherException
  {
    if (encryptedValue == null) {
      return null;
    }

    // PHC format strings start with $ and are modern secrets that need to be stored
    // Legacy encrypted values don't start with $ or _, just wrap them
    if (isLegacyToken(encryptedValue) && !encryptedValue.startsWith("$")) {
      log.debug("Importing legacy encrypted secret");
      return new SecretImpl(encryptedValue);
    }

    // Decrypt and re-encrypt to generate new values with fresh encryption parameters
    // This ensures:
    // 1) No security concerns when copying secrets between instances
    // 2) Secrets are properly migrated when instances use different encryption keys
    try {
      SecretEncryptionKey decryptionKey = defaultKey;

      // Use the migration cipher password if provided, otherwise use default key
      if (decryptionCipherPassword != null && !decryptionCipherPassword.isEmpty()) {
        log.debug("Using migration cipher password for secret import");
        decryptionKey = new SecretEncryptionKey(null, decryptionCipherPassword);
      }

      return encrypt(purpose, toChars(cipherFactory.create(decryptionKey, encryptedValue).decrypt()), null, userId);
    }
    catch (Exception e) {
      throw new CipherException("Failed to import encrypted secret", e);
    }
  }

  @Override
  public String exportEncryptedWithPassword(final String secretId, final String customPassword) throws CipherException {
    checkNotNull(secretId, "secretId cannot be null");
    checkNotNull(customPassword, "customPassword cannot be null");

    if (customPassword.isEmpty()) {
      throw new CipherException("Custom password cannot be empty");
    }

    // Handle legacy tokens - return as-is since they're already encrypted
    if (isLegacyToken(secretId)) {
      log.debug("Exporting legacy encrypted secret");
      return secretId;
    }

    try {
      // 1. Get the secret from storage
      SecretData data = secretsStore.read(parseToken(secretId))
          .orElseThrow(() -> new CipherException("Secret not found for token: " + secretId));

      // 2. Decrypt using instance key
      char[] plaintext = doDecrypt(data);

      // 3. Re-encrypt with custom password
      SecretEncryptionKey exportKey = new SecretEncryptionKey(null, customPassword);
      PbeCipherFactory.PbeCipher cipher = cipherFactory.create(exportKey);
      String encrypted = cipher.encrypt(toBytes(plaintext)).toPhcString();

      log.debug("Exported secret {} with custom password", secretId);
      return encrypted;
    }
    catch (Exception e) {
      throw new CipherException("Failed to export secret with custom password: " + secretId, e);
    }
  }

  @Override
  public String encryptPlaintextWithPassword(
      final String plaintext,
      final String customPassword) throws CipherException
  {
    checkNotNull(plaintext, "plaintext cannot be null");
    checkNotNull(customPassword, "customPassword cannot be null");

    if (customPassword.isEmpty()) {
      throw new CipherException("Custom password cannot be empty");
    }

    try {
      SecretEncryptionKey exportKey = new SecretEncryptionKey(null, customPassword);
      PbeCipherFactory.PbeCipher cipher = cipherFactory.create(exportKey);
      return cipher.encrypt(plaintext.getBytes(StandardCharsets.UTF_8)).toPhcString();
    }
    catch (Exception e) {
      throw new CipherException("Failed to encrypt plaintext with custom password", e);
    }
  }

  @Override
  public String encryptBytesWithPassword(
      final byte[] plaintext,
      final String customPassword) throws CipherException
  {
    checkNotNull(plaintext, "plaintext cannot be null");
    checkNotNull(customPassword, "customPassword cannot be null");

    if (customPassword.isEmpty()) {
      throw new CipherException("Custom password cannot be empty");
    }

    try {
      SecretEncryptionKey exportKey = new SecretEncryptionKey(null, customPassword);
      PbeCipherFactory.PbeCipher cipher = cipherFactory.create(exportKey);
      return cipher.encrypt(plaintext).toPhcString();
    }
    catch (Exception e) {
      throw new CipherException("Failed to encrypt bytes with custom password", e);
    }
  }

  @Override
  public boolean exists(final String secretId) {
    if (secretId == null || secretId.isEmpty()) {
      return false;
    }

    if (isLegacyToken(secretId)) {
      log.debug("Legacy tokens are not stored, cannot check existence");
      return false;
    }

    try {
      return secretsStore.read(parseToken(secretId)).isPresent();
    }
    catch (IllegalArgumentException e) {
      log.debug("Invalid secret ID format: {}", secretId);
      return false;
    }
    catch (Exception e) {
      log.warn("Failed to check secret existence for {}, treating as non-existent", secretId, e);
      return false;
    }
  }

  private char[] doDecrypt(final String token) throws CipherException {
    if (isLegacyToken(token)) {
      return decryptLegacy(token);
    }

    SecretData data = secretsStore.read(parseToken(token))
        .orElseThrow(() -> new CipherException("Unable to find secret for the specified token"));

    return doDecrypt(data);
  }

  private char[] doDecrypt(final SecretData data) {
    // First check if a key ID is specified but not found
    if (data.getKeyId() != null && !encryptionKeySource.getKey(data.getKeyId()).isPresent()) {
      throw new CipherException(format("unable to find secret key with id '%s'.", data.getKeyId()));
    }
    SecretEncryptionKey key = Optional.ofNullable(data.getKeyId())
        .flatMap(encryptionKeySource::getKey)
        .orElse(defaultKey);

    PbeCipherFactory.PbeCipher pbeCipher = cipherFactory.create(key, data.getSecret());
    char[] decrypted = toChars(pbeCipher.decrypt());

    // If algorithm needs migration, re-encrypt with current algorithm
    if (!pbeCipher.isDefaultCipher()) {
      reEncryptWithCurrentAlgorithm(data, decrypted);
    }
    return decrypted;
  }

  private void reEncryptWithCurrentAlgorithm(
      final SecretData data,
      final char[] valueToEncrypt) throws CipherException
  {
    Optional<SecretEncryptionKey> customKey = Optional.ofNullable(data.getKeyId()).flatMap(encryptionKeySource::getKey);

    String newEncrypted = doEncrypt(valueToEncrypt, customKey);

    secretsStore.update(data.getId(), data.getSecret(), data.getKeyId(), newEncrypted);
  }

  /**
   * @deprecated this is used to encrypt legacy values with {@link LegacyCipherFactory.PbeCipher} until the system
   *             migrates to the new version.
   */
  @Deprecated
  private String encryptWithLegacyPBE(final char[] secret) {
    if (secret == null) {
      return null;
    }
    return BASE_64.encode(legacyCipher.encrypt(toBytes(secret)));
  }

  /**
   * @deprecated this is used to encrypt legacy values with {@link MavenCipher} until the system migrates to the new
   *             version
   */
  @Deprecated
  private String encryptWithMavenCipher(final char[] secret) {
    String raw = new String(secret);

    if (mavenCipher.isPasswordCipher(raw)) {
      return raw;
    }

    String encoded = mavenCipher.encrypt(raw, phraseService.getPhrase(DEFAULT_PASSPHRASE));

    if (encoded != null && !encoded.equals(raw)) {
      phraseService.mark(encoded);
    }

    return encoded;
  }

  /**
   * @deprecated this is used to decrypt legacy stored values.
   */
  @Deprecated
  private char[] decryptLegacy(final String secret) {
    if (secret == null) {
      return null;
    }

    if (mavenCipher.isPasswordCipher(secret)) {
      return decryptWithMavenCipher(secret);
    }

    return toChars(legacyCipher.decrypt(BASE_64.decode(secret)));
  }

  /**
   * @deprecated this is used to decrypt legacy stored values that were previously encrypted with {@link MavenCipher}
   */
  @Deprecated
  private char[] decryptWithMavenCipher(final String secret) {
    if (phraseService.usesLegacyEncoding(secret)) {
      return mavenCipher.decryptChars(secret, DEFAULT_PASSPHRASE);
    }
    return mavenCipher.decryptChars(secret, phraseService.getPhrase(DEFAULT_PASSPHRASE));
  }

  private int parseToken(final String token) {
    checkArgument(token.startsWith(UNDERSCORE), "Unexpected token");
    return Integer.parseInt(token.substring(1));
  }

  private static boolean isLegacyToken(final String token) {
    return !token.startsWith(UNDERSCORE);
  }

  private static byte[] toBytes(final char[] chars) {
    ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
    byte[] bytes = new byte[byteBuffer.limit()];
    byteBuffer.get(bytes);
    return bytes;
  }

  private static char[] toChars(final byte[] bytes) {
    CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
    char[] chars = new char[charBuffer.limit()];
    charBuffer.get(chars);
    return chars;
  }

  /*
   * Jackson annotations to prevent serialization in case of accidental return
   */
  @JsonIgnoreType
  private class SecretImpl
      implements Secret
  {
    @JsonIgnore
    private final String tokenId;

    private SecretImpl(final String token) {
      this.tokenId = token;
    }

    @Override
    public String getId() {
      return tokenId;
    }

    @Override
    public char[] decrypt() throws CipherException {
      boolean isLegacy = isLegacyToken(tokenId);

      try {
        return SecretsServiceImpl.this.doDecrypt(tokenId);
      }
      catch (CipherException | IllegalArgumentException | NullPointerException e) {
        if (isLegacy) {
          log.debug("Failed to decrypt legacy secret, tokenId will be returned as secret");
          return tokenId.toCharArray();
        }

        throw e;
      }
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SecretImpl other = (SecretImpl) o;
      return Objects.equals(tokenId, other.tokenId);
    }
  }
}
