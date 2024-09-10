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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.crypto.LegacyCipherFactory;
import org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.PhraseService;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.maven.MavenCipher;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

@Named
@Singleton
public class SecretsServiceImpl
    extends ComponentSupport
    implements SecretsFactory, SecretsService
{
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

  @Inject
  public SecretsServiceImpl(
      final LegacyCipherFactory legacyCipherFactory,
      final MavenCipher mavenCipher,
      final PhraseService phraseService,
      final PbeCipherFactory pbeCipherFactory,
      final SecretsStore secretsStore,
      final EncryptionKeySource encryptionKeySource,
      final DatabaseCheck databaseCheck,
      @Named("${nexus.mybatis.cipher.password:-changeme}") final String legacyPassword,
      @Named("${nexus.mybatis.cipher.salt:-changeme}") final String legacySalt,
      @Named("${nexus.mybatis.cipher.iv:-0123456789ABCDEF}") final String legacyIv)
  {
    this.legacyCipher = checkNotNull(legacyCipherFactory).create(legacyPassword, legacySalt, legacyIv);//NOSONAR
    this.mavenCipher = checkNotNull(mavenCipher);//NOSONAR
    this.phraseService = checkNotNull(phraseService);//NOSONAR
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
      final DatabaseCheck databaseCheck) throws CipherException
  {
    this(legacyCipherFactory, mavenCipher, phraseService, pbeCipherFactory, secretsStore, encryptionKeySource,
        databaseCheck, "changeme", "changeme", "0123456789ABCDEF");
  }

  @Override
  public Secret from(final String token) {
    return new SecretImpl(token);
  }

  @Override
  public Secret encrypt(final String purpose, final char[] secret, @Nullable final String userId)
      throws CipherException
  {
    return this.encrypt(purpose, secret, this::encryptWithLegacyPBE, userId);
  }

  @Override
  public Secret encryptMaven(final String purpose, final char[] secret, @Nullable final String userid)
      throws CipherException
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

    //defaulting key_id as NULL, since NULL means legacy encryption
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
    if (customKey.isPresent()) {
      return cipherFactory.create(customKey.get()).encrypt(toBytes(secret)).toPhcString();
    }

    return cipherFactory.create(defaultKey).encrypt(toBytes(secret)).toPhcString();
  }

  private char[] decrypt(final String token) throws CipherException {
    if (isLegacyToken(token)) {
      return decryptLegacy(token);
    }

    Optional<SecretData> secret = secretsStore.read(parseToken(token));

    if (!secret.isPresent()) {
      throw new CipherException("Unable find secret for the specified token");
    }

    SecretData data = secret.get();

    Optional<SecretEncryptionKey> secretKey = Optional.ofNullable(data.getKeyId())
        .flatMap(encryptionKeySource::getKey);

    if (secretKey.isPresent()) {
      return toChars(cipherFactory.create(secretKey.get()).decrypt(EncryptedSecret.parse(data.getSecret())));
    }

    if (data.getKeyId() != null) {
      log.warn("key id '{}' present in record but not found in existing secrets, secret id : {}", data.getKeyId(),
          data.getId());
      throw new CipherException(format("unable to find secret key with id '%s'.", data.getKeyId()));
    }

    return toChars(cipherFactory.create(defaultKey).decrypt(EncryptedSecret.parse(data.getSecret())));
  }

  /**
   * @deprecated this is used to encrypt legacy values with {@link LegacyCipherFactory.PbeCipher} until the system
   * migrates to the new version.
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
   * version
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
      return SecretsServiceImpl.this.decrypt(tokenId);
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
