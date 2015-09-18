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
package org.sonatype.nexus.ssl;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

/**
 * Provides access to identity and truststores.
 *
 * @since 3.0
 */
public interface KeyStoreManager
{
  /**
   * Returns an array of TrustManager that represents all client certificates that can connect.
   *
   * @throws KeystoreException thrown if the array of TrustManagers cannot be created
   */
  TrustManager[] getTrustManagers() throws KeystoreException;

  /**
   * Returns an array of KeyManagers used to handle the authentication with the remote peers.
   *
   * @throws KeystoreException thrown if the array of KeyManagers cannot be created
   */
  KeyManager[] getKeyManagers() throws KeystoreException;

  /**
   * Imports a clients public key that will be allowed to connect.
   *
   * @param certificate the public certificate to be added.
   * @param alias       the alias of the public key
   * @throws KeystoreException thrown if the certificate cannot be imported.
   */
  void importTrustCertificate(Certificate certificate, String alias) throws KeystoreException;

  /**
   * Imports a clients public key that will be allowed to connect.
   *
   * @param certificateInPEM the public certificate to be added encoded in the PEM format.
   * @param alias            the alias of the public key
   * @throws KeystoreException thrown if the certificate cannot be imported.
   * @throws java.security.cert.CertificateParsingException
   *                           thrown if the PEM formatted string cannot be parsed into a certificate.
   */
  void importTrustCertificate(String certificateInPEM, String alias) throws KeystoreException, CertificateException;

  /**
   * Returns a Certificate by an alias, that was previously stored in the keystore.
   *
   * @param alias the alias of the Certificate to be returned.
   * @return a previously imported Certificate.
   * @throws KeyNotFoundException thrown if a certificate with the given alias is not found.
   * @throws KeystoreException    thrown if there is a problem retrieving the certificate.
   */
  Certificate getTrustedCertificate(String alias) throws KeystoreException;

  /**
   * Returns a collection of trusted certificates.
   *
   * @throws KeystoreException thrown if there is a problem opening the keystore.
   */
  Collection<Certificate> getTrustedCertificates() throws KeystoreException;

  /**
   * Removes a trusted certificate from the store.  Calling this method with an alias that does NOT exist will not
   * throw a KeystoreException.
   *
   * @param alias the alias of the certificate to be removed.
   * @throws KeystoreException thrown if the certificate by this alias cannot be removed or does not exist.
   */
  void removeTrustCertificate(String alias) throws KeystoreException;

  /**
   * Generates and stores a key pair used for authenticating remote clients.
   *
   * @param commonName         typically a first and last name in the format of "Joe Coder"
   * @param organizationalUnit the organization unit of the user.
   * @param organization       the organization the user belongs to.
   * @param locality           city or locality of the user.
   * @param state              state or providence of the user.
   * @param country            two letter country code.
   * @throws KeystoreException thrown if the key pair cannot be created.
   */
  void generateAndStoreKeyPair(String commonName,
                               String organizationalUnit,
                               String organization,
                               String locality,
                               String state,
                               String country)
      throws KeystoreException;

  /**
   * Returns true if the key pair has already been created, false otherwise.
   */
  boolean isKeyPairInitialized();

  /**
   * Returns a Certificate generated from the public and private key.
   * This certificate will be passed to a remote client before that remote client is able to authenticate.
   *
   * @throws KeystoreException thrown when the certificate has not been created.
   */
  Certificate getCertificate() throws KeystoreException;

  /**
   * Removes the private key from the KeyStore.
   *
   * @throws KeystoreException thrown if the KeyStore has not been initialized, the key could not be found, or an
   *                           error updating the KeyStore.
   */
  void removePrivateKey() throws KeystoreException;

  PrivateKey getPrivateKey() throws KeystoreException;
}
