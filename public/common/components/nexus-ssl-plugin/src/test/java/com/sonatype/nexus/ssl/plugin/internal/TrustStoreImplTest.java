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

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLContext;

import com.sonatype.nexus.ssl.plugin.internal.keystore.TrustedKeyStoreManager;
import com.sonatype.nexus.ssl.plugin.internal.keystore.TrustedSSLCertificate;
import com.sonatype.nexus.ssl.plugin.internal.keystore.TrustedSSLCertificateStore;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyNotFoundException;
import org.sonatype.nexus.ssl.KeyStoreManager;
import org.sonatype.nexus.ssl.KeystoreException;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TrustStoreImplTest
{

  private static final String CERT_IN_PEM = """
      -----BEGIN CERTIFICATE-----
      MIIByzCCAXUCBgE0OsUqMjANBgkqhkiG9w0BAQUFADBtMRYwFAYDVQQDEw10byBi
      ZSBjaGFuZ2VkMQ8wDQYDVQQLEwZjaGFuZ2UxDzANBgNVBAoTBmNoYW5nZTEPMA0G
      A1UEBxMGY2hhbmdlMQ8wDQYDVQQIEwZjaGFuZ2UxDzANBgNVBAYTBmNoYW5nZTAg
      Fw0xMTEyMTQwNDEyMDdaGA8yMTExMTEyMDA0MTIwN1owbTEWMBQGA1UEAxMNdG8g
      YmUgY2hhbmdlZDEPMA0GA1UECxMGY2hhbmdlMQ8wDQYDVQQKEwZjaGFuZ2UxDzAN
      BgNVBAcTBmNoYW5nZTEPMA0GA1UECBMGY2hhbmdlMQ8wDQYDVQQGEwZjaGFuZ2Uw
      XDANBgkqhkiG9w0BAQEFAANLADBIAkEAtyZDEbRZ9snDlCQbKerKAGGMHXIWF1t2
      6SBEAuC6krlujo5vMQsE/0Qp0jePjf9IKj8dR5RcXDKNi4mITY/Y4wIDAQABMA0G
      CSqGSIb3DQEBBQUAA0EAjX5DHXWkFxVWuvymp/2VUkcs8/PV1URpjpnVRL22GbXU
      UTlNxF8vcC+LMpLCaAk3OLezSwYkpptRFK/x3EWq7g==
      -----END CERTIFICATE-----""";

  private static final String SHA_CERT_FINGERPRINT = "64:C4:44:A9:02:F7:F0:02:16:AA:C3:43:0B:BF:ED:44:C8:81:87:CD";

  private static final String TRUSTED_CERTIFICATES_MIGRATION_COMPLETE = "trusted.certificates.migration.complete";

  @Mock
  private EventManager eventManager;

  @Mock
  private KeyStoreManager keyStoreManager;

  @Mock
  private TrustedSSLCertificateStore trustedSSLCertificateStore;

  @Mock
  private TrustedKeyStoreManager trustedKeyStoreManager;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  private final CryptoHelper cryptoHelper = new CryptoHelperImpl(false);

  @Test
  public void testImportTrustCertificateWhenMigrationIsCompleted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);

    Certificate certificateResponse = underTest.importTrustCertificate(certificate, SHA_CERT_FINGERPRINT);

    assertThat(certificate, equalTo(certificateResponse));
    verify(trustedKeyStoreManager).validateCertificateIntoKeyStore(any(), any());
    verify(globalKeyValueStore).getBoolean(any());
    verify(trustedSSLCertificateStore).save(any(), any());
    verify(eventManager, times(2)).post(any());
    verify(keyStoreManager, never()).importTrustCertificate((Certificate) any(), any());
    verify(databaseCheck, never()).isAtLeast(any());
  }

  @Test
  public void testImportTrustCertificateWhenMigrationIsNotExecuted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.empty());
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);

    Certificate certificateResponse = underTest.importTrustCertificate(certificate, SHA_CERT_FINGERPRINT);

    assertThat(certificate, equalTo(certificateResponse));
    verify(trustedKeyStoreManager).validateCertificateIntoKeyStore(any(), any());
    verify(globalKeyValueStore).getBoolean(any());
    verify(databaseCheck).isAtLeast(any());
    verify(keyStoreManager).importTrustCertificate((Certificate) any(), any());
    verify(eventManager, times(2)).post(any());
    verify(trustedSSLCertificateStore, never()).save(any(), any());
  }

  @Test
  public void testImportTrustCertificateWhenCertificateCannotBeLoadedIntoTheKeystore() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);
    doThrow(new KeystoreException("The certificate cannot be loaded into the keystore")).when(trustedKeyStoreManager)
        .validateCertificateIntoKeyStore(any(), any());

    assertThrows(KeystoreException.class, () -> underTest.importTrustCertificate(certificate, SHA_CERT_FINGERPRINT));

    verify(trustedKeyStoreManager).validateCertificateIntoKeyStore(any(), any());
    verify(trustedSSLCertificateStore, never()).save(any(), any());
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testImportTrustCertificateWhenTheCertificateIsInPem() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);
    Certificate certificateResponse = underTest.importTrustCertificate(CERT_IN_PEM, SHA_CERT_FINGERPRINT);
    assertThat(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM), equalTo(certificateResponse));
    verify(trustedKeyStoreManager).validateCertificateIntoKeyStore(any(), any());
    verify(trustedSSLCertificateStore).save(any(), any());
    verify(eventManager, times(2)).post(any());
  }

  @Test
  public void testGetTrustedCertificateWhenMigrationIsCompleted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    TrustedSSLCertificate trustedSSLCertificate = mock(TrustedSSLCertificate.class);
    when(trustedSSLCertificate.getAlias()).thenReturn(SHA_CERT_FINGERPRINT);
    when(trustedSSLCertificate.getPem()).thenReturn(CERT_IN_PEM);

    when(trustedSSLCertificateStore.find(SHA_CERT_FINGERPRINT)).thenReturn(Optional.of(trustedSSLCertificate));

    Certificate certificateResponse = underTest.getTrustedCertificate(SHA_CERT_FINGERPRINT);

    assertThat(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM), equalTo(certificateResponse));
    verify(globalKeyValueStore).getBoolean(any());
    verify(trustedSSLCertificateStore).find(any());
    verify(keyStoreManager, never()).getTrustedCertificate(any());
    verify(databaseCheck, never()).isAtLeast(any());
  }

  @Test
  public void testGetTrustedCertificateWhenMigrationIsNotExecuted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.empty());
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);
    when(keyStoreManager.getTrustedCertificate(SHA_CERT_FINGERPRINT)).thenReturn(certificate);

    Certificate certificateResponse = underTest.getTrustedCertificate(SHA_CERT_FINGERPRINT);

    assertThat(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM), equalTo(certificateResponse));
    verify(globalKeyValueStore).getBoolean(any());
    verify(keyStoreManager).getTrustedCertificate(any());
    verify(trustedSSLCertificateStore, never()).find(any());
    verify(databaseCheck, never()).isAtLeast(any());
  }

  @Test
  public void testGetTrustedCertificateWhenAliasIsNull() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);
    assertThrows(NullPointerException.class, () -> underTest.getTrustedCertificate(null));

    verify(trustedSSLCertificateStore, never()).find(any());
  }

  @Test
  public void testGetTrustedCertificateWhenTheAliasDoesNotExist() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);
    when(trustedSSLCertificateStore.find(SHA_CERT_FINGERPRINT)).thenReturn(Optional.empty());

    assertThrows(KeyNotFoundException.class, () -> underTest.getTrustedCertificate(SHA_CERT_FINGERPRINT));

    verify(trustedSSLCertificateStore).find(any());
  }

  @Test
  public void testGetTrustedCertificatesWhenMigrationIsCompleted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    TrustedSSLCertificate trustedSSLCertificate = mock(TrustedSSLCertificate.class);
    when(trustedSSLCertificate.getAlias()).thenReturn(SHA_CERT_FINGERPRINT);
    when(trustedSSLCertificate.getPem()).thenReturn(CERT_IN_PEM);
    when(trustedSSLCertificateStore.findAll()).thenReturn(List.of(trustedSSLCertificate));

    Collection<Certificate> certificates = underTest.getTrustedCertificates();

    assertThat(certificates.size(), equalTo(1));
    assertThat(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM), equalTo(certificates.iterator().next()));
    verify(globalKeyValueStore).getBoolean(any());
    verify(trustedSSLCertificateStore).findAll();
    verify(keyStoreManager, never()).getTrustedCertificates();
    verify(databaseCheck, never()).isAtLeast(any());
  }

  @Test
  public void testGetTrustedCertificatesWhenMigrationIsNotExecuted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.empty());
    TrustStoreImpl trustStore =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);
    when(keyStoreManager.getTrustedCertificates()).thenReturn(List.of(certificate));

    Collection<Certificate> certificates = trustStore.getTrustedCertificates();

    assertThat(certificates.size(), equalTo(1));
    assertThat(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM), equalTo(certificates.iterator().next()));
    verify(globalKeyValueStore).getBoolean(any());
    verify(keyStoreManager).getTrustedCertificates();
    verify(trustedSSLCertificateStore, never()).findAll();
    verify(databaseCheck, never()).isAtLeast(any());
  }

  @Test
  public void testRemoveTrustCertificateWhenMigrationIsCompleted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    TrustedSSLCertificate trustedSSLCertificate = mock(TrustedSSLCertificate.class);
    when(trustedSSLCertificate.getAlias()).thenReturn(SHA_CERT_FINGERPRINT);
    when(trustedSSLCertificate.getPem()).thenReturn(CERT_IN_PEM);

    when(trustedSSLCertificateStore.find(SHA_CERT_FINGERPRINT)).thenReturn(Optional.of(trustedSSLCertificate));

    underTest.removeTrustCertificate(SHA_CERT_FINGERPRINT);

    verify(globalKeyValueStore).getBoolean(any());
    verify(trustedSSLCertificateStore).find(any());
    verify(trustedSSLCertificateStore).delete(any());
    verify(eventManager, times(2)).post(any());
    verify(databaseCheck, never()).isAtLeast(any());
    verify(keyStoreManager, never()).getTrustedCertificate(any());
    verify(keyStoreManager, never()).removeTrustCertificate(any());
  }

  @Test
  public void testRemoveTrustCertificateWhenMigrationIsNotExecuted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.empty());
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM);
    when(keyStoreManager.getTrustedCertificate(SHA_CERT_FINGERPRINT)).thenReturn(certificate);

    underTest.removeTrustCertificate(SHA_CERT_FINGERPRINT);

    verify(globalKeyValueStore).getBoolean(any());
    verify(databaseCheck).isAtLeast(any());
    verify(keyStoreManager).getTrustedCertificate(any());
    verify(keyStoreManager).removeTrustCertificate(any());
    verify(eventManager, times(2)).post(any());
    verify(trustedSSLCertificateStore, never()).find(any());
    verify(trustedSSLCertificateStore, never()).delete(any());
  }

  @Test
  public void testRemoveTrustCertificateWhenTheAliasDoesNotExist() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);
    when(trustedSSLCertificateStore.find(SHA_CERT_FINGERPRINT)).thenReturn(Optional.empty());

    assertThrows(KeyNotFoundException.class, () -> underTest.removeTrustCertificate(SHA_CERT_FINGERPRINT));

    verify(trustedSSLCertificateStore).find(any());
    verify(trustedSSLCertificateStore, never()).delete(any());
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testGetSSLContextWhenMigrationIsCompleted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.of(true));
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    SSLContext sslContext = underTest.getSSLContext();

    assertThat(sslContext, notNullValue());
    verify(globalKeyValueStore).getBoolean(any());
    verify(trustedSSLCertificateStore).findAll();
    verify(keyStoreManager, never()).getTrustedCertificates();
    verify(databaseCheck, never()).isAtLeast(any());
  }

  @Test
  public void testGetSSLContextWhenMigrationIsNotExecuted() throws Exception {
    when(globalKeyValueStore.getBoolean(TRUSTED_CERTIFICATES_MIGRATION_COMPLETE)).thenReturn(Optional.empty());
    TrustStoreImpl underTest =
        new TrustStoreImpl(eventManager, keyStoreManager, trustedSSLCertificateStore,
            trustedKeyStoreManager, databaseCheck, globalKeyValueStore, cryptoHelper);

    SSLContext sslContext = underTest.getSSLContext();

    assertThat(sslContext, notNullValue());
    verify(globalKeyValueStore).getBoolean(any());
    verify(keyStoreManager).getTrustManagers();
    verify(trustedSSLCertificateStore, never()).findAll();
    verify(databaseCheck, never()).isAtLeast(any());
  }
}
