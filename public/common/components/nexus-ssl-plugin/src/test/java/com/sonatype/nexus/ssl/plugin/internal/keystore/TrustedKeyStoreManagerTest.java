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

import java.security.cert.Certificate;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyStoreManagerConfiguration;
import org.sonatype.nexus.ssl.internal.ReloadableX509TrustManager;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TrustedKeyStoreManagerTest
{
  private CryptoHelper crypto;

  private KeyStoreManagerConfiguration config;

  private TrustedKeyStoreManager underTest;

  @Before
  public void setUp() throws Exception {
    crypto = new CryptoHelperImpl(false);
    config = createMockConfiguration();
    underTest = new TrustedKeyStoreManager(crypto, config);
  }

  private KeyStoreManagerConfiguration createMockConfiguration() {
    KeyStoreManagerConfiguration config = mock(KeyStoreManagerConfiguration.class);
    // use lower strength for faster test execution
    when(config.getKeyStoreType()).thenReturn("JKS");
    when(config.getKeyAlgorithm()).thenReturn("RSA");
    when(config.getKeyAlgorithmSize()).thenReturn(1024);
    when(config.getSignatureAlgorithm()).thenReturn("SHA1WITHRSA");
    when(config.getCertificateValidity()).thenReturn(Time.days(36500));
    when(config.getKeyManagerAlgorithm()).thenReturn(KeyManagerFactory.getDefaultAlgorithm());
    when(config.getTrustManagerAlgorithm()).thenReturn(TrustManagerFactory.getDefaultAlgorithm());
    when(config.getPrivateKeyStorePassword()).thenReturn("pwd".toCharArray());
    when(config.getTrustedKeyStorePassword()).thenReturn("pwd".toCharArray());
    when(config.getPrivateKeyPassword()).thenReturn("pwd".toCharArray());
    return config;
  }

  @Test
  public void testGetTrustManagers() {
    TrustManager[] trustManagers = underTest.getTrustManagers(List.of());
    assertThat(trustManagers, notNullValue());
    assertThat(trustManagers[0], is(instanceOf(ReloadableX509TrustManager.class)));
  }

  @Test
  public void testValidateCertificateIntoKeyStore() throws Exception {
    String fingerprintAlias = "fingerprintAlias";
    Certificate certificate = CertificateUtil.decodePEMFormattedCertificate("""
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
        -----END CERTIFICATE-----""");

    underTest.validateCertificateIntoKeyStore(fingerprintAlias, certificate);
  }
}
