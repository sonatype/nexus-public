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
package com.sonatype.nexus.ssl.plugin.validator;

import org.sonatype.nexus.ssl.CertificateUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class PemPrivateKeyValidatorTest
{
  MockedStatic<CertificateUtil> certificateUtil;

  PemPrivateKeyValidator underTest = new PemPrivateKeyValidator();

  @BeforeEach
  void setUp() throws Exception {
    certificateUtil = mockStatic(CertificateUtil.class);
  }

  @AfterEach
  void tearDown() throws Exception {
    certificateUtil.close();
  }

  @Test
  void testIsValid() {
    certificateUtil.when(() -> CertificateUtil.isPEMFormattedPrivateKey(anyString())).thenReturn(true);

    assertTrue(underTest.isValid("a-pem-formatted-string", null));
  }

  @Test
  void testIsValid_null() {
    assertTrue(underTest.isValid(null, null));
  }

  @Test
  void testIsValid_invalid() {
    certificateUtil.when(() -> CertificateUtil.isPEMFormattedPrivateKey(anyString())).thenReturn(false);

    assertFalse(underTest.isValid("a-pem-formatted-string", null));
  }

  @Test
  void testIsValid_placeholder() {
    assertTrue(underTest.isValid("#~NXRM~PLACEHOLDER~PASSWORD~#", null));
  }
}
