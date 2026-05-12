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
package com.sonatype.nexus.ssl.plugin.tasks;

import java.security.cert.Certificate;
import java.util.List;

import com.sonatype.nexus.ssl.plugin.internal.keystore.TrustedSSLCertificateStore;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyStoreManager;
import org.sonatype.nexus.ssl.KeystoreException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TrustedCertificateMigrationServiceTest
{
  private static final String CERT_IN_PEM_1 = """
      -----BEGIN CERTIFICATE-----
      MIIEVzCCAz+gAwIBAgIRAJH3kfOo5zlHEtfG8frX5scwDQYJKoZIhvcNAQELBQAw
      OzELMAkGA1UEBhMCVVMxHjAcBgNVBAoTFUdvb2dsZSBUcnVzdCBTZXJ2aWNlczEM
      MAoGA1UEAxMDV1IyMB4XDTI1MDMxMDA4Mzc0NloXDTI1MDYwMjA4Mzc0NVowGTEX
      MBUGA1UEAxMOd3d3Lmdvb2dsZS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNC
      AASM6Rwpn6uqTsVH4JsZ48qrHxvNHC3GU2KnKRoEFDk2jE1rS3ztVqFDxluXoGMl
      ncxelUpa6gF5vS7I40xLYMv9o4ICQTCCAj0wDgYDVR0PAQH/BAQDAgeAMBMGA1Ud
      JQQMMAoGCCsGAQUFBwMBMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFG9TtVd00OTm
      NKCAEUwEUz8sNudNMB8GA1UdIwQYMBaAFN4bHu15FdQ+NyTDIbvsNDltQrIwMFgG
      CCsGAQUFBwEBBEwwSjAhBggrBgEFBQcwAYYVaHR0cDovL28ucGtpLmdvb2cvd3Iy
      MCUGCCsGAQUFBzAChhlodHRwOi8vaS5wa2kuZ29vZy93cjIuY3J0MBkGA1UdEQQS
      MBCCDnd3dy5nb29nbGUuY29tMBMGA1UdIAQMMAowCAYGZ4EMAQIBMDYGA1UdHwQv
      MC0wK6ApoCeGJWh0dHA6Ly9jLnBraS5nb29nL3dyMi85VVZiTjB3NUU2WS5jcmww
      ggEEBgorBgEEAdZ5AgQCBIH1BIHyAPAAdgDPEVbu1S58r/OHW9lpLpvpGnFnSrAX
      7KwB0lt3zsw7CAAAAZV/aujWAAAEAwBHMEUCIGOEB4SJXHoNrcgDVTn5s2wsRetb
      cmOuLPAhjO+HtjJ8AiEAgZ1wJlqRzyY9utlTxyfF6yrTH7+Nj+nkaxXgjRgs30UA
      dgDM+w9qhXEJZf6Vm1PO6bJ8IumFXA2XjbapflTA/kwNsAAAAZV/aujVAAAEAwBH
      MEUCIGtx1oHXW1u4IOrg9YCL3kCNHGDCfvjg2/rNdUyOaiD3AiEA7gKYFD6B86oZ
      F+JOcOuN5adcm2uelgKs1H+uCB0RVZgwDQYJKoZIhvcNAQELBQADggEBAARWWDG7
      gzurGgoheKdLs4FOZcSzSz+aKKUKRVAyXVo6ihVEVMbGiUQbpgTLo1M77TUrhgRE
      d6tJ6OHkiPWvj+4JkT/2XkOwEQlgyyMLqWr3AnDGk5K66WJFVKzx+zDmrtKipKMs
      Zd3kog5diS2yhluO2ogffx1h8kpRHNjixCq/mwfmM2PYAJsBkRqAyn8LWpCs3mqn
      NCK0amTI6AfcAwGRecS1CeX/yYOZzEO0uyb1N+MGVwbNLgqJoNb56VZgPryAARhc
      lAbKDCMzmQrqas47YeYmBGlg4gIyh2u5f0CYi62aD9d3T1I6uHGOMAR/jk7ZlF3p
      MIoXloiPwbsbICg=
      -----END CERTIFICATE-----""";

  private static final String CERT_IN_PEM_2 = """
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

  @Mock
  private KeyStoreManager keyStoreManager;

  @Mock
  private TrustedSSLCertificateStore trustedSSLCertificateStore;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  private TrustedCertificateMigrationService underTest;

  @Before
  public void setUp() throws Exception {
    underTest =
        new TrustedCertificateMigrationService(keyStoreManager, trustedSSLCertificateStore, globalKeyValueStore);
  }

  @Test
  public void testMigrate() throws Exception {
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM_1);
    when(keyStoreManager.getTrustedCertificates()).thenReturn(List.of(certificate));

    underTest.migrate();

    verify(keyStoreManager).getTrustedCertificates();
    verify(trustedSSLCertificateStore).save(any(), any());
    verify(globalKeyValueStore).setBoolean(any(), anyBoolean());
  }

  @Test
  public void testMigrateWhenThereIsNotCertificates() throws Exception {
    when(keyStoreManager.getTrustedCertificates()).thenReturn(List.of());

    underTest.migrate();

    verify(keyStoreManager).getTrustedCertificates();
    verify(trustedSSLCertificateStore, never()).save(any(), any());
    verify(globalKeyValueStore).setBoolean(any(), anyBoolean());
  }

  @Test
  public void testMigratesWhenGetTrustedCertificatesFailsTheMigrationIsSkipped() throws Exception {
    doThrow(new KeystoreException("someError")).when(keyStoreManager)
        .getTrustedCertificates();

    underTest.migrate();

    verify(keyStoreManager).getTrustedCertificates();
    verify(trustedSSLCertificateStore, never()).save(any(), any());
    verify(globalKeyValueStore, never()).setBoolean(any(), anyBoolean());
  }

  @Test
  public void testMigratesWhenTheFirstCertificateFailsTheSecondIsSaved() throws Exception {
    List<Certificate> certificates = List.of(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM_1),
        CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM_2));
    when(keyStoreManager.getTrustedCertificates()).thenReturn(certificates);
    String alias = CertificateUtil.calculateFingerprint(certificates.get(0));
    String pem = CertificateUtil.serializeCertificateInPEM(certificates.get(0));
    doThrow(new RuntimeException("someError")).when(trustedSSLCertificateStore)
        .save(alias, pem);

    underTest.migrate();

    verify(keyStoreManager).getTrustedCertificates();
    // even if the first certificate fails, the second one should be saved
    verify(trustedSSLCertificateStore, times(2)).save(any(), any());
    verify(globalKeyValueStore).setBoolean(any(), anyBoolean());
  }

  @Test
  public void testMigratesWhenThereAreTwoCertificates() throws Exception {
    List<Certificate> certificates = List.of(CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM_1),
        CertificateUtil.decodePEMFormattedCertificate(CERT_IN_PEM_2));
    when(keyStoreManager.getTrustedCertificates()).thenReturn(certificates);

    underTest.migrate();

    verify(keyStoreManager).getTrustedCertificates();
    verify(trustedSSLCertificateStore, times(2)).save(any(), any());
    verify(globalKeyValueStore).setBoolean(any(), anyBoolean());
  }
}
