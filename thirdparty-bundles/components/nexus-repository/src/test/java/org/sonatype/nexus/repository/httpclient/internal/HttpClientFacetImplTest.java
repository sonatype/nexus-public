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
package org.sonatype.nexus.repository.httpclient.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link HttpClientFacetImpl}.
 */
public class HttpClientFacetImplTest
    extends TestSupport
{
  private HttpClientFacetImpl underTest;

  @Mock
  private HttpClientManager httpClientManager;

  private HttpClientFacetImpl.Config config = new HttpClientFacetImpl.Config();

  private UsernameAuthenticationConfiguration usernameAuthentication = new UsernameAuthenticationConfiguration();

  private NtlmAuthenticationConfiguration ntlmAuthentication = new NtlmAuthenticationConfiguration();

  // Value generated using: http://www.blitter.se/utils/basic-authentication-header-generator/
  private static final String BASIC_AUTH_ENCODED = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  @Before
  public void setUp() {
    underTest = new HttpClientFacetImpl(httpClientManager, config);

    usernameAuthentication.setUsername(USERNAME);
    usernameAuthentication.setPassword(PASSWORD);
  }

  @Test
  public void createBasicAuthHeaderWithoutAuthConfiguredThrowsException() throws Exception {
    Header basicAuth = underTest.createBasicAuthHeader();

    assertThat(basicAuth, is(nullValue()));
  }

  @Test
  public void createBasicAuthHeaderWithoutUsernameAuthThrowsException() throws Exception {
    config.authentication = ntlmAuthentication;

    Header basicAuth = underTest.createBasicAuthHeader();

    assertThat(basicAuth, is(nullValue()));
  }

  @Test
  public void createBasicAuthWithUsernameAuthConfigWorks() throws Exception {
    config.authentication = usernameAuthentication;

    Header basicAuth = underTest.createBasicAuthHeader();

    assertThat(basicAuth.getName(), is(equalTo(HttpHeaders.AUTHORIZATION)));
    assertThat(basicAuth.getValue(), is(equalTo(BASIC_AUTH_ENCODED)));
  }
}
