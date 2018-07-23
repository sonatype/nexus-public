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
package org.sonatype.nexus.ssl.internal.geronimo;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyNotFoundException;
import org.sonatype.nexus.ssl.KeystoreException;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link KeystoreInstance}. Where the keystores are actually stored
 * is dependent on the {@link KeyStoreStorage} implementation provided.
 *
 * This implementation is not thread safe.
 */
@NotThreadSafe
public class FileKeystoreInstance
    implements KeystoreInstance
{
  private static final Logger log = LoggerFactory.getLogger(FileKeystoreInstance.class);

  private final CryptoHelper crypto;

  private KeyStoreStorage storage;

  private String keystoreName;

  private String keystoreType;

  private char[] keystorePassword; // Used to "unlock" the keystore for other services

  private Map<String, char[]> keyPasswords = new HashMap<>();

  private char[] openPassword; // The password last used to open the keystore for editing

  // The following variables are the state of the keystore, which should be chucked if the file on disk changes
  private List<String> privateKeys = new ArrayList<>();

  private List<String> trustCerts = new ArrayList<>();

  private KeyStore keystore;

  public FileKeystoreInstance(final CryptoHelper crypto,
                              final KeyStoreStorage storage,
                              final String keystoreName,
                              final char[] keystorePassword,
                              final String keystoreType,
                              final Map<String, char[]> keyPasswords)
  {
    this.crypto = crypto;
    this.storage = storage;
    this.keystoreName = keystoreName;
    this.keystoreType = keystoreType;
    this.keystorePassword = keystorePassword;
    if (keyPasswords != null) {
      this.keyPasswords.putAll(keyPasswords);
    }

    initializeKeystoreIfNotExist();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "storage=" + storage +
        ", keystoreType='" + keystoreType + '\'' +
        '}';
  }

  private void initializeKeystoreIfNotExist() {
    if (!storage.exists()) {

      log.debug("keystore does not exist, creating new one of type: {}", keystoreType);

      try {
        KeyStore tempKeystore = crypto.createKeyStore(keystoreType);
        tempKeystore.load(null, keystorePassword);

        keystore = tempKeystore;
        saveKeystore(keystorePassword);
        loadKeystoreData(keystorePassword);
      }
      catch (Exception e) {
        throw new IllegalArgumentException("Invalid keystore storage (" + storage + ")", e);
      }
    }
  }

  @Override
  public String[] listPrivateKeys(final char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    return privateKeys.toArray(new String[privateKeys.size()]);
  }

  @Override
  public String[] listTrustCertificates(final char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    return trustCerts.toArray(new String[trustCerts.size()]);
  }

  @Override
  public void importTrustCertificate(final Certificate cert, final String alias, final char[] storePassword)
      throws KeystoreException
  {
    if (storePassword == null) {
      throw new NullPointerException("storePassword is null");
    }
    ensureLoaded(storePassword);
    try {
      keystore.setCertificateEntry(alias, cert);
    }
    catch (KeyStoreException e) {
      throw new KeystoreException(
          "Unable to set certificate entry in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    trustCerts.add(alias);
    saveKeystore(storePassword);
  }

  @Override
  public void generateKeyPair(String alias, char[] storePassword, char[] keyPassword, String keyAlgorithm,
                              int keySize, String signatureAlgorithm, int validity, String commonName,
                              String orgUnit, String organization, String locality, String state, String country)
      throws KeystoreException
  {
    if (storePassword == null) {
      throw new NullPointerException("storePassword is null");
    }
    ensureLoaded(storePassword);
    try {
      KeyPairGenerator kpgen = crypto.createKeyPairGenerator(keyAlgorithm);
      kpgen.initialize(keySize);
      KeyPair keyPair = kpgen.generateKeyPair();
      X509Certificate cert = generateCertificate(keyPair.getPublic(), keyPair.getPrivate(), signatureAlgorithm,
          validity, commonName, orgUnit, organization, locality, state,
          country);

      keystore.setKeyEntry(alias, keyPair.getPrivate(), keyPassword, new Certificate[]{cert});
      privateKeys.add(alias);
      keyPasswords.put(alias, keyPassword);
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to generate key pair in keystore '" + keystoreName + "'", e);
    }
    catch (InvalidKeyException e) {
      throw new KeystoreException("Unable to generate key pair in keystore '" + keystoreName + "'", e);
    }
    catch (SignatureException e) {
      throw new KeystoreException("Unable to generate key pair in keystore '" + keystoreName + "'", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("Unable to generate key pair in keystore '" + keystoreName + "'", e);
    }
    catch (CertificateEncodingException e) {
      throw new KeystoreException("Unable to generate key pair in keystore '" + keystoreName + "'", e);
    }
    saveKeystore(storePassword);
  }

  @Override
  public void deleteEntry(final String alias, final char[] storePassword)
      throws KeystoreException
  {
    if (storePassword == null) {
      throw new NullPointerException("storePassword is null");
    }
    ensureLoaded(storePassword);
    try {
      keystore.deleteEntry(alias);
    }
    catch (KeyStoreException e) {
      throw new KeystoreException(
          "Unable to delete key in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    privateKeys.remove(alias);
    trustCerts.remove(alias);
    if (keyPasswords.containsKey(alias)) {
      keyPasswords.remove(alias);
    }
    saveKeystore(storePassword);
  }

  @Override
  public KeyManager[] getKeyManager(final String algorithm, final String alias, final char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      KeyManagerFactory keyFactory = crypto.createKeyManagerFactory(algorithm);
      if (privateKeys.size() == 1) {
        keyFactory.init(keystore, keyPasswords.get(alias));
      }
      else {
        // When there is more than one private key in the keystore, we create a temporary "sub keystore"
        // with only one entry of our interest and use it
        KeyStore subKeystore = KeyStore.getInstance(keystore.getType(), keystore.getProvider());
        try {
          subKeystore.load(null, null);
        }
        catch (NoSuchAlgorithmException e) {
          // should not occur
        }
        catch (CertificateException e) {
          // should not occur
        }
        catch (IOException e) {
          // should not occur
        }
        subKeystore.setKeyEntry(alias, keystore.getKey(alias, keyPasswords.get(alias)),
            keyPasswords.get(alias), keystore.getCertificateChain(alias));
        keyFactory.init(subKeystore, keyPasswords.get(alias));
      }
      return keyFactory.getKeyManagers();
    }
    catch (KeyStoreException e) {
      throw new KeystoreException(
          "Unable to retrieve key manager in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException(
          "Unable to retrieve key manager in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (UnrecoverableKeyException e) {
      throw new KeystoreException(
          "Unable to retrieve key manager in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
  }

  @Override
  public TrustManager[] getTrustManager(final String algorithm, final char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      TrustManagerFactory trustFactory = crypto.createTrustManagerFactory(algorithm);
      trustFactory.init(keystore);
      return trustFactory.getTrustManagers();
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to retrieve trust manager in keystore '" + keystoreName + "'", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("Unable to retrieve trust manager in keystore '" + keystoreName + "'", e);
    }
  }

  @Override
  public PrivateKey getPrivateKey(final String alias, final char[] storePassword, final char[] keyPassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      PrivateKey key = (PrivateKey) keystore.getKey(alias, keyPassword);
      if (key == null) {
        throw new KeyNotFoundException(
            "Keystore '" + keystoreName + "' does not contain a private key with alias '" + alias + "'.");
      }
      return key;
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to retrieve private key from keystore", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("Unable to retrieve private key from keystore", e);
    }
    catch (UnrecoverableKeyException e) {
      throw new KeystoreException("Unable to retrieve private key from keystore", e);
    }
  }

  @Override
  public Certificate getCertificate(final String alias, final char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      Certificate cert = keystore.getCertificate(alias);
      if (cert == null) {
        throw new KeyNotFoundException(
            "Keystore '" + keystoreName + "' does not contain a certificate with alias '" + alias + "'.");
      }
      return cert;
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to retrieve certificate from keystore", e);
    }
  }

  @Override
  public Certificate getCertificate(final String alias) {
    try {
      return keystore.getCertificate(alias);
    }
    catch (KeyStoreException e) {
      log.error("Unable to read certificate from keystore", e);
    }
    return null;
  }

  // ==================== Internals =====================

  private void loadKeystoreData(final char[] password)
      throws KeystoreException
  {
    try {
      // Make sure the keystore is loadable using the provided password before resetting the instance variables.
      KeyStore tempKeystore = crypto.createKeyStore(keystoreType);
      storage.load(tempKeystore, password);
      // Keystore could be loaded successfully.  Initialize the instance variables to reflect the new keystore.
      keystore = tempKeystore;
      privateKeys.clear();
      trustCerts.clear();
      openPassword = password;
      Enumeration<String> aliases = keystore.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        if (keystore.isKeyEntry(alias)) {
          privateKeys.add(alias);
        }
        else if (keystore.isCertificateEntry(alias)) {
          trustCerts.add(alias);
        }
      }
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to open keystore with provided password", e);
    }
    catch (IOException e) {
      throw new KeystoreException("Unable to open keystore with provided password", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("Unable to open keystore with provided password", e);
    }
    catch (CertificateException e) {
      throw new KeystoreException("Unable to open keystore with provided password", e);
    }
  }

  private boolean isLoaded(final char[] password) {
    if (openPassword == null || openPassword.length != password.length) {
      return false;
    }
    if (storage.modified()) {
      return false;
    }
    for (int i = 0; i < password.length; i++) {
      if (password[i] != openPassword[i]) {
        return false;
      }
    }
    return true;
  }

  private void ensureLoaded(final char[] storePassword)
      throws KeystoreException
  {
    char[] password;
    if (storePassword == null) {
      password = keystorePassword;
    }
    else {
      password = storePassword;
    }
    if (!isLoaded(password)) {
      loadKeystoreData(password);
    }
  }

  private X509Certificate generateCertificate(PublicKey publicKey, PrivateKey privateKey, String algorithm,
                                              int validity, String commonName, String orgUnit, String organization,
                                              String locality, String state, String country)
      throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CertificateEncodingException
  {
    return CertificateUtil.generateCertificate(publicKey, privateKey, algorithm,
        validity, commonName, orgUnit, organization, locality, state,
        country);
  }

  private void saveKeystore(final char[] password)
      throws KeystoreException
  {
    try {
      storage.save(keystore, password);
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to save keystore '" + keystoreName + "'", e);
    }
    catch (IOException e) {
      throw new KeystoreException("Unable to save keystore '" + keystoreName + "'", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException("Unable to save keystore '" + keystoreName + "'", e);
    }
    catch (CertificateException e) {
      throw new KeystoreException("Unable to save keystore '" + keystoreName + "'", e);
    }
  }

}
