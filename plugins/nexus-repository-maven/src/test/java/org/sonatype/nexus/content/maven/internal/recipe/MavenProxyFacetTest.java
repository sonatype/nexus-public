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

package org.sonatype.nexus.content.maven.internal.recipe;

import java.net.URI;
import java.net.URISyntaxException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.repository.maven.MavenProxyRequestHeaderSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenProxyFacetTest
    extends TestSupport
{
  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private MavenProxyRequestHeaderSupport mavenProxyRequestHeaderSupport;

  private MavenProxyFacet underTest;

  @Before
  public void setUp() {
    this.underTest = new MavenProxyFacet(constraintViolationFactory
        , mavenProxyRequestHeaderSupport);
    when(applicationVersion.getEdition()).thenReturn("edition");
  }

  @Test
  public void testNonMavenCentralHostAndVerifyRequestHeader() throws URISyntaxException {
    URI uri = new URI("schema", "host", "/path/test", "fragment");
    Context context = mock(Context.class);

    HttpRequestBase request = underTest.buildFetchHttpRequest(uri, context);
    assertEquals(request.getAllHeaders().length, 0);
  }

  @Test
  public void testMavenCentralHostAndVerifyRequestHeader() throws URISyntaxException {
    URI uri = new URI("schema", "repo1.maven.org", "/path/test", "fragment");
    Context context = mock(Context.class);
    HttpRequestBase request = underTest.buildFetchHttpRequest(uri, context);
    assertEquals(request.getAllHeaders().length, 1);
  }
}
