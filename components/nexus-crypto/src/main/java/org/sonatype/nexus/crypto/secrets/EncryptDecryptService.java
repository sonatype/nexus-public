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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

@Named
@Singleton
public class EncryptDecryptService extends ComponentSupport
    implements EncryptDecrypt<String, String, String>
{
  private PbeCipher pbeCipher;

  private final PbeCipherFactory pbeCipherFactory;

  private String secret = "changeme";

  @Inject
  public EncryptDecryptService(final PbeCipherFactory pbeCipherFactory) {
    this.pbeCipherFactory = checkNotNull(pbeCipherFactory);
    initCipher();
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
  public String encrypt(String stringToEncrypt)
  {
    return pbeCipher.encrypt(stringToEncrypt.getBytes(UTF_8)).toPhcString();
  }

  @Override
  public String decrypt(String stringToDecrypt) {
    return new String(pbeCipher.decrypt(EncryptedSecret.parse(stringToDecrypt)), UTF_8);
  }

  public void setSecret(String secret) {
    this.secret = secret;
    initCipher();
  }

  private void initCipher() {
    try {
      SecretEncryptionKey secretKey = new SecretEncryptionKey("key", secret);
      pbeCipher = pbeCipherFactory.create(secretKey);
    }
    catch (Exception e) {
      log.error("Failed to load cipher", e);
      throw new RuntimeException("Unable to initialize encryption.", e);
    }
  }
}
