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
package org.sonatype.nexus.crypto;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Provides access to cryptology components.
 *
 * @since 3.0
 */
public interface CryptoHelper
{
  Cipher createCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException;

  int getCipherMaxAllowedKeyLength(String transformation) throws NoSuchAlgorithmException;

  Signature createSignature(String algorithm) throws NoSuchAlgorithmException;

  SecureRandom createSecureRandom(String algorithm) throws NoSuchAlgorithmException;

  SecureRandom createSecureRandom();

  KeyStore createKeyStore(String type) throws KeyStoreException;

  KeyPairGenerator createKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException;

  CertificateFactory createCertificateFactory(String type) throws CertificateException;

  KeyManagerFactory createKeyManagerFactory(String algorithm) throws NoSuchAlgorithmException;

  TrustManagerFactory createTrustManagerFactory(String algorithm) throws NoSuchAlgorithmException;

  MessageDigest createDigest(String algorithm) throws NoSuchAlgorithmException;

  SecretKeyFactory createSecretKeyFactory(String algorithm) throws NoSuchAlgorithmException;
}

