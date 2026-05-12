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
package org.sonatype.nexus.api.rest.selfhosted.security.ssrf;

import java.util.Set;

import org.sonatype.nexus.api.rest.selfhosted.security.ssrf.model.SsrfProtectionConfigurationXO;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;
import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SsrfProtectionApiResourceTest
{
  @Mock
  private AntiSsrfService antiSsrfService;

  private SsrfProtectionApiResource underTest;

  @BeforeEach
  void setup() {
    underTest = new SsrfProtectionApiResource(antiSsrfService);
  }

  @Test
  void testGetConfiguration() {
    SsrfProtectionConfiguration config =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1"), Set.of("example.com"));
    when(antiSsrfService.getConfiguration()).thenReturn(config);

    SsrfProtectionConfigurationXO result = underTest.getConfiguration();

    assertThat(result.isEnabled(), is(true));
    assertThat(result.getAllowedIPs(), is(Set.of("10.0.0.1")));
    assertThat(result.getAllowedDomains(), is(Set.of("example.com")));
  }

  @Test
  void testUpdateConfiguration() {
    when(antiSsrfService.getConfiguration()).thenReturn(
        new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    SsrfProtectionConfigurationXO input = new SsrfProtectionConfigurationXO();
    input.setEnabled(false);
    input.setAllowedIPs(Set.of("192.168.1.1"));
    input.setAllowedDomains(Set.of("internal.corp"));

    underTest.updateConfiguration(input);

    verify(antiSsrfService).updateConfiguration(any(SsrfProtectionConfiguration.class));
    verify(antiSsrfService).getConfiguration();
  }

  @Test
  void testGetConfigurationDisabled() {
    SsrfProtectionConfiguration config = new SsrfProtectionConfiguration(false, Set.of(), Set.of());
    when(antiSsrfService.getConfiguration()).thenReturn(config);

    SsrfProtectionConfigurationXO result = underTest.getConfiguration();

    assertThat(result.isEnabled(), is(false));
    assertThat(result.getAllowedIPs().isEmpty(), is(true));
    assertThat(result.getAllowedDomains().isEmpty(), is(true));
  }

  @Test
  void testXoJsonDeserialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"enabled\": true, \"allowedIPs\": [\"10.0.0.1\"], \"allowedDomains\": [\"example.com\"]}";
    SsrfProtectionConfigurationXO xo = mapper.readValue(json, SsrfProtectionConfigurationXO.class);

    assertThat(xo.isEnabled(), is(true));
    assertThat(xo.getAllowedIPs(), is(Set.of("10.0.0.1")));
    assertThat(xo.getAllowedDomains(), is(Set.of("example.com")));
  }

  @Test
  void testXoNullCollections() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"enabled\": false}";
    SsrfProtectionConfigurationXO xo = mapper.readValue(json, SsrfProtectionConfigurationXO.class);

    assertThat(xo.isEnabled(), is(false));
    assertThat(xo.getAllowedIPs().isEmpty(), is(true));
    assertThat(xo.getAllowedDomains().isEmpty(), is(true));
  }

  @Test
  void testXoToConfiguration() {
    SsrfProtectionConfigurationXO xo = new SsrfProtectionConfigurationXO();
    xo.setEnabled(true);
    xo.setAllowedIPs(Set.of("10.0.0.1"));
    xo.setAllowedDomains(Set.of("example.com"));

    SsrfProtectionConfiguration config = xo.toConfiguration();

    assertThat(config.enabled(), is(true));
    assertThat(config.allowedIPs(), is(Set.of("10.0.0.1")));
    assertThat(config.allowedDomains(), is(Set.of("example.com")));
  }

  @Test
  void testXoRejectsBodyWithoutEnabledField() {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"bad\": \"str\"}";
    assertThrows(MismatchedInputException.class,
        () -> mapper.readValue(json, SsrfProtectionConfigurationXO.class));
  }

  @Test
  void testXoFromConfiguration() {
    SsrfProtectionConfiguration config =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1"), Set.of("example.com"));

    SsrfProtectionConfigurationXO xo = new SsrfProtectionConfigurationXO(config);

    assertThat(xo.isEnabled(), is(true));
    assertThat(xo.getAllowedIPs(), is(Set.of("10.0.0.1")));
    assertThat(xo.getAllowedDomains(), is(Set.of("example.com")));
  }
}
