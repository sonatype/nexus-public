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
package com.sonatype.nexus.ssl.plugin.internal;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.sonatype.nexus.ssl.plugin.internal.keystore.KeyStoreDataEvent;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.EventType;
import org.sonatype.nexus.distributed.event.service.api.common.CertificateDistributedEvent;
import org.sonatype.nexus.ssl.CertificateCreatedEvent;
import org.sonatype.nexus.ssl.CertificateDeletedEvent;
import org.sonatype.nexus.ssl.KeyStoreManager;
import org.sonatype.nexus.ssl.KeystoreException;
import org.sonatype.nexus.ssl.TrustStore;

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.ssl.CertificateUtil.calculateSha1;
import static org.sonatype.nexus.ssl.CertificateUtil.decodePEMFormattedCertificate;

/**
 * {@link TrustStore} implementation.
 *
 * @since ssl 1.0
 */
@Named
@Singleton
public class TrustStoreImpl
    extends ComponentSupport
    implements EventAware, TrustStore
{
  public static final SecureRandom DEFAULT_RANDOM = null;

  private final FreezeService freezeService;

  private final EventManager eventManager;

  private final KeyManager[] keyManagers;

  private final TrustManager[] trustManagers;

  private X509TrustManager managedTrustManager;

  private final KeyStoreManager keyStoreManager;

  private volatile SSLContext sslcontext;

  @Inject
  public TrustStoreImpl(
      final EventManager eventManager,
      @Named("ssl") final KeyStoreManager keyStoreManager,
      final FreezeService freezeService) throws Exception
  {
    this.eventManager = checkNotNull(eventManager);
    this.keyStoreManager = checkNotNull(keyStoreManager);
    this.freezeService = checkNotNull(freezeService);
    this.keyManagers = getSystemKeyManagers();
    this.trustManagers = getTrustManagers();
  }

  @Override
  public Certificate importTrustCertificate(
      final Certificate certificate,
      final String alias) throws KeystoreException
  {
    freezeService.checkWritable("Unable to import a certificate while database is frozen.");

    keyStoreManager.importTrustCertificate(certificate, alias);

    eventManager.post(new CertificateCreatedEvent(alias, certificate));
    eventManager.post(new CertificateDistributedEvent(EventType.CREATED));

    log.info("Certificate added successfully in trust-store with Fingerprint: {}, Name: {} and SHA1 Identifier: {} ",
        alias,
        getCertificateName(certificate),
        getCertificateSha1(certificate));

    return certificate;
  }

  @Override
  public Certificate importTrustCertificate(
      final String certificateInPEM,
      final String alias) throws KeystoreException, CertificateException
  {
    final Certificate certificate = decodePEMFormattedCertificate(certificateInPEM);

    return importTrustCertificate(certificate, alias);
  }

  @Override
  public Certificate getTrustedCertificate(final String alias) throws KeystoreException {
    return keyStoreManager.getTrustedCertificate(alias);
  }

  @Override
  public Collection<Certificate> getTrustedCertificates() throws KeystoreException {
    return keyStoreManager.getTrustedCertificates();
  }

  @Override
  public void removeTrustCertificate(final String alias) throws KeystoreException {
    freezeService.checkWritable("Unable to remove a certificate while database is frozen.");

    Certificate certificate = getTrustedCertificate(alias);
    keyStoreManager.removeTrustCertificate(alias);
    sslcontext = null;

    eventManager.post(new CertificateDeletedEvent(alias, certificate));
    eventManager.post(new CertificateDistributedEvent(EventType.DELETED));

    log.info(
        "Certificate removed successfully from trust-store with Fingerprint : {}, Name : {} and SHA1 Identifier : {}",
        alias,
        getCertificateName(certificate),
        getCertificateSha1(certificate));
  }

  @Override
  public SSLContext getSSLContext() {
    SSLContext _sslcontext = this.sslcontext; // local variable allows concurrent removeTrustCertificate
    if (_sslcontext == null) {
      try {
        // the trusted key store may have asychronously changed when NXRM is clustered, reload the managed store used
        // for fallback so the context doesn't use stale key store
        this.managedTrustManager = getManagedTrustManager(keyStoreManager);
        _sslcontext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
        _sslcontext.init(keyManagers, trustManagers, DEFAULT_RANDOM);
        this.sslcontext = _sslcontext;
      }
      catch (Exception e) {
        log.debug("Could not create SSL context", e);
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    }
    return _sslcontext;
  }

  @Override
  public KeyManager[] getKeyManagers() {
    return keyManagers; // NOSONAR
  }

  @Subscribe
  public void onKeyStoreDataUpdated(final KeyStoreDataEvent event) {
    sslcontext = null;
  }

  @Subscribe
  public void on(final CertificateDistributedEvent event) throws Exception {
    if (!event.isLocal()) {
      keyStoreManager.reloadTrustedKeystore();
      if (EventType.DELETED.equals(event.getEventType())) {
        sslcontext = null;
      }
    }
  }

  private TrustManager[] getTrustManagers() throws Exception {
    final TrustManager[] systemTrustManagers = getSystemTrustManagers();

    if (systemTrustManagers != null) {
      return stream(systemTrustManagers)
          .map(tm -> {
            if (tm instanceof X509TrustManager) {
              return new FallbackOnManagedX509TrustManager((X509TrustManager) tm);
            }
            else {
              return tm;
            }
          })
          .toArray(TrustManager[]::new);
    }
    return null;
  }

  private static X509TrustManager getManagedTrustManager(
      final KeyStoreManager keyStoreManager) throws KeystoreException
  {
    final TrustManager[] managedTrustManagers = keyStoreManager.getTrustManagers();
    if (managedTrustManagers != null) {
      for (TrustManager tm : managedTrustManagers) {
        if (tm instanceof X509TrustManager) {
          return (X509TrustManager) tm;
        }
      }
    }
    return null;
  }

  private static KeyManager[] getSystemKeyManagers() throws Exception {
    KeyManagerFactory keyManagerFactory;

    String keyAlgorithm = System.getProperty("ssl.KeyManagerFactory.algorithm");
    if (keyAlgorithm == null) {
      keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
    }
    String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType");
    if (keyStoreType == null) {
      keyStoreType = KeyStore.getDefaultType();
    }
    if ("none".equalsIgnoreCase(keyStoreType)) {
      keyManagerFactory = KeyManagerFactory.getInstance(keyAlgorithm);
    }
    else {
      final String keyStoreFileName = System.getProperty("javax.net.ssl.keyStore");
      if (keyStoreFileName != null) {
        File keyStoreFile = new File(keyStoreFileName);
        keyManagerFactory = KeyManagerFactory.getInstance(keyAlgorithm);
        String keyStoreProvider = System.getProperty("javax.net.ssl.keyStoreProvider");
        KeyStore keyStore;
        if (keyStoreProvider != null) {
          keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        }
        else {
          keyStore = KeyStore.getInstance(keyStoreType);
        }
        String password = System.getProperty("javax.net.ssl.keyStorePassword");
        try (FileInputStream in = new FileInputStream(keyStoreFile)) {
          keyStore.load(in, password != null ? password.toCharArray() : null);
        }
        keyManagerFactory.init(keyStore, password != null ? password.toCharArray() : null);
      }
      else {
        return null;
      }
    }

    return keyManagerFactory.getKeyManagers();
  }

  private static TrustManager[] getSystemTrustManagers() throws Exception {
    TrustManagerFactory trustManagerFactory;

    String trustAlgorithm = System.getProperty("ssl.TrustManagerFactory.algorithm");
    if (trustAlgorithm == null) {
      trustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    }
    String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
    if (trustStoreType == null) {
      trustStoreType = KeyStore.getDefaultType();
    }
    if ("none".equalsIgnoreCase(trustStoreType)) {
      trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
    }
    else {
      File trustStoreFile;
      KeyStore trustStore;

      String trustStoreFileName = System.getProperty("javax.net.ssl.trustStore");
      if (trustStoreFileName != null) {
        trustStoreFile = new File(trustStoreFileName);
        trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
        final String trustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider");
        if (trustStoreProvider != null) {
          trustStore = KeyStore.getInstance(trustStoreType, trustStoreProvider);
        }
        else {
          trustStore = KeyStore.getInstance(trustStoreType);
        }
      }
      else {
        File javaHome = new File(System.getProperty("java.home"));
        File file = new File(javaHome, "lib/security/jssecacerts");
        if (!file.exists()) {
          file = new File(javaHome, "lib/security/cacerts");
          trustStoreFile = file;
        }
        else {
          trustStoreFile = file;
        }

        trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      }
      final String password = System.getProperty("javax.net.ssl.trustStorePassword");
      try (FileInputStream in = new FileInputStream(trustStoreFile)) {
        trustStore.load(in, password != null ? password.toCharArray() : null);
      }
      trustManagerFactory.init(trustStore);
    }
    return trustManagerFactory.getTrustManagers();
  }

  private String getCertificateName(final Certificate certificate) {
    if (certificate instanceof X509Certificate) {
      X509Certificate cert = (X509Certificate) certificate;
      return cert.getSubjectDN().getName();
    }
    else {
      log.warn("Unknown certificate found, hence can't get the name.");
      return "Unknown";
    }
  }

  private String getCertificateSha1(final Certificate certificate) {
    try {
      return calculateSha1(certificate);
    }
    catch (CertificateEncodingException e) {
      log.error("Error occurred while calculating certificate SHA1", e);
      return "Unknown";
    }
  }

  /**
   * Wraps an {@link X509TrustManager} with one that falls back on the
   * managed trust manager when checking if a certificate is trusted.
   */
  private class FallbackOnManagedX509TrustManager
      implements X509TrustManager
  {

    private final X509TrustManager primary;

    FallbackOnManagedX509TrustManager(final X509TrustManager primary) {
      this.primary = checkNotNull(primary);
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      primary.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      try {
        primary.checkServerTrusted(chain, authType);
      }
      catch (CertificateException e) {
        if (managedTrustManager == null) {
          throw e;
        }
        try {
          // if managed trust manager rejects too then rethrow original rejection, otherwise accept certificate by
          // swallowing original rejection
          managedTrustManager.checkServerTrusted(chain, authType);
        }
        catch (CertificateException managedException) {
          throw e;
        }
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return primary.getAcceptedIssuers();
    }
  }
}
