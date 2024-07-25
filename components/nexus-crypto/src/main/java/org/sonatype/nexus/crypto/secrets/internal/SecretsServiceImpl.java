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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.PbeCipherFactory;
import org.sonatype.nexus.crypto.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.crypto.secrets.SecretsService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.Base64Variants;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class SecretsServiceImpl
    implements SecretsFactory, SecretsService
{
  private static final Base64Variant BASE_64 = Base64Variants.getDefaultVariant();

  /**
   * @deprecated this is used to decrypt legacy stored values, or encrypt them until the system has migrated
   */
  @Deprecated
  private final PbeCipher legacyCipher;

  @Inject
  public SecretsServiceImpl(
      final PbeCipherFactory cipherFactory,
      @Named("${nexus.mybatis.cipher.password:-changeme}") final String legacyPassword,
      @Named("${nexus.mybatis.cipher.salt:-changeme}") final String legacySalt,
      @Named("${nexus.mybatis.cipher.iv:-0123456789ABCDEF}") final String legacyIv) throws Exception
  {
    this.legacyCipher = checkNotNull(cipherFactory).create(legacyPassword, legacySalt, legacyIv);
  }

  @VisibleForTesting
  SecretsServiceImpl(final PbeCipherFactory cipherFactory) throws Exception {
    this(cipherFactory, "changeme", "changeme", "0123456789ABCDEF");
  }

  @Override
  public Secret from(final String token) {
    return new SecretImpl(token);
  }

  @Override
  public Secret encrypt(final String purpose, final char[] secret, final String userId) {
    return new SecretImpl(encryptLegacy(secret));
  }

  private char[] decrypt(final String token) {
    return decryptLegacy(token);
  }

  /**
   * @deprecated this is used to encrypt legacy values until the system migrates to the new version.
   */
  @Deprecated
  private String encryptLegacy(final char[] secret) {
    if (secret == null) {
      return null;
    }
    return BASE_64.encode(legacyCipher.encrypt(toBytes(secret)));
  }

  /**
   * @deprecated this is used to decrypt legacy stored values.
   */
  @Deprecated
  private char[] decryptLegacy(final String secret) {
    if (secret == null) {
      return null;
    }
    return toChars(legacyCipher.decrypt(BASE_64.decode(secret)));
  }

  private static byte[] toBytes(final char[] chars) {
    CharBuffer cbuffer = CharBuffer.wrap(chars);
    return StandardCharsets.UTF_8.encode(cbuffer).array();
  }

  private static char[] toChars(final byte[] chars) {
    ByteBuffer bbuffer = ByteBuffer.wrap(chars);
    return StandardCharsets.UTF_8.decode(bbuffer).array();
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
    public char[] decrypt() {
      return SecretsServiceImpl.this.decrypt(tokenId);
    }
  }
}
