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
package org.sonatype.nexus.repository.cache;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.replication.PullReplicationSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NegativeCacheHandlerTest
    extends TestSupport
{
  @Mock
  private NegativeCacheFacet mockNegativeCacheFacet;

  @Mock
  private NegativeCacheKey mockNegativeCacheKey;

  @Mock
  private Context mockContext;

  @Mock
  private Request mockRequest;

  @Mock
  private Repository mockRepository;

  private NegativeCacheHandler underTest;

  @Before
  public void setUp() {
    underTest = new NegativeCacheHandler();

    when(mockContext.getRequest()).thenReturn(mockRequest);
    when(mockContext.getRepository()).thenReturn(mockRepository);
    when(mockRequest.getAction()).thenReturn(HttpMethods.GET);
    when(mockRepository.facet(NegativeCacheFacet.class)).thenReturn(mockNegativeCacheFacet);
    when(mockNegativeCacheFacet.getCacheKey(mockContext)).thenReturn(mockNegativeCacheKey);
  }

  /**
   * Given:
   * - request is not a GET/HEAD request
   * Then:
   *  - context is asked to proceed
   *  - response from context is passed on
   *  - no other actions (checked by no interactions with repository)
   */
  @Test
  public void directlyProceedOnNonGetOrHeadRequests() throws Exception {
    when(mockRequest.getAction()).thenReturn(HttpMethods.PUT);
    Response contextResponse = HttpResponses.ok();
    when(mockContext.proceed()).thenReturn(contextResponse);
    Response response = underTest.handle(mockContext);
    assert response == contextResponse;
    verify(mockContext).proceed();
    verify(mockRepository, never()).facet(any());
  }

  /**
   * Given:
   * - request is a Replication request
   * Then:
   *  - context is asked to proceed
   *  - response from context is passed on
   *  - if successful, cache is invalidated for key
   *  - no other actions (checked by no checking of key being cached)
   */
  @Test
  public void directlyProceedOnReplicationRequestInvalidateOnSuccess() throws Exception {
    AttributesMap contextAttributes = new AttributesMap();
    contextAttributes.set(PullReplicationSupport.IS_REPLICATION_REQUEST, true);
    when(mockContext.getAttributes()).thenReturn(contextAttributes);
    Response contextResponse = HttpResponses.ok();
    when(mockContext.proceed()).thenReturn(contextResponse);
    Response response = underTest.handle(mockContext);
    assert response == contextResponse;
    verify(mockContext).proceed();
    verify(mockNegativeCacheFacet).invalidate(mockNegativeCacheKey);
    verify(mockNegativeCacheFacet, never()).get(any());
  }
  /**
   * Given:
   * - request is a Replication request
   * Then:
   *  - context is asked to proceed
   *  - response from context is passed on
   *  - if not successful, cache is left as-is
   *  - no other actions (checked by no checking of key being cached)
   */
  @Test
  public void directlyProceedOnReplicationRequestLeaveExistingOnFail() throws Exception {
    AttributesMap contextAttributes = new AttributesMap();
    contextAttributes.set(PullReplicationSupport.IS_REPLICATION_REQUEST, true);
    when(mockContext.getAttributes()).thenReturn(contextAttributes);
    Response contextResponse = HttpResponses.notFound();
    when(mockContext.proceed()).thenReturn(contextResponse);
    Response response = underTest.handle(mockContext);
    assert response == contextResponse;
    verify(mockContext).proceed();
    verify(mockNegativeCacheFacet, never()).invalidate(mockNegativeCacheKey);
    verify(mockNegativeCacheFacet, never()).get(any());
  }

  /**
   * Given:
   * - no cached key present
   * - a 404 response from context for GET
   * Then:
   *  - 404 response is cached
   */
  @Test
  public void a404ResponseGetsCachedForGet() throws Exception {
    when(mockRequest.getAction()).thenReturn(HttpMethods.GET);
    verify404Cached();
  }

  /**
   * Given:
   * - no cached key present
   * - a 404 response from context for HEAD
   * Then:
   *  - 404 response is cached
   */
  @Test
  public void a404ResponseGetsCachedForHead() throws Exception {
    when(mockRequest.getAction()).thenReturn(HttpMethods.HEAD);
    verify404Cached();
  }

  /**
   * Given:
   * - cached key present
   * Then:
   *  - cached status is returned
   *  - context is not asked to proceed
   *  - key is not put in cache
   *  - key is not invalidated
   */
  @Test
  public void returnCached404() throws Exception {
    Status cachedStatus = Status.failure(HttpStatus.NOT_FOUND, "404");
    when(mockNegativeCacheFacet.get(mockNegativeCacheKey)).thenReturn(cachedStatus);
    Response response = underTest.handle(mockContext);
    assert response.getStatus() == cachedStatus;
    verify(mockContext, never()).proceed();
    verify(mockNegativeCacheFacet, never()).put(any(NegativeCacheKey.class), any(Status.class));
    verify(mockNegativeCacheFacet, never()).invalidate(any(NegativeCacheKey.class));
  }

  /**
   * Given:
   * - no cached key present
   * - a non 404 response from context
   * Then:
   *  - context is asked to proceed
   *  - response from context is passed on
   *  - key is not put in cache
   *  - key is not invalidated
   */
  @Test
  public void aNon404ResponsePassesThrough() throws Exception {
    Response contextResponse = HttpResponses.serviceUnavailable("503");
    when(mockContext.proceed()).thenReturn(contextResponse);
    when(mockNegativeCacheFacet.get(mockNegativeCacheKey)).thenReturn(null);
    Response response = underTest.handle(mockContext);
    assert response == contextResponse;
    verify(mockContext).proceed();
    verify(mockNegativeCacheFacet, never()).put(any(NegativeCacheKey.class), any(Status.class));
    verify(mockNegativeCacheFacet, never()).invalidate(any(NegativeCacheKey.class));
  }

  /**
   * Given:
   * - no cached key present
   * - successful response from context
   * Then:
   *  - context is asked to proceed
   *  - response from context is passed on
   *  - key is not put in cache
   *  - key is invalidated
   */
  @Test
  public void successfulResponseInvalidatesCache() throws Exception {
    Response contextResponse = HttpResponses.ok("200");
    when(mockContext.proceed()).thenReturn(contextResponse);
    when(mockNegativeCacheFacet.get(mockNegativeCacheKey)).thenReturn(null);
    Response response = underTest.handle(mockContext);
    assert response == contextResponse;
    verify(mockContext).proceed();
    verify(mockNegativeCacheFacet, never()).put(any(NegativeCacheKey.class), any(Status.class));
    verify(mockNegativeCacheFacet).invalidate(any(NegativeCacheKey.class));
  }

  /**
   * Verify 404 response is cached:
   * - context is asked to proceed
   * - response from context is passed on
   * - key is put in cache
   * - key is not invalidated
   */
  void verify404Cached() throws Exception {
    Response contextResponse = HttpResponses.notFound("404");
    when(mockContext.proceed()).thenReturn(contextResponse);
    when(mockNegativeCacheFacet.get(mockNegativeCacheKey)).thenReturn(null);
    Response response = underTest.handle(mockContext);
    assert response == contextResponse;
    verify(mockContext).proceed();
    verify(mockNegativeCacheFacet).put(mockNegativeCacheKey, response.getStatus());
    verify(mockNegativeCacheFacet, never()).invalidate(any(NegativeCacheKey.class));
  }
}
