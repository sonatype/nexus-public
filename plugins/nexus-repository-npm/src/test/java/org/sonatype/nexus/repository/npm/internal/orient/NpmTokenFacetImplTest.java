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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.NpmTokenFacet;
import org.sonatype.nexus.repository.npm.internal.security.NpmTokenManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link NpmTokenFacetImpl}
 */
public class NpmTokenFacetImplTest
    extends TestSupport
{

  @Mock
  private NpmTokenManager npmTokenManager;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  private NpmTokenFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    underTest = new NpmTokenFacetImpl(npmTokenManager);
    underTest.attach(repository);
  }

  @Test
  public void failedLoginTest() throws Exception {
    // mocks
    final Context contextMocked = mock(Context.class);
    final Request requestMocked = mock(Request.class);
    final Payload payloadMocked = mock(Payload.class);
    final TempBlob tempBlob = mock(TempBlob.class);

    // data
    final String someJson = "{\"name\":\"name\",\"password\":\"password\"}";

    // behaviors
    when(contextMocked.getRequest()).thenReturn(requestMocked);
    when(requestMocked.getPayload()).thenReturn(payloadMocked);
    when(storageFacet.createTempBlob(eq(payloadMocked), any())).thenReturn(tempBlob);
    when(tempBlob.get()).thenReturn(new ByteArrayInputStream(someJson.getBytes(StandardCharsets.UTF_8)));
    when(npmTokenManager.login("name", "password")).thenReturn(null);

    // test
    Response response = underTest.login(contextMocked);

    // checks
    assertThat("login call should have returned a value", response, notNullValue() );
    verify(npmTokenManager).login("name", "password");
    assertThat("expecting a 401 for the status", response.getStatus().getCode(), is(401));

  }

}
