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
package org.sonatype.nexus.crypto.secrets;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.springframework.stereotype.Component;

@Component
public class EncryptDecryptService
    implements EncryptDecrypt<String, String, String>
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final PbeCipherFactory pbeCipherFactory;

  private String secret = "changeme";

  @Autowired
  public EncryptDecryptService(final PbeCipherFactory pbeCipherFactory) {
    this.pbeCipherFactory = checkNotNull(pbeCipherFactory);
  }

  @Override
  public String encode(String encryptedString) {
    return new String(Base64.getEncoder().encode(encryptedString.getBytes(UTF_8)), UTF_8);
  }

  @Override
  public String decode(String encodedString) {
    return new String(Base64.getDecoder().decode(encodedString.getBytes(UTF_8)), UTF_8);
  }

  @Override
  public String encrypt(String stringToEncrypt) {
    try {
      SecretEncryptionKey secretKey = new SecretEncryptionKey(secret);
      PbeCipher pbeCipher = pbeCipherFactory.create(secretKey);
      return pbeCipher.encrypt(stringToEncrypt.getBytes(UTF_8)).toPhcString();
    }
    catch (Exception e) {
      log.error("Failed to encrypt", e);
      throw new RuntimeException("Unable to encrypt.", e);
    }
  }

  @Override
  public String decrypt(String stringToDecrypt) {
    try {
      SecretEncryptionKey secretKey = new SecretEncryptionKey(secret);
      PbeCipher pbeCipher = pbeCipherFactory.create(secretKey, stringToDecrypt);
      return new String(pbeCipher.decrypt(), UTF_8);
    }
    catch (Exception e) {
      log.error("Failed to decrypt", e);
      throw new RuntimeException("Unable to decrypt.", e);
    }
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
}
