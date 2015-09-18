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

import java.security.PrivateKey;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.sonatype.nexus.ssl.KeystoreException;

/**
 * Management interface for dealing with a specific Keystore
 *
 * @version $Rev$ $Date$
 */
public interface KeystoreInstance
{

  /**
   * Returns the name of the keystore as known to the keystore manager.
   */
  public String getKeystoreName();

  /**
   * Returns the type of the keystore.
   */
  public String getKeystoreType();

  /**
   * Saves a password to access the keystore as a whole.  This means that any
   * other server component can use this keystore to create a socket factory.
   * However, the relevant private key in the keystore must also be unlocked.
   */
  public void unlockKeystore(char[] password)
      throws KeystoreException;

  /**
   * Clears any saved password, meaning this keystore cannot be used by other
   * server components.  You can still query and update it by passing the
   * password to other functions,
   */
  public void lockKeystore(char[] password)
      throws KeystoreException;

  /**
   * Checks whether this keystore is unlocked, which is to say, available for
   * other components to use to generate socket factories.
   * Does not check whether the unlock password is actually correct.
   */
  public boolean isKeystoreLocked();

  /**
   * Gets the aliases of all private key entries in the keystore
   *
   * @param storePassword Used to open the keystore. If null, the
   *                      internal password will be used and may
   * @throws KeystoreIsLocked if a null password was provided and the keystore
   *                          is locked, or if a bad password was provided
   */
  public String[] listPrivateKeys(char[] storePassword)
      throws KeystoreException;

  /**
   * Saves a password to access a private key.  This means that if the
   * keystore is also unlocked, any server component can create an SSL
   * socket factory using this private key.  Note that the keystore
   * must be unlocked before this can be called.
   */
  public void unlockPrivateKey(String alias, char[] storePassword, char[] keyPassword)
      throws KeystoreException;

  /**
   * Gets the aliases for all the private keys that are currently unlocked.
   * This only works if the keystore is unlocked.
   */
  public String[] getUnlockedKeys(char[] storePassword)
      throws KeystoreException;

  /**
   * Checks whether this keystore can be used as a truststore (e.g. has at
   * least one trust certificate).  This only works if the keystore is
   * unlocked.
   */
  public boolean isTrustStore(char[] storePassword)
      throws KeystoreException;

  /**
   * Clears any saved password for the specified private key, meaning this
   * key cannot be used for a socket factory by other server components.
   * You can still query and update it by passing the password to other
   * functions,
   *
   * @param storePassword The password used to access the keystore. Must be non-null.
   */
  public void lockPrivateKey(String alias, char[] storePassword)
      throws KeystoreException;

  /**
   * Checks whether the specified private key is locked, which is to say,
   * available for other components to use to generate socket factories.
   * Does not check whether the unlock password is actually correct.
   */
  public boolean isKeyLocked(String alias);

  /**
   * Gets the aliases of all trusted certificate entries in the keystore.
   *
   * @param storePassword Used to open the keystore or null to use the internal password.
   * @throws KeystoreIsLocked if the keystore coul not be unlocked
   */
  public String[] listTrustCertificates(char[] storePassword)
      throws KeystoreException;

  /**
   * Gets a particular certificate from the keystore.  This may be a trust
   * certificate or the certificate corresponding to a particular private
   * key.
   *
   * @param alias         The certificate to look at
   * @param storePassword Used to open the keystore or null to use the internal password.
   */
  public Certificate getCertificate(String alias, char[] storePassword)
      throws KeystoreException;

  /**
   * Gets a particular certificate chain from the keystore.
   *
   * @param alias         The certificate chain to look at
   * @param storePassword Used to open the keystore or null to use the internal password.
   * @throws KeystoreIsLocked if the keystore coul not be unlocked
   */
  public Certificate[] getCertificateChain(String alias, char[] storePassword)
      throws KeystoreException;

  /**
   * Gets the alias corresponding to the given certificate.
   *
   * @param cert          The certificate used to retrieve the alias
   * @param storePassword Used to open the keystore or null to use the internal password.
   * @throws KeystoreIsLocked if the keystore coul not be unlocked
   */
  public String getCertificateAlias(Certificate cert, char[] storePassword)
      throws KeystoreException;

