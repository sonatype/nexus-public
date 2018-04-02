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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchGroupHandler;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchParameterExtractor;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponse;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseFactory;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseMapper;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseObject;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponsePackage;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.io.CharStreams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;

public class NpmSearchGroupHandlerTest
    extends TestSupport
{
  static final int MAX_SEARCH_RESULTS = 250;

  @Mock
  NpmSearchParameterExtractor npmSearchParameterExtractor;

  @Mock
  NpmSearchResponseFactory npmSearchResponseFactory;

  @Mock
  NpmSearchResponseMapper npmSearchResponseMapper;

  @Mock
  Context context;

  @Mock
  Request request;

  @Mock
  Parameters parameters;

  @Mock
  Repository repository;

  @Mock
  Repository memberRepository1;

  @Mock
  Repository memberRepository2;

  @Mock
  ViewFacet viewFacet1;

  @Mock
  ViewFacet viewFacet2;

  @Mock
  Response response1;

  @Mock
  Response response2;

  @Mock
  Payload payload1;

  @Mock
  Payload payload2;

  @Mock
  Status status1;

  @Mock
  Status status2;

  @Mock
  InputStream payloadInputStream1;

  @Mock
  InputStream payloadInputStream2;

  @Mock
  NpmSearchResponse npmSearchResponse1;

  @Mock
  NpmSearchResponse npmSearchResponse2;

  @Mock
  NpmSearchResponseObject npmSearchResponseObject1;

  @Mock
  NpmSearchResponseObject npmSearchResponseObject2;

  @Mock
  NpmSearchResponsePackage npmSearchResponsePackage1;

  @Mock
  NpmSearchResponsePackage npmSearchResponsePackage2;

  @Mock
  GroupFacet groupFacet;

  @Mock
  DispatchedRepositories dispatchedRepositories;

  @Mock
  NpmSearchResponse searchResponse;

  NpmSearchGroupHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new NpmSearchGroupHandler(npmSearchParameterExtractor, npmSearchResponseFactory,
        npmSearchResponseMapper, MAX_SEARCH_RESULTS);

    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(request.getParameters()).thenReturn(parameters);

    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    when(groupFacet.members()).thenReturn(asList(memberRepository1, memberRepository2));

    when(memberRepository1.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(memberRepository2.facet(ViewFacet.class)).thenReturn(viewFacet2);

    when(npmSearchResponseFactory.buildEmptyResponse()).thenReturn(searchResponse);
    when(npmSearchResponseFactory.buildResponseForObjects(any(List.class))).then(invocation -> {
      List<NpmSearchResponseObject> objects = (List<NpmSearchResponseObject>) invocation.getArguments()[0];
      NpmSearchResponse response = new NpmSearchResponse();
      response.setObjects(objects);
      response.setTime("Wed Jan 25 2017 19:23:35 GMT+0000 (UTC)");
      response.setTotal(objects.size());
      return response;
    });

    when(npmSearchParameterExtractor.extractText(parameters)).thenReturn("text");
    when(npmSearchParameterExtractor.extractSize(parameters)).thenReturn(20);
    when(npmSearchParameterExtractor.extractFrom(parameters)).thenReturn(0);

    when(npmSearchResponseMapper.writeString(any(NpmSearchResponse.class))).thenReturn("response");

    when(viewFacet1.dispatch(request, context)).thenReturn(response1);
    when(viewFacet2.dispatch(request, context)).thenReturn(response2);

    when(response1.getPayload()).thenReturn(payload1);
    when(response1.getStatus()).thenReturn(status1);

    when(response2.getPayload()).thenReturn(payload2);
    when(response2.getStatus()).thenReturn(status2);

    when(status1.getCode()).thenReturn(OK);
    when(status2.getCode()).thenReturn(OK);

    when(payload1.openInputStream()).thenReturn(payloadInputStream1);
    when(payload2.openInputStream()).thenReturn(payloadInputStream2);

    when(npmSearchResponseMapper.readFromInputStream(payloadInputStream1)).thenReturn(npmSearchResponse1);
    when(npmSearchResponseMapper.readFromInputStream(payloadInputStream2)).thenReturn(npmSearchResponse2);

    when(npmSearchResponse1.getObjects()).thenReturn(singletonList(npmSearchResponseObject1));
    when(npmSearchResponse2.getObjects()).thenReturn(singletonList(npmSearchResponseObject2));

    when(npmSearchResponseObject1.getSearchScore()).thenReturn(0.8);
    when(npmSearchResponseObject1.getPackageEntry()).thenReturn(npmSearchResponsePackage1);

    when(npmSearchResponseObject2.getSearchScore()).thenReturn(0.9);
    when(npmSearchResponseObject2.getPackageEntry()).thenReturn(npmSearchResponsePackage2);

    when(npmSearchResponsePackage1.getName()).thenReturn("package-1");
    when(npmSearchResponsePackage2.getName()).thenReturn("package-2");
  }

  @Test
  public void testSearchWithEmptyString() throws Exception {
    when(npmSearchResponseFactory.buildEmptyResponse()).thenReturn(searchResponse);
    when(npmSearchParameterExtractor.extractText(parameters)).thenReturn("");
    when(npmSearchResponseMapper.writeString(any(NpmSearchResponse.class))).thenReturn("response");

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(npmSearchResponseFactory).buildEmptyResponse();

    verifyNoMoreInteractions(viewFacet1);
    verifyNoMoreInteractions(viewFacet2);
  }

  @Test
  public void testMergingResultsWithDifferentPackageNames() throws Exception {
    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2).dispatch(request, context);

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(2));
    assertThat(searchResponse.getObjects(), hasSize(2));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject2, npmSearchResponseObject1));
  }

  @Test
  public void testMergingResultsWithSamePackageName() throws Exception {
    when(npmSearchResponsePackage2.getName()).thenReturn("package-1");

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2).dispatch(request, context);

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject1));
  }

  @Test
  public void testPropagateRequests() throws Exception {
    when(npmSearchParameterExtractor.extractSize(parameters)).thenReturn(100);
    when(npmSearchParameterExtractor.extractFrom(parameters)).thenReturn(50);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    verify(parameters).replace("from", "0");
    verify(parameters).replace("size", Integer.toString(MAX_SEARCH_RESULTS));

    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2).dispatch(request, context);
  }

  @Test
  public void testSkipResultWithMissingPackage() throws Exception {
    when(npmSearchResponseObject1.getPackageEntry()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithMissingPackageName() throws Exception {
    when(npmSearchResponsePackage1.getName()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithMissingScore() throws Exception {
    when(npmSearchResponseObject1.getSearchScore()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithBadResponse() throws Exception {
    when(status1.getCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject2));
  }

  @Test
  public void testSkipResultWithMissingPayload() throws Exception {
    when(response1.getPayload()).thenReturn(null);

    Response response = underTest.doGet(context, dispatchedRepositories);

    assertThat(response.getStatus().getCode(), is(OK));
    assertThat(response.getPayload(), not(nullValue()));
    try (InputStream in = response.getPayload().openInputStream()) {
      assertThat(CharStreams.toString(new InputStreamReader(in, UTF_8)), is("response"));
    }

    ArgumentCaptor<NpmSearchResponse> searchResponseCaptor = ArgumentCaptor.forClass(NpmSearchResponse.class);
    verify(npmSearchResponseMapper).writeString(searchResponseCaptor.capture());

    NpmSearchResponse searchResponse = searchResponseCaptor.getValue();
    assertThat(searchResponse.getTotal(), is(1));
    assertThat(searchResponse.getObjects(), hasSize(1));
    assertThat(searchResponse.getObjects(), contains(npmSearchResponseObject2));
  }
}
