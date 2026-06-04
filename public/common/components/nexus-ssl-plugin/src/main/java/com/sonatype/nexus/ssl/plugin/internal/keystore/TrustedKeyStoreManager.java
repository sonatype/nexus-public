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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyStoreManagerConfiguration;
import org.sonatype.nexus.ssl.KeystoreException;
import org.sonatype.nexus.ssl.internal.ReloadableX509TrustManager;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * key-store manager for only trusted certificates.
 *
 */
@Component
public class TrustedKeyStoreManager
{
  private final CryptoHelper crypto;

  private final KeyStoreManagerConfiguration config;

  @Autowired
  public TrustedKeyStoreManager(
      final CryptoHelper crypto,
      @Qualifier("ssl") KeyStoreManagerConfiguration config)
  {
    this.crypto = checkNotNull(crypto);
    this.config = checkNotNull(config);
  }

  public TrustManager[] getTrustManagers(final List<String> certificatesAsPem) {
    try {
      KeyStore keyStore = crypto.createKeyStore(config.getKeyStoreType());
      keyStore.load(null, config.getTrustedKeyStorePassword());

      for (String pem : certificatesAsPem) {
        Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(pem);
        String fingerprintAlias = CertificateUtil.calculateFingerprint(certificate);
        keyStore.setCertificateEntry(fingerprintAlias, certificate);
      }

      TrustManagerFactory trustFactory = crypto.createTrustManagerFactory(config.getTrustManagerAlgorithm());
      trustFactory.init(keyStore);
      TrustManager[] trustManagers = trustFactory.getTrustManagers();

      // replace the default X509TrustManager with our ReloadableX509TrustManager into trustManagers
      ReloadableX509TrustManager.replaceX509TrustManager(null, trustManagers);

      return trustManagers;
    }
    catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load a certificate into the keystore. This a way to validate the certificate does not have issues
   *
   * @param fingerprintAlias The alias to use for the certificate.
   * @param certificate The certificate to load.
   * @throws KeystoreException if the certificate cannot be loaded into the keystore.
   */
  public void validateCertificateIntoKeyStore(
      final String fingerprintAlias,
      final Certificate certificate) throws KeystoreException
  {
    try {
      KeyStore keyStore = crypto.createKeyStore(config.getKeyStoreType());
      keyStore.load(null, config.getTrustedKeyStorePassword());
      keyStore.setCertificateEntry(fingerprintAlias, certificate);
    }
    catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
      throw new KeystoreException("The certificate cannot be loaded into the keystore", e);
    }
  }
}
