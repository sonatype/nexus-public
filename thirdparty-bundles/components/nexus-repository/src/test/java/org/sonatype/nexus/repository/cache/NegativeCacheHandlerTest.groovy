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
package org.sonatype.nexus.repository.cache

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.HttpResponses
import org.sonatype.nexus.repository.http.HttpStatus
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Request
import org.sonatype.nexus.repository.view.Response
import org.sonatype.nexus.repository.view.Status

import org.junit.Before
import org.junit.Test

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Tests for {@link NegativeCacheHandler}.
 */
class NegativeCacheHandlerTest
extends TestSupport
{
  private NegativeCacheHandler underTest
  private NegativeCacheFacet facet
  private NegativeCacheKey key
  private Context context
  private Request request
  private Repository repository

  @Before
  void setUp() {
    underTest = new NegativeCacheHandler()
    facet = mock(NegativeCacheFacet)
    key = mock(NegativeCacheKey)
    context = mock(Context)
    request = mock(Request)
    repository = mock(Repository)
    when(context.getRequest()).thenReturn(request)
    when(context.getRepository()).thenReturn(repository)
    when(request.getAction()).thenReturn(HttpMethods.GET)
    when(repository.facet(NegativeCacheFacet)).thenReturn(facet)
    when(facet.getCacheKey(context)).thenReturn(key)
  }

  /**
   * Given:
   * - request is not an GET request
   * Then:
   * - context is asked to proceed
   * - response from context is passed on
   * - no other actions (checked by no interactions with repository)
   */
  @Test
  void 'directly proceed on non GET requests'() {
    when(request.getAction()).thenReturn(HttpMethods.PUT)
    Response contextResponse = HttpResponses.ok()
    when(context.proceed()).thenReturn(contextResponse)
    Response response = underTest.handle(context)
    assert response == contextResponse
    verify(context).proceed()
    verify(repository, never()).facet(any(Class))
  }

  /**
   * Given:
   * - no cached key present
   * - a 404 response from context
   * Then:
   * - context is asked to proceed
   * - response from context is passed on
   * - key is put in cache
   * - key is not invalidated
   */
  @Test
  void '404 response gets cached'() {
    Response contextResponse = HttpResponses.notFound('404')
    when(context.proceed()).thenReturn(contextResponse)
    when(facet.get(key)).thenReturn(null)
    Response response = underTest.handle(context)
    assert response == contextResponse
    verify(context).proceed()
    verify(facet).put(key, response.status)
    verify(facet, never()).invalidate(any(NegativeCacheKey))
  }

  /**
   * Given:
   * - cached key present
   * Then:
   * - cached status is returned
   * - context is not asked to proceed
   * - key is not put in cache
   * - key is not invalidated
   */
  @Test
  void 'return cached 404'() {
    Status cachedStatus = Status.failure(HttpStatus.NOT_FOUND, '404')
    when(facet.get(key)).thenReturn(cachedStatus)
    Response response = underTest.handle(context)
    assert response.getStatus() == cachedStatus
    verify(context, never()).proceed()
    verify(facet, never()).put(any(NegativeCacheKey), any(Status))
    verify(facet, never()).invalidate(any(NegativeCacheKey))
  }

  /**
   * Given:
   * - no cached key present
   * - a non 404 response from context
   * Then:
   * - context is asked to proceed
   * - response from context is passed on
   * - key is not put in cache
   * - key is not invalidated
   */
  @Test
  void 'non 404 response passes through'() {
    Response contextResponse = HttpResponses.serviceUnavailable('503')
    when(context.proceed()).thenReturn(contextResponse)
    when(facet.get(key)).thenReturn(null)
    Response response = underTest.handle(context)
    assert response == contextResponse
    verify(context).proceed()
    verify(facet, never()).put(any(NegativeCacheKey), any(Status))
    verify(facet, never()).invalidate(any(NegativeCacheKey))
  }

  /**
   * Given:
   * - no cached key present
   * - successful response from context
   * Then:
   * - context is asked to proceed
   * - response from context is passed on
   * - key is not put in cache
   * - key is invalidated
   */
  @Test
  void 'successful response invalidates cache'() {
    Response contextResponse = HttpResponses.ok('200')
    when(context.proceed()).thenReturn(contextResponse)
    when(facet.get(key)).thenReturn(null)
    Response response = underTest.handle(context)
    assert response == contextResponse
    verify(context).proceed()
    verify(facet, never()).put(any(NegativeCacheKey), any(Status))
    verify(facet).invalidate(any(NegativeCacheKey))
  }

}
