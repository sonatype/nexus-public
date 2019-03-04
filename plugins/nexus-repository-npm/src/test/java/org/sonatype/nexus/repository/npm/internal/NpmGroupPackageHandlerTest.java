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
package org.sonatype.nexus.repository.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.view.Status.success;

public class NpmGroupPackageHandlerTest
    extends TestSupport
{
  @Mock
  private Context context;

  @Mock
  private State state;

  @Mock
  private NpmGroupFacet groupFacet;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private Repository group;

  @Mock
  private Repository proxy;

  @Mock
  private Repository hosted;

  @Mock
  private Request request;

  @Mock
  private ViewFacet viewFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private Asset asset;

  @Mock
  private DispatchedRepositories dispatchedRepositories;

  private NpmGroupPackageHandler underTest;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    underTest = new NpmGroupPackageHandler();

    AttributesMap attributesMap = new AttributesMap();
    attributesMap.set(TokenMatcher.State.class, state);
    when(context.getAttributes()).thenReturn(attributesMap);
    when(context.getRepository()).thenReturn(group);
    when(context.getRequest()).thenReturn(request);

    when(group.getName()).thenReturn(NpmGroupFacetTest.class.getSimpleName() + "-group");
    when(group.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(group.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(groupFacet.buildPackageRoot(anyMap(), eq(context)))
        .thenReturn(new Content(new BytesPayload("test".getBytes(), "")));

    when(viewFacet.dispatch(request, context))
        .thenReturn(new Response.Builder().status(success(OK)).build());

    when(proxy.facet(ViewFacet.class)).thenReturn(viewFacet);
    when(hosted.facet(ViewFacet.class)).thenReturn(viewFacet);

    when(request.getHeaders()).thenReturn(new Headers());
    when(request.getAttributes()).thenReturn(new AttributesMap());

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);

    UnitOfWork.beginBatch(storageTx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldReturnPackageRootForSingleResponse() throws Exception {
    when(groupFacet.members()).thenReturn(singletonList(proxy));

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));

    verify(viewFacet).dispatch(eq(request), eq(context));
    verify(groupFacet).buildPackageRoot(anyMap(), eq(context));
    verify(groupFacet).getFromCache(eq(context));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldReturnPackageRootMultipleResponses() throws Exception {
    when(groupFacet.members()).thenReturn(asList(proxy, hosted));

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));

    verify(viewFacet, times(2)).dispatch(eq(request), eq(context));
    verify(groupFacet).buildPackageRoot(anyMap(), eq(context));
    verify(groupFacet).getFromCache(eq(context));
  }

  @Test
  public void shouldReturnFailureWhenNoResponse() throws Exception {
    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(NOT_FOUND));
    verify(groupFacet).getFromCache(eq(context));
  }

  @Test
  public void shouldReturnFromCache() throws Exception {
    NpmStreamPayload payload = new NpmStreamPayload(() -> new ByteArrayInputStream("test".getBytes()));
    NpmContent cacheContent = new NpmContent(payload);
    when(groupFacet.getFromCache(eq(context))).thenReturn(cacheContent);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), equalTo(cacheContent));

    verify(viewFacet, never()).dispatch(eq(request), eq(context));
    verify(groupFacet).getFromCache(eq(context));

    response = underTest.doGet(context, dispatchedRepositories);
    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), equalTo(cacheContent));

    verify(viewFacet, never()).dispatch(eq(request), eq(context));
    verify(groupFacet, times(2)).getFromCache(eq(context));
  }

  @Test
  public void shouldReturnMergedPackageRoot_When_CacheThrowsMissingBlobException() throws Exception {
    when(groupFacet.getFromCache(eq(context))).thenReturn(createNpmContentWithMissingAssetBlob());
    when(groupFacet.buildMergedPackageRootOnMissingBlob(any(), any(), any()))
        .thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(groupFacet.members()).thenReturn(asList(proxy, hosted));

    Response response = underTest.doGet(context, dispatchedRepositories);
    assertThat(response.getStatus().getCode(), is(OK));

    try (InputStream inputStream = response.getPayload().openInputStream()) {
      assertThat(IOUtils.toString(inputStream), equalTo("test"));
    }
  }

  @Test
  public void shouldReturn_When_CacheThrowsMissingBlobException_And_MembersReturnNoResponse() throws Exception {
    when(groupFacet.getFromCache(eq(context))).thenReturn(createNpmContentWithMissingAssetBlob());
    when(groupFacet.buildMergedPackageRootOnMissingBlob(any(), any(), any())).thenReturn(null);
    when(groupFacet.members()).thenReturn(asList(proxy, hosted));

    when(viewFacet.dispatch(request, context))
        .thenReturn(new Response.Builder().status(success(NOT_FOUND)).build());

    Response response = underTest.doGet(context, dispatchedRepositories);
    assertThat(response.getStatus().getCode(), is(OK));

    try (InputStream inputStream = response.getPayload().openInputStream()) {
      NestedAttributesMap map = NpmJsonUtils.parse(() -> inputStream);
      MatcherAssert.assertThat(map.get("success"), Matchers.equalTo(false));
      MatcherAssert.assertThat(map.get("error"), Matchers.equalTo(
          "Failed to stream response due to: Members had no metadata to merge for repository " + group.getName()));
    }
  }

  private NpmContent createNpmContentWithMissingAssetBlob() {
    NpmStreamPayload payload = new NpmStreamPayload(() -> {
      throw new MissingAssetBlobException(asset);
    });

    return new NpmContent(payload);
  }
}
