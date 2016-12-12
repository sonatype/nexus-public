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
import java.security.cert.CertificateException;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.ssl.KeyStoreManager;
import org.sonatype.nexus.ssl.KeystoreException;

import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.orient.testsupport.OrientExceptionMocker.mockOrientException;

public class TrustStoreImplTest
    extends TestSupport
{

  private final String[] CERT_IN_PEM = {
      "-----BEGIN CERTIFICATE-----",
      "MIIByzCCAXUCBgE0OsUqMjANBgkqhkiG9w0BAQUFADBtMRYwFAYDVQQDEw10byBi",
      "ZSBjaGFuZ2VkMQ8wDQYDVQQLEwZjaGFuZ2UxDzANBgNVBAoTBmNoYW5nZTEPMA0G",
      "A1UEBxMGY2hhbmdlMQ8wDQYDVQQIEwZjaGFuZ2UxDzANBgNVBAYTBmNoYW5nZTAg",
      "Fw0xMTEyMTQwNDEyMDdaGA8yMTExMTEyMDA0MTIwN1owbTEWMBQGA1UEAxMNdG8g",
      "YmUgY2hhbmdlZDEPMA0GA1UECxMGY2hhbmdlMQ8wDQYDVQQKEwZjaGFuZ2UxDzAN",
      "BgNVBAcTBmNoYW5nZTEPMA0GA1UECBMGY2hhbmdlMQ8wDQYDVQQGEwZjaGFuZ2Uw",
      "XDANBgkqhkiG9w0BAQEFAANLADBIAkEAtyZDEbRZ9snDlCQbKerKAGGMHXIWF1t2",
      "6SBEAuC6krlujo5vMQsE/0Qp0jePjf9IKj8dR5RcXDKNi4mITY/Y4wIDAQABMA0G",
      "CSqGSIb3DQEBBQUAA0EAjX5DHXWkFxVWuvymp/2VUkcs8/PV1URpjpnVRL22GbXU",
      "UTlNxF8vcC+LMpLCaAk3OLezSwYkpptRFK/x3EWq7g==", "-----END CERTIFICATE-----"
  };

  private final String CERT_IN_PEM_UNIX = Stream.of(CERT_IN_PEM)
      .collect(joining("\n"));

  TrustStoreImpl underTest;

  Exception frozenException;

  @Mock
  DatabaseFreezeService databaseFreezeService;

  @Mock
  EventManager eventManager;

  @Mock
  KeyStoreManager keyStoreManager;

  @Mock
  Certificate certificate;

  @Before
  public void setUp() throws Exception {
    underTest = new TrustStoreImpl(eventManager, keyStoreManager, databaseFreezeService);

    frozenException = mockOrientException(OModificationOperationProhibitedException.class);
  }

  @Test
  public void importTrustCertificate() throws Exception {
    underTest.importTrustCertificate(certificate, "test");

    verify(keyStoreManager).importTrustCertificate(certificate, "test");
    verify(databaseFreezeService).checkUnfrozen("Unable to import a certificate while database is frozen.");
  }

  @Test
  public void importTrustCertificate_frozen() throws Exception {
    doThrow(frozenException).when(databaseFreezeService).checkUnfrozen(anyString());

    try {
      underTest.importTrustCertificate(certificate, "test");
      fail();
    }
    catch (OModificationOperationProhibitedException e) {
      //expected
    }
    verify(keyStoreManager, times(0)).importTrustCertificate(any(Certificate.class), anyString());
    verifyZeroInteractions(eventManager);
  }

  @Test
  public void importTrustCertificateStrings() throws KeystoreException, CertificateException {
    underTest.importTrustCertificate(CERT_IN_PEM_UNIX, "test");

    verify(keyStoreManager).importTrustCertificate(isA(Certificate.class), eq("test"));
    verify(databaseFreezeService).checkUnfrozen("Unable to import a certificate while database is frozen.");
  }

  @Test
  public void importTrustCertificateStrings_frozen() throws Exception {
    doThrow(frozenException).when(databaseFreezeService).checkUnfrozen(anyString());

    try {
      underTest.importTrustCertificate(CERT_IN_PEM_UNIX, "test");
      fail();
    }
    catch (OModificationOperationProhibitedException e) {
      //expected
    }
    verify(keyStoreManager, times(0)).importTrustCertificate(any(Certificate.class), anyString());
    verifyZeroInteractions(eventManager);
  }

  @Test
  public void testDelete() throws KeystoreException {
    when(keyStoreManager.getTrustedCertificate("test")).thenReturn(certificate);

    underTest.removeTrustCertificate("test");

    verify(keyStoreManager).removeTrustCertificate("test");
    verify(databaseFreezeService).checkUnfrozen("Unable to remove a certificate while database is frozen.");
  }

  @Test
  public void testDelete_frozen() throws KeystoreException {
    doThrow(frozenException).when(databaseFreezeService).checkUnfrozen(anyString());

    try {
      underTest.removeTrustCertificate("test");
      fail();
    }
    catch (OModificationOperationProhibitedException e) {
      //expected
    }

    verify(keyStoreManager, times(0)).removeTrustCertificate(anyString());
    verifyZeroInteractions(eventManager);
  }
}
