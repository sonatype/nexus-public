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

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TrustedSSLCertificateExport}
 */
@ExtendWith(MockitoExtension.class)
class TrustedSSLCertificateExportTest
{
  @Mock
  private TrustedSSLCertificateStore trustedSSLCertificateStore;

  @Captor
  private ArgumentCaptor<String> aliasCaptor;

  @Captor
  private ArgumentCaptor<String> pemCaptor;

  @InjectMocks
  private TrustedSSLCertificateExport underTest;

  private File jsonFile;

  @TempDir
  File tempDir;

  @BeforeEach
  void setup() {
    jsonFile = new File(tempDir, "trusted_ssl_certificate.json");
  }

  @Test
  void testExport() throws Exception {
    List<TrustedSSLCertificate> certificateList = Arrays.asList(
        createTrustedSSLCertificate("alias1", "pem-content-1"),
        createTrustedSSLCertificate("alias2", "pem-content-2"));

    when(trustedSSLCertificateStore.findAll()).thenReturn(certificateList);

    underTest.export(jsonFile);

    assertThat(jsonFile.exists(), is(true));

    String jsonContent = Files.readString(jsonFile.toPath());
    assertThat(jsonContent, allOf(
        containsString("alias1"),
        containsString("alias2"),
        containsString("pem-content-1"),
        containsString("pem-content-2")));
  }

  @Test
  void testRestore() throws Exception {
    TrustedSSLCertificate cert1 = createTrustedSSLCertificate("alias1", "pem-content-1");
    TrustedSSLCertificate cert2 = createTrustedSSLCertificate("alias2", "pem-content-2");

    when(trustedSSLCertificateStore.findAll()).thenReturn(List.of(cert1, cert2));

    underTest.export(jsonFile);

    underTest.restore(jsonFile);

    verify(trustedSSLCertificateStore, times(2)).save(aliasCaptor.capture(), pemCaptor.capture());
    List<String> capturedAliases = aliasCaptor.getAllValues();
    List<String> capturedPems = pemCaptor.getAllValues();

    assertThat(capturedAliases.get(0), is("alias1"));
    assertThat(capturedPems.get(0), is("pem-content-1"));

    assertThat(capturedAliases.get(1), is("alias2"));
    assertThat(capturedPems.get(1), is("pem-content-2"));
  }

  private TrustedSSLCertificateData createTrustedSSLCertificate(final String alias, final String pem) {
    return new TrustedSSLCertificateData(alias, pem);
  }
}
