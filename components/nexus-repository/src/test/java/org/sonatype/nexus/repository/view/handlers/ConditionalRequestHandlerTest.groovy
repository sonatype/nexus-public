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
package org.sonatype.nexus.repository.view.handlers

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.view.ContentTypes
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Request
import org.sonatype.nexus.repository.view.Response
import org.sonatype.nexus.repository.view.Status
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.payloads.StringPayload

import com.google.common.net.HttpHeaders
import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Tests for {@link ConditionalRequestHandler}.
 */
class ConditionalRequestHandlerTest
    extends TestSupport
{
  @Mock
  Context context

  @Mock
  Repository repository

  @Mock
  ViewFacet viewFacet

  Response response200 = new Response.Builder()
      .status(Status.success(200))
      .payload(new StringPayload('payload', ContentTypes.TEXT_PLAIN))
      .header(HttpHeaders.LAST_MODIFIED, 'Tue, 16 Jun 2015 11:30:00 GMT')
      .build()

  Response response201 = new Response.Builder()
      .status(Status.success(201))
      .build()

  ConditionalRequestHandler conditionalRequestHandler = new ConditionalRequestHandler()

  @Before
  void prepare() {
    when(context.getRepository()).thenReturn(repository)
    when(repository.facet(eq(ViewFacet.class))).thenReturn(viewFacet)
    when(viewFacet.dispatch(Matchers.any(Request))).thenReturn(response200);
  }

  @Test
  void 'basic GET'() {
    Request request = new Request.Builder().action(HttpMethods.GET).path('/foo').build()
    when(context.getRequest()).thenReturn(request)
    when(context.proceed()).thenReturn(response200)
    Response response = conditionalRequestHandler.handle(context)
    verify(context, times(1)).proceed()
    assertThat(response.status.code, equalTo(200))
  }

  @Test
  void 'conditional GET false'() {
    Request request = new Request.Builder().action(HttpMethods.GET).path('/foo').header(HttpHeaders.IF_MODIFIED_SINCE, 'Tue, 16 Jun 2015 11:30:00 GMT').build()
    when(context.getRequest()).thenReturn(request)
    when(context.proceed()).thenReturn(response200)
    Response response = conditionalRequestHandler.handle(context)
    verify(context, times(1)).proceed()
    assertThat(response.status.code, equalTo(304))
  }

  @Test
  void 'conditional GET true'() {
    Request request = new Request.Builder().action(HttpMethods.GET).path('/foo').header(HttpHeaders.IF_MODIFIED_SINCE, 'Tue, 16 Jun 2014 11:30:00 GMT').build()
    when(context.getRequest()).thenReturn(request)
    when(context.proceed()).thenReturn(response200)
    Response response = conditionalRequestHandler.handle(context)
    verify(context, times(1)).proceed()
    assertThat(response.status.code, equalTo(200))
  }

  @Test
  void 'basic PUT'() {
    Request request = new Request.Builder().action(HttpMethods.PUT).path('/foo').payload(new StringPayload('payload', ContentTypes.TEXT_PLAIN)).build()
    when(context.getRequest()).thenReturn(request)
    when(context.proceed()).thenReturn(response201)
    Response response = conditionalRequestHandler.handle(context)
    verify(context, times(1)).proceed()
    assertThat(response.status.code, equalTo(201))
  }

  @Test
  void 'conditional PUT false'() {
    Request request = new Request.Builder().action(HttpMethods.PUT).path('/foo').payload(new StringPayload('payload', ContentTypes.TEXT_PLAIN)).header(HttpHeaders.IF_UNMODIFIED_SINCE, 'Tue, 16 Jun 2014 11:30:00 GMT').build()
    when(context.getRequest()).thenReturn(request)
    when(context.proceed()).thenReturn(response201)
    Response response = conditionalRequestHandler.handle(context)
    verify(context, times(0)).proceed()
    assertThat(response.status.code, equalTo(412))
  }

  @Test
  void 'conditional PUT true'() {
    Request request = new Request.Builder().action(HttpMethods.PUT).path('/foo').payload(new StringPayload('payload', ContentTypes.TEXT_PLAIN)).header(HttpHeaders.IF_UNMODIFIED_SINCE, 'Tue, 16 Jun 2015 11:30:00 GMT').build()
    when(context.getRequest()).thenReturn(request)
    when(context.proceed()).thenReturn(response201)
    Response response = conditionalRequestHandler.handle(context)
    verify(context, times(1)).proceed()
    assertThat(response.status.code, equalTo(201))
  }
}
