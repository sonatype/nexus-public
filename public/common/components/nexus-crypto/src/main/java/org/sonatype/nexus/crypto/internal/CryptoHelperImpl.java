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
package org.sonatype.nexus.crypto.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.sonatype.nexus.crypto.CryptoHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_FIPS_ENABLED_NAMED_VALUE;

/**
 * Default {@link CryptoHelper} using provider from BouncyCastleProvider or BouncyCastleFipsProvider.
 *
 * @since 3.0
 */
@Component
public class CryptoHelperImpl
    implements CryptoHelper
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Provider provider;

  @Autowired
  public CryptoHelperImpl(@Value(NEXUS_SECURITY_FIPS_ENABLED_NAMED_VALUE) final boolean nexusSecurityFipsEnabled) {
    this.provider = configureProvider(nexusSecurityFipsEnabled);
  }

  private static Provider configureProvider(final boolean nexusSecurityFipsEnabled) {
    String providerName = nexusSecurityFipsEnabled
        ? BouncyCastleFipsProvider.PROVIDER_NAME
        : BouncyCastleProvider.PROVIDER_NAME;

    Provider providerChosen = Security.getProvider(providerName);
    if (providerChosen != null) {
      return providerChosen;
    }

    if (nexusSecurityFipsEnabled) {
      loadFipsProvider();
      setFipsApprovedMode();
    }
    else {
      loadNonFipsProvider();
    }

    return Security.getProvider(providerName);
  }

  private static void loadNonFipsProvider() {
    // BouncyCastleProvider must be set as the last provider
    Security.addProvider(new BouncyCastleProvider());
  }

  private static void loadFipsProvider() {
    try {
      Class<?> providerClass =
          getFipsClassLoader().loadClass("org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider");
      Provider providerFips = (Provider) providerClass.getConstructor().newInstance();
      // BouncyCastleFipsProvider must be set as the first provider to ensure it is used by default
      Security.insertProviderAt(providerFips, 1);
    }
    catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException
        | IllegalAccessException e) {
      throw new RuntimeException("Failed to initialize FIPS provider", e);
    }
  }

  private static void setFipsApprovedMode() {
    try {
      Class<?> registrarClass = getFipsClassLoader().loadClass("org.bouncycastle.crypto.CryptoServicesRegistrar");
      Method setApprovedOnly = registrarClass.getMethod("setApprovedOnlyMode", boolean.class);
      setApprovedOnly.invoke(null, true);
    }
    catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Failed to initialize FIPS approved mode only", e);
    }
  }

  private static URLClassLoader getFipsClassLoader() {
    URL fipsJarUrl = BouncyCastleFipsProvider.class.getProtectionDomain().getCodeSource().getLocation();
    return new URLClassLoader(new URL[]{fipsJarUrl}, null);
  }

  @Override
  public Provider getProvider() {
    return provider;
  }

  private void logFallback(final Throwable cause) {
    log.trace("Falling back to system selection due to: " + cause); // omit stack-trace
  }

  @Override
  public Cipher createCipher(final String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
    checkNotNull(transformation);
    Cipher obj;
    try {
      obj = Cipher.getInstance(transformation, getProvider());
    }
    catch (Exception e) {
      logFallback(e);
      obj = Cipher.getInstance(transformation);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created cipher: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public int getCipherMaxAllowedKeyLength(final String transformation) throws NoSuchAlgorithmException {
    return Cipher.getMaxAllowedKeyLength(transformation);
  }

  @Override
  public Signature createSignature(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    Signature obj;
    try {
      obj = Signature.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = Signature.getInstance(algorithm);
    }

    if (log.isTraceEnabled()) {
      log.trace("Created signature: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public SecureRandom createSecureRandom(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    SecureRandom obj;
    try {
      obj = SecureRandom.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = SecureRandom.getInstance(algorithm);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created secure-random: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public SecureRandom createSecureRandom() {
    SecureRandom obj = new SecureRandom();
    if (log.isTraceEnabled()) {
      log.trace("Created secure-random: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public KeyStore createKeyStore(final String type) throws KeyStoreException {
    checkNotNull(type);
    KeyStore obj;
    try {
      obj = KeyStore.getInstance(type, getProvider());
    }
    catch (KeyStoreException e) {
      logFallback(e);
      obj = KeyStore.getInstance(type);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created key-store: {} ({})", obj.getType(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public KeyPairGenerator createKeyPairGenerator(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    KeyPairGenerator obj;
    try {
      obj = KeyPairGenerator.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = KeyPairGenerator.getInstance(algorithm);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created key-pair-generator: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public CertificateFactory createCertificateFactory(final String type) throws CertificateException {
    checkNotNull(type);
    CertificateFactory obj;
    try {
      obj = CertificateFactory.getInstance(type, getProvider());
    }
    catch (CertificateException e) {
      logFallback(e);
      obj = CertificateFactory.getInstance(type);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created certificate-factory: {} ({})", obj.getType(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public KeyManagerFactory createKeyManagerFactory(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    KeyManagerFactory obj;
    try {
      obj = KeyManagerFactory.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = KeyManagerFactory.getInstance(algorithm);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created key-manager-factory: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public TrustManagerFactory createTrustManagerFactory(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    TrustManagerFactory obj;
    try {
      obj = TrustManagerFactory.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = TrustManagerFactory.getInstance(algorithm);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created trust-manager-factory: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public MessageDigest createDigest(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    MessageDigest obj;
    try {
      obj = MessageDigest.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = MessageDigest.getInstance(algorithm);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created message-digest: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }

  @Override
  public SecretKeyFactory createSecretKeyFactory(final String algorithm) throws NoSuchAlgorithmException {
    checkNotNull(algorithm);
    SecretKeyFactory obj;
    try {
      obj = SecretKeyFactory.getInstance(algorithm, getProvider());
    }
    catch (NoSuchAlgorithmException e) {
      logFallback(e);
      obj = SecretKeyFactory.getInstance(algorithm);
    }
    if (log.isTraceEnabled()) {
      log.trace("Created secret-key-factory: {} ({})", obj.getAlgorithm(), obj.getProvider().getName());
    }
    return obj;
  }
}
