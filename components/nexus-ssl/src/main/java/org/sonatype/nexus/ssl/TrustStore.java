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

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collection;

import javax.net.ssl.SSLContext;

import org.sonatype.nexus.ssl.KeystoreException;

/**
 * Nexus SSL TrustStore.
 *
 * @since ssl 1.0
 */
public interface TrustStore
{
  /**
   * Imports a clients public key that will be allowed to connect.
   *
   * @param certificate the public certificate to be added.
   * @param alias       the alias of the public key
   * @return imported certificate
   * @throws KeystoreException thrown if the certificate cannot be imported.
   */
  Certificate importTrustCertificate(Certificate certificate, String alias) throws KeystoreException;

  /**
   * Imports a clients public key that will be allowed to connect.
   *
   * @param certificateInPEM the public certificate to be added encoded in the PEM format.
   * @param alias            the alias of the public key
   * @return imported certificate
   * @throws KeystoreException thrown if the certificate cannot be imported.
   * @throws java.security.cert.CertificateParsingException
   *                           thrown if the PEM formatted string cannot be parsed into a certificate.
   */
  Certificate importTrustCertificate(String certificateInPEM, String alias)
      throws KeystoreException, CertificateException;

  /**
   * Returns a Certificate by an alias, that was previously stored in the keystore.
   *
   * @param alias the alias of the Certificate to be returned.
   * @return a previously imported Certificate.
   * @throws KeystoreException thrown if there is a problem retrieving the certificate.
   */
  Certificate getTrustedCertificate(String alias) throws KeystoreException;

  /**
   * Returns a collection of trusted certificates.
   *
   * @throws KeystoreException thrown if there is a problem opening the keystore.
   */
  Collection<Certificate> getTrustedCertificates() throws KeystoreException;

  /**
   * Removes a trusted certificate from the store. Calling this method with an alias that does NOT exist will not
   * throw a KeystoreException.
   *
   * @param alias the alias of the certificate to be removed.
   * @throws KeystoreException thrown if the certificate by this alias cannot be removed or does not exist.
   */
  void removeTrustCertificate(String alias) throws KeystoreException;

  /**
   * Get the {@link SSLContext}.
   *
   * @return SSL context
   * @since 3.0
   */
  SSLContext getSSLContext();
}
