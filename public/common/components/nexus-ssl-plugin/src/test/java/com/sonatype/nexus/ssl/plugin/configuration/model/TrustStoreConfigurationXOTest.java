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
package com.sonatype.nexus.ssl.plugin.configuration.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link TrustStoreConfigurationXO}
 */
class TrustStoreConfigurationXOTest
{
  private static final String EXPECTED_TYPE_ID = "security.trust";

  @Test
  void shouldReturnTypeIdAndNullCertsForDefaultConstructor() {
    TrustStoreConfigurationXO xo = new TrustStoreConfigurationXO();

    assertThat(xo.getConfigurationTypeId(), is(EXPECTED_TYPE_ID));
    assertThat(xo.getCertificates(), is(nullValue()));
  }

  @Test
  void shouldReturnTypeIdAndCertsForParameterizedConstructor() {
    List<String> certs = List.of("cert1", "cert2", "cert3");

    TrustStoreConfigurationXO xo = new TrustStoreConfigurationXO(certs);

    assertThat(xo.getConfigurationTypeId(), is(EXPECTED_TYPE_ID));
    assertThat(xo.getCertificates(), is(certs));
  }

  @Test
  void shouldSetAndGetCertificates() {
    TrustStoreConfigurationXO xo = new TrustStoreConfigurationXO();

    List<String> certs = List.of("encrypted1", "encrypted2");
    xo.setCertificates(certs);

    assertThat(xo.getCertificates(), contains("encrypted1", "encrypted2"));
    assertThat(xo.getConfigurationTypeId(), is(EXPECTED_TYPE_ID));
  }

  @Test
  void shouldHaveCorrectTypeIdConstant() {
    assertThat(TrustStoreConfigurationXO.TYPE_ID, is(EXPECTED_TYPE_ID));
  }
}
