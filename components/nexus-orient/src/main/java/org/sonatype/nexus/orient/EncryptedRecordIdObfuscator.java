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
package org.sonatype.nexus.orient;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.io.Hex;
import org.sonatype.nexus.crypto.CryptoHelper;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encrypted {@link RecordIdObfuscator}.
 *
 * @since 3.0
 */
@Named("encrypted")
@Singleton
public class EncryptedRecordIdObfuscator
  extends RecordIdObfuscatorSupport
{
  private static final String CPREFIX = "${nexus.orient.encryptedRecordIdObfuscator";

  private static final String TRANSFORMATION = "DES/CBC/NoPadding";

  private final CryptoHelper crypto;

  private final AlgorithmParameterSpec paramSpec;

  private final SecretKey secretKey;

  @Inject
  public EncryptedRecordIdObfuscator(final CryptoHelper crypto,
                                     @Named(CPREFIX + ".password:-changeme}") final String password,
                                     @Named(CPREFIX + ".salt:-changeme}") final String salt,
                                     @Named(CPREFIX + ".iv:-0123456789ABCDEF}") final String iv)
      throws Exception
  {
    this.crypto = checkNotNull(crypto);

    checkNotNull(iv);
    this.paramSpec = new IvParameterSpec(Hex.decode(iv));

    SecretKeyFactory factory = crypto.createSecretKeyFactory("PBKDF2WithHmacSHA1");
    checkNotNull(password);
    checkNotNull(salt);
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 1024, 64);

    SecretKey tmp = factory.generateSecret(spec);
    this.secretKey = new SecretKeySpec(tmp.getEncoded(), "DES");
  }

  @Override
  protected String doEncode(final OClass type, final ORID rid) throws Exception {
    Cipher cipher = crypto.createCipher(TRANSFORMATION);
    // rid is 10 byte long, need to be in multiples of 8 for cipher
    byte[] plain = ByteBuffer.allocate(16).put(rid.toStream()).array();
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
    byte[] encrypted = cipher.doFinal(plain);
    return Hex.encode(encrypted);
  }

  @Override
  protected ORID doDecode(final OClass type, final String encoded) throws Exception {
    Cipher cipher = crypto.createCipher(TRANSFORMATION);
    byte[] encrypted = Hex.decode(encoded);
    cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
    byte[] plain = cipher.doFinal(encrypted);
    return new ORecordId().fromStream(plain);
  }
}
