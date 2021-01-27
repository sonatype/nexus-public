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
package org.sonatype.nexus.repository.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Unit tests for {@link GpgUtils}
 */
public class GpgUtilsTest
{
  private static final String GPG_PUBLIC_KEY_PATH = "RPM-GPG-KEY-nxrmtest.public.key";

  private static final String GPG_SECRET_KEY_PATH = "RPM-GPG-KEY-nxrmtest.secret.key";

  private static final String GPG_SECRET_KEY_NO_PASS_PATH = "RPM-GPG-KEY-NO-PASS-nxrmtest.secret.key";

  private static final String GPG_KEY_PASSPHRASE = "admin123";

  private String secretKey;

  private String secretKeyNoPass;

  private String publicKey;

  // used to fix java.security.NoSuchProviderException: no such provider: BC
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Before
  public void init() throws IOException {
    try (InputStream secretIs = getClass().getResourceAsStream(GPG_SECRET_KEY_PATH);
         InputStream secretNoPassIs = getClass().getResourceAsStream(GPG_SECRET_KEY_NO_PASS_PATH);
         InputStream publicIs = getClass().getResourceAsStream(GPG_PUBLIC_KEY_PATH)) {
      secretKey = IOUtils.toString(secretIs, UTF_8);
      secretKeyNoPass = IOUtils.toString(secretNoPassIs, UTF_8);
      publicKey = IOUtils.toString(publicIs, UTF_8);
    }
  }

  @Test
  public void testIsConfigured() {
    assertTrue(GpgUtils.isConfigured(secretKey, GPG_KEY_PASSPHRASE));
  }

  @Test
  public void testWithIncorrectPassphrase() {
    assertFalse(GpgUtils.isConfigured(secretKey, "incorrectPass"));
  }

  @Test
  public void testWithIncorrectSecretKey() {
    String incorrectSecretKey =
        "-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
        "\n" +
        "-----END PGP PRIVATE KEY BLOCK-----\n";
    assertFalse(GpgUtils.isConfigured(incorrectSecretKey, StringUtils.EMPTY));
  }

  @Test
  public void testWithEmptySecretKey() {
    String incorrectSecretKey = "";
    assertFalse(GpgUtils.isConfigured(incorrectSecretKey, StringUtils.EMPTY));
  }

  @Test
  public void testWithEmptyPassphrase() {
    assertTrue(GpgUtils.isConfigured(secretKeyNoPass, null));
  }

  @Test
  public void testIsSignedExternal() throws Exception {
    String msgToSign = "Hello World";
    byte[] signedData = GpgUtils.signExternal(IOUtils.toInputStream(msgToSign, UTF_8), secretKey, GPG_KEY_PASSPHRASE);
    assertTrue(verify(msgToSign, signedData));
  }

  private boolean verify(final String msgToSign, final byte[] signedData) throws IOException, PGPException
  {
    try (ArmoredInputStream in = new ArmoredInputStream(new ByteArrayInputStream(signedData))) {
      PGPObjectFactory objectFactory = new PGPObjectFactory(in, new JcaKeyFingerprintCalculator());
      PGPSignatureList pgpSignatures = (PGPSignatureList) objectFactory.nextObject();
      PGPSignature signature = pgpSignatures.get(0);

      PGPPublicKey publicKey = buildPublicKey();
      signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
      signature.update(msgToSign.getBytes(UTF_8));
      return signature.verify();
    }
  }

  private PGPPublicKey buildPublicKey() throws IOException {
    try (InputStream publicIs = IOUtils.toInputStream(publicKey, UTF_8)) {
      PGPPublicKeyRingCollection pgpSec = new PGPPublicKeyRingCollection(
          PGPUtil.getDecoderStream(publicIs),
          new JcaKeyFingerprintCalculator());

      Iterator<PGPPublicKeyRing> keyRings = pgpSec.getKeyRings();
      while (keyRings.hasNext()) {
        PGPPublicKeyRing keyRing = keyRings.next();

        Iterator<PGPPublicKey> keys = keyRing.getPublicKeys();
        while (keys.hasNext()) {
          PGPPublicKey key = keys.next();

          if (key.isEncryptionKey()) {
            return key;
          }
        }
      }
    }
    catch (PGPException ex) {
      throw new RuntimeException(ex);
    }

    throw new IllegalStateException("Can't find encryption key in key ring.");
  }
}
