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
package org.sonatype.nexus.repository.npm.security;

import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.repository.npm.security.NpmToken;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

public class NpmTokenTest
    extends TestSupport
{
  private static final String TOKEN = randomUUID().toString();

  @Mock
  private HttpServletRequest request;

  NpmToken underTest;

  @Before
  public void setup() throws Exception {
    underTest = new NpmToken();
  }

  @Test
  public void extractNpmToken() throws Exception {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer NpmToken." + TOKEN);
    String token = underTest.extract(request);
    assertThat(token, is(equalTo(TOKEN)));
  }

  @Test
  public void shouldNotMatchOtherFormat() throws Exception {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer NotNpm." + TOKEN);
    String token = underTest.extract(request);
    assertThat(token, is(nullValue()));
  }

  @Test
  public void extractNpmTokenWhenFormatNotPresent() throws Exception {
    when(request.getHeader(AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
    String token = underTest.extract(request);
    assertThat(token, is(equalTo(TOKEN)));
  }
}
