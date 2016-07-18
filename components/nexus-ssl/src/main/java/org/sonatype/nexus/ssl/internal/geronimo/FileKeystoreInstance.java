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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyNotFoundException;
import org.sonatype.nexus.ssl.KeystoreException;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import org.apache.geronimo.crypto.asn1.ASN1InputStream;
import org.apache.geronimo.crypto.asn1.ASN1Sequence;
import org.apache.geronimo.crypto.asn1.ASN1Set;
import org.apache.geronimo.crypto.asn1.DEROutputStream;
import org.apache.geronimo.crypto.asn1.x509.X509CertificateStructure;
import org.apache.geronimo.crypto.asn1.x509.X509Name;
import org.apache.geronimo.crypto.encoders.Base64;
import org.apache.geronimo.crypto.jce.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of KeystoreInstance that accesses a keystore file on the
 * local filesystem, identified by the file's name (the last component of
 * the name only, not the full path).
 *
 * @version $Rev$ $Date$
 */
public class FileKeystoreInstance
    implements KeystoreInstance
{
  private static final Logger log = LoggerFactory.getLogger(FileKeystoreInstance.class);

  final static String JKS = "JKS";

  private final CryptoHelper crypto;

  private KeyStoreStorage storage;

  private String keystoreName;

  private String keystoreType;

  private char[] keystorePassword; // Used to "unlock" the keystore for other services

  private Map<String, char[]> keyPasswords = new HashMap<String, char[]>();

  private char[] openPassword; // The password last used to open the keystore for editing

  // The following variables are the state of the keystore, which should be chucked if the file on disk changes
  private List privateKeys = new ArrayList();

  private List trustCerts = new ArrayList();

  private KeyStore keystore;

  public FileKeystoreInstance(CryptoHelper crypto,
                              KeyStoreStorage storage,
                              String keystoreName,
                              char[] keystorePassword,
                              String keystoreType,
                              Map<String, char[]> keyPasswords)
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

  // KeystoreInstnace interface

  public String getKeystoreName() {
    return keystoreName;
  }

  public String getKeystoreType() {
    return keystoreType;
  }

  public void unlockKeystore(char[] password)
      throws KeystoreException
  {
    if (password == null) {
      throw new NullPointerException("password is null");
    }
    ensureLoaded(password);
  }

  public void setKeystorePassword(String password) {
    keystorePassword = password == null ? null : password.toCharArray();
  }

  public void lockKeystore(char[] password)
      throws KeystoreException
  {
    try {
      keyPasswords.clear();
      storePasswords();
    }
    catch (Exception e) {
      throw new KeystoreException("Unable to set attribute keystorePassword on myself!", e);
    }
  }

  public boolean isKeystoreLocked() {
    return keystorePassword == null;
  }

  public String[] listPrivateKeys(char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    return (String[]) privateKeys.toArray(new String[privateKeys.size()]);
  }

  public void unlockPrivateKey(String alias, char[] storePassword, char[] password)
      throws KeystoreException
  {
    if (storePassword == null && keystorePassword == null) {
      throw new KeystoreException("storePassword is null and keystore is locked for availability.");
    }
    if (storePassword != null) {
      getPrivateKey(alias, storePassword, password);
    }
    else {
      getPrivateKey(alias, keystorePassword, password);
    }
    keyPasswords.put(alias, password);
    storePasswords();
  }

  public String[] getUnlockedKeys(char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    return (String[]) keyPasswords.keySet().toArray(new String[keyPasswords.size()]);
  }

  public boolean isTrustStore(char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    return trustCerts.size() > 0;
  }

  public void lockPrivateKey(String alias, char[] storePassword)
      throws KeystoreException
  {
    if (storePassword == null) {
      throw new NullPointerException("storePassword is null");
    }
    ensureLoaded(storePassword);
    keyPasswords.remove(alias);
    storePasswords();
  }

  // FIXME: This apparently does nothing... why does this exist?

  private void storePasswords()
      throws KeystoreException
  {
    StringBuilder buf = new StringBuilder();
    for (Iterator it = keyPasswords.entrySet().iterator(); it.hasNext(); ) {
      if (buf.length() > 0) {
        buf.append("]![");
      }
      Map.Entry entry = (Map.Entry) it.next();
      buf.append(entry.getKey()).append("=").append((char[]) entry.getValue());
    }
  }

  public void setKeyPasswords(String passwords) {
  } // Just so the kernel sees the new value

  /**
   * Checks whether the specified private key is locked, which is to say,
   * available for other components to use to generate socket factories.
   * Does not check whether the unlock password is actually correct.
   */
  public boolean isKeyLocked(String alias) {
    return keyPasswords.get(alias) == null;
  }

  public String[] listTrustCertificates(char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    return (String[]) trustCerts.toArray(new String[trustCerts.size()]);
  }

  public void importTrustCertificate(Certificate cert, String alias, char[] storePassword)
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

  public String generateCSR(String alias, char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      // find certificate by alias
      X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
      // find private key by alias
      PrivateKey key = (PrivateKey) keystore.getKey(alias, (char[]) keyPasswords.get(alias));
      // generate csr
      String csr = generateCSR(cert, key);
      return csr;
    }
    catch (KeyStoreException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (UnrecoverableKeyException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (InvalidKeyException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (NoSuchProviderException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (SignatureException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (IOException e) {
      throw new KeystoreException(
          "Unable to generate CSR in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
  }

  private String generateCSR(X509Certificate cert, PrivateKey signingKey)
      throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException,
             KeyStoreException, IOException
  {
    String sigalg = cert.getSigAlgName();
    X509Name subject;
    try {
      ASN1InputStream ais = new ASN1InputStream(cert.getEncoded());
      X509CertificateStructure x509Struct = new X509CertificateStructure((ASN1Sequence) ais.readObject());
      ais.close();
      subject = x509Struct.getSubject();
    }
    catch (CertificateEncodingException e) {
      log.warn(
          e.toString() + " while retrieving subject from certificate to create CSR.  Using subjectDN instead.");
      subject = new X509Name(cert.getSubjectDN().toString());
    }
    PublicKey publicKey = cert.getPublicKey();
    ASN1Set attributes = null;

    PKCS10CertificationRequest csr = new PKCS10CertificationRequest(sigalg,
        subject, publicKey, attributes, signingKey);

    if (!csr.verify()) {
      throw new KeyStoreException("CSR verification failed");
    }

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    DEROutputStream deros = new DEROutputStream(os);
    deros.writeObject(csr.getDERObject());
    String b64 = new String(Base64.encode(os.toByteArray()));

    final String BEGIN_CERT_REQ = "-----BEGIN CERTIFICATE REQUEST-----";
    final String END_CERT_REQ = "-----END CERTIFICATE REQUEST-----";
    final int CERT_REQ_LINE_LENGTH = 70;

    StringBuilder sbuf = new StringBuilder(BEGIN_CERT_REQ).append('\n');

    int idx = 0;
    while (idx < b64.length()) {

      int len = (idx + CERT_REQ_LINE_LENGTH > b64.length()) ? b64
          .length()
          - idx : CERT_REQ_LINE_LENGTH;

      String chunk = b64.substring(idx, idx + len);

      sbuf.append(chunk).append('\n');
      idx += len;
    }

    sbuf.append(END_CERT_REQ);
    return sbuf.toString();
  }

  public void importPKCS7Certificate(String alias, String certbuf, char[] storePassword)
      throws KeystoreException
  {
    if (storePassword == null) {
      throw new NullPointerException("storePassword is null");
    }
    ensureLoaded(storePassword);
    InputStream is = null;
    try {
      is = new ByteArrayInputStream(certbuf.getBytes());
      CertificateFactory cf = crypto.createCertificateFactory("X.509");
      Collection certcoll = cf.generateCertificates(is);
      Certificate[] chain = new Certificate[certcoll.size()];
      Iterator iter = certcoll.iterator();
      for (int i = 0; iter.hasNext(); i++) {
        chain[i] = (Certificate) iter.next();
      }
      if (keystore.getCertificate(alias).getPublicKey().equals(chain[0].getPublicKey())) {
        char[] keyPassword = (char[]) keyPasswords.get(alias);
        keystore.setKeyEntry(alias, keystore.getKey(alias, keyPassword), keyPassword, chain);
        saveKeystore(keystorePassword);
      }
      else {
        log.error(
            "Error in importPKCS7Certificate.  PublicKey in the certificate received is not related to the PrivateKey in the keystore. keystore = "
                + keystoreName + ", alias = " + alias);
      }
    }
    catch (CertificateException e) {
      throw new KeystoreException(
          "Unable to import PKCS7 certificat in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (KeyStoreException e) {
      throw new KeystoreException(
          "Unable to import PKCS7 certificat in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (NoSuchAlgorithmException e) {
      throw new KeystoreException(
          "Unable to import PKCS7 certificat in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    catch (UnrecoverableKeyException e) {
      throw new KeystoreException(
          "Unable to import PKCS7 certificat in keystore '" + keystoreName + "' for alias '" + alias + "'", e);
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (Exception e) {
        }
      }
    }
  }

  public void deleteEntry(String alias, char[] storePassword)
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
      storePasswords();
    }
    saveKeystore(storePassword);
  }

  public KeyManager[] getKeyManager(String algorithm, String alias, char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      KeyManagerFactory keyFactory = crypto.createKeyManagerFactory(algorithm);
      if (privateKeys.size() == 1) {
        keyFactory.init(keystore, (char[]) keyPasswords.get(alias));
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
        subKeystore.setKeyEntry(alias, keystore.getKey(alias, (char[]) keyPasswords.get(alias)),
            (char[]) keyPasswords.get(alias), keystore.getCertificateChain(alias));
        keyFactory.init(subKeystore, (char[]) keyPasswords.get(alias));
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

  public TrustManager[] getTrustManager(String algorithm, char[] storePassword)
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

  /**
   * Gets the private key with the specified alias.
   *
   * @param alias         The alias of the private key to be retrieved
   * @param storePassword The password used to access the keystore
   * @param keyPassword   The password to use to protect the new key
   * @return PrivateKey with the alias specified
   */
  public PrivateKey getPrivateKey(String alias, char[] storePassword, char[] keyPassword)
      throws KeyNotFoundException, KeystoreException, KeystoreIsLocked
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

  /**
   * Gets a particular certificate from the keystore.  This may be a trust
   * certificate or the certificate corresponding to a particular private
   * key.
   * This only works if the keystore is unlocked.
   *
   * @param alias The certificate to look at
   */
  public Certificate getCertificate(String alias, char[] storePassword)
      throws KeystoreIsLocked, KeyNotFoundException, KeystoreException
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

  public String getCertificateAlias(Certificate cert, char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      String alias = keystore.getCertificateAlias(cert);
      if (alias == null) {
        throw new KeyNotFoundException("Keystore '" + keystoreName
            + "' does not contain an alias corresponding to the given certificate.");
      }
      return alias;
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to read certificate alias from keystore", e);
    }
  }

  public Certificate[] getCertificateChain(String alias, char[] storePassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    try {
      Certificate[] certs = keystore.getCertificateChain(alias);
      if (certs == null) {
        throw new KeyNotFoundException(
            "Keystore '" + keystoreName + "' does not contain a certificate chain with alias '" + alias
                + "'.");
      }
      return certs;
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to read certificate chain from keystore", e);
    }
  }

  /**
   * Gets a particular certificate from the keystore.  This may be a trust
   * certificate or the certificate corresponding to a particular private
   * key.
   * This only works if the keystore is unlocked.
   *
   * @param alias The certificate to look at
   */
  public Certificate getCertificate(String alias) {
    if (isKeystoreLocked()) {
      return null;
    }
    try {
      return keystore.getCertificate(alias);
    }
    catch (KeyStoreException e) {
      log.error("Unable to read certificate from keystore", e);
    }
    return null;
  }

  /**
   * Changes the keystore password.
   *
   * @param storePassword Current password for the keystore
   * @param newPassword   New password for the keystore
   */
  public void changeKeystorePassword(char[] storePassword, char[] newPassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    saveKeystore(newPassword);
    log.info("Password changed for keystore: {}", keystoreName);
    // Copy password, since input is mutable
    openPassword = Arrays.copyOf(newPassword, newPassword.length);
    if (!isKeystoreLocked()) {
      unlockKeystore(openPassword);
    }
  }

  /**
   * Changes the password for a private key entry in the keystore.
   *
   * @param storePassword  Password for the keystore
   * @param keyPassword    Current password for the private key
   * @param newKeyPassword New password for the private key
   */
  public void changeKeyPassword(String alias, char[] storePassword, char[] keyPassword, char[] newKeyPassword)
      throws KeystoreException
  {
    ensureLoaded(storePassword);
    if (!privateKeys.contains(alias)) {
      throw new KeystoreException("No private key entry " + alias + " exists in the keystore " + keystoreName);
    }
    if (keyPasswords.containsKey(alias)) {
      if (!Arrays.equals(keyPasswords.get(alias), keyPassword)) {
        throw new KeystoreException("Incorrect password provided for private key entry " + alias);
      }
      keyPasswords.put(alias, newKeyPassword);
    }
    PrivateKey key = getPrivateKey(alias, storePassword, keyPassword);
    Certificate[] chain = getCertificateChain(alias, storePassword);
    try {
      keystore.setKeyEntry(alias, key, newKeyPassword, chain);
      saveKeystore(storePassword);
      log.info("Password changed for private key entry " + alias + " in keystore " + keystoreName + ".");
      if (keyPasswords.containsKey(alias)) {
        storePasswords();
      }
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Could not change password for private key entry " + alias, e);
    }
  }

  // ==================== Internals =====================

  private void loadKeystoreData(char[] password)
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
      Enumeration aliases = keystore.aliases();
      while (aliases.hasMoreElements()) {
        String alias = (String) aliases.nextElement();
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

  private boolean isLoaded(char[] password) {
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

  private void ensureLoaded(char[] storePassword)
      throws KeystoreException
  {
    char[] password;
    if (storePassword == null) {
      if (isKeystoreLocked()) {
        throw new KeystoreIsLocked(
            "Keystore '" + keystoreName + "' is locked; please unlock it in the console.");
      }
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

  private void saveKeystore(char[] password)
      throws KeystoreException
  {
    try {
      storage.save(keystore, password);
    }
    catch (KeyStoreException e) {
      throw new KeystoreException("Unable to save keystore '" + keystoreName + "'", e);
    }
    catch (FileNotFoundException e) {
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