  /**
   * Adds a certificate to this keystore as a trusted certificate.
   *
   * @param cert          The certificate to add
   * @param alias         The alias to list the certificate under
   * @param storePassword Used to open the keystore. Must be non null
   */
  public void importTrustCertificate(Certificate cert, String alias, char[] storePassword)
      throws KeystoreException;

  /**
   * Generates a new private key and certificate pair in this keystore.
   *
   * @param alias              The alias to store the new key pair under
   * @param storePassword      The password used to access the keystore
   * @param keyPassword        The password to use to protect the new key
   * @param keyAlgorithm       The algorithm used for the key (e.g. RSA)
   * @param keySize            The number of bits in the key (e.g. 1024)
   * @param signatureAlgorithm The algorithm used to sign the key (e.g. MD5withRSA)
   * @param validity           The number of days the certificate should be valid for
   * @param commonName         The CN portion of the identity on the certificate
   * @param orgUnit            The OU portion of the identity on the certificate
   * @param organization       The O portion of the identity on the certificate
   * @param locality           The L portion of the identity on the certificate
   * @param state              The ST portion of the identity on the certificate
   * @param country            The C portion of the identity on the certificate
   */
  public void generateKeyPair(String alias, char[] storePassword, char[] keyPassword, String keyAlgorithm,
                              int keySize,
                              String signatureAlgorithm, int validity, String commonName, String orgUnit,
                              String organization, String locality, String state, String country)
      throws KeystoreException;

  /**
   * Gets a KeyManager for a key in this Keystore.  This only works if both
   * the keystore and the private key in question have been unlocked,
   * allowing other components in the server to access them.
   *
   * @param algorithm     The SSL algorithm to use for this key manager
   * @param alias         The alias of the key to use in the keystore
   * @param storePassword The password used to access the keystore
   */
  public KeyManager[] getKeyManager(String algorithm, String alias, char[] storePassword)
      throws KeystoreException;

  /**
   * Gets a TrustManager for this keystore.  This only works if the keystore
   * has been unlocked, allowing other components in the server to access it.
   *
   * @param algorithm     The SSL algorithm to use for this trust manager
   * @param storePassword The password used to access the keystore
   */
  public TrustManager[] getTrustManager(String algorithm, char[] storePassword)
      throws KeystoreException;

  public String generateCSR(String alias, char[] storePassword)
      throws KeystoreException;

  public void importPKCS7Certificate(String alias, String certbuf, char[] storePassword)
      throws KeystoreException;

  /**
   * Deletes a key from this Keystore.
   *
   * @param alias         the alias to delete
   * @param storePassword The password used to access the keystore
   */
  public void deleteEntry(String alias, char[] storePassword)
      throws KeystoreException;

  /**
   * Gets the private key with the specified alias.
   *
   * @param alias         The alias of the private key to be retrieved
   * @param storePassword The password used to access the keystore
   * @param keyPassword   The password to use to protect the new key
   * @return PrivateKey with the alias specified
   */
  public PrivateKey getPrivateKey(String alias, char[] storePassword, char[] keyPassword)
      throws KeystoreException;

  /**
   * Gets a particular certificate from the keystore.  This may be a trust
   * certificate or the certificate corresponding to a particular private
   * key.
   * This only works if the keystore is unlocked.
   *
   * @param alias Alias of the certificate
   */
  public Certificate getCertificate(String alias);

  /**
   * Changes the keystore password.
   *
   * @param storePassword Current password for the keystore
   * @param newPassword   New password for the keystore
   */
  public void changeKeystorePassword(char[] storePassword, char[] newPassword)
      throws KeystoreException;

  /**
   * Changes the password for a private key entry in the keystore.
   *
   * @param storePassword  Password for the keystore
   * @param keyPassword    Current password for the private key
   * @param newKeyPassword New password for the private key
   */
  public void changeKeyPassword(String alias, char[] storePassword, char[] keyPassword, char[] newKeyPassword)
      throws KeystoreException;
}
