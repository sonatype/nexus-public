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
package org.sonatype.nexus.repository.group;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.group.GroupHandler.IQ_MEMBER_REPO_NAME;
import static org.sonatype.nexus.repository.group.GroupHandler.USE_DISPATCHED_RESPONSE;
import static org.sonatype.nexus.repository.http.HttpResponses.forbidden;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;
import static org.sonatype.nexus.repository.http.HttpResponses.ok;
import static org.sonatype.nexus.repository.http.HttpResponses.serviceUnavailable;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_NAME;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_VALUE;

public class GroupHandlerTest
    extends TestSupport
{
  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private Repository proxy1;

  @Mock
  private Repository proxy2;

  @Mock
  private ViewFacet viewFacet1;

  @Mock
  private ViewFacet viewFacet2;

  @Mock
  private Configuration configuration1;

  @Mock
  private Configuration configuration2;

  private GroupHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new GroupHandler();

    when(context.getRequest()).thenReturn(request);
    when(proxy1.getName()).thenReturn("Proxy 1");
    when(proxy1.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(proxy1.getType()).thenReturn(new ProxyType());
    when(proxy1.getConfiguration()).thenReturn(configuration1);
    when(configuration1.isOnline()).thenReturn(true);
    when(proxy2.getName()).thenReturn("Proxy 2");
    when(proxy2.facet(ViewFacet.class)).thenReturn(viewFacet2);
    when(proxy2.getType()).thenReturn(new ProxyType());
    when(proxy2.getConfiguration()).thenReturn(configuration2);
    when(configuration2.isOnline()).thenReturn(true);
  }

  @Test
  public void whenAllRepositoryReturnOkThenGroupReturnsOk() throws Exception {
    Response ok1 = ok();
    setupDispatch(ok1, ok());

    assertGetFirst(ok1);
  }

  @Test
  public void whenAnyRepositoryReturnsOkThenGroupReturnsOk() throws Exception {
    Response ok2 = ok();
    setupDispatch(notFound(), ok2);

    assertGetFirst(ok2);
  }

  @Test
  public void whenAllRepositoriesReturnNotFoundThenGroupReturnsNotFound() throws Exception {
    setupDispatch(notFound(), notFound());

    assertGetFirstNotFound(asList(proxy1, proxy2));
  }

  @Test
  public void whenAnyRepositoryReturnsNotOkThenGroupReturnsNotFound() throws Exception {
    setupDispatch(forbidden(), forbidden());

    assertGetFirstNotFound(asList(proxy1, proxy2));

    setupDispatch(forbidden(), serviceUnavailable());

    assertGetFirstNotFound(asList(proxy1, proxy2));
  }

  @Test
  public void whenNoRepositoriesInGroupThenGroupReturnsNotFound() throws Exception {
    assertGetFirstNotFound(emptyList());
  }

  @Test
  public void returnsFirstOkOrFirstUseDispatchedResponse() throws Exception {
    Response ok = ok();
    Response forbidden1 = forbidden();

    forbidden1.getAttributes().set(USE_DISPATCHED_RESPONSE, true);
    setupDispatch(ok, forbidden1);

    assertGetFirst(ok);
    verify(viewFacet1).dispatch(request, context);
    verify(viewFacet2, times(0)).dispatch(request, context);

    setupDispatch(forbidden1, ok);

    assertGetFirst(forbidden1);
    verify(viewFacet1, times(2)).dispatch(request, context);
    verify(viewFacet2, times(0)).dispatch(request, context);
  }

  @Test
  public void returnsFirstOkOrFirstBypassHttpErrorsHeaderResponse() throws Exception {
    Response forbidden = forbidden();
    forbidden.getHeaders().set(BYPASS_HTTP_ERRORS_HEADER_NAME, BYPASS_HTTP_ERRORS_HEADER_VALUE);

    Response ok = ok();

    setupDispatch(forbidden, ok);
    assertGetFirst(forbidden);
    verify(viewFacet1, times(1)).dispatch(request, context);
    verify(viewFacet2, times(0)).dispatch(request, context);

    setupDispatch(ok, forbidden);
    assertGetFirst(ok);
    verify(viewFacet1, times(2)).dispatch(request, context);
    verify(viewFacet2, times(0)).dispatch(request, context);
  }

  private void setupDispatch(final Response response1, final Response response2) throws Exception {
    when(viewFacet1.dispatch(request, context)).thenReturn(response1);
    when(viewFacet2.dispatch(request, context)).thenReturn(response2);
  }

  private void assertGetFirst(final Response expectedResponse) throws Exception {
    assertThat(underTest.getFirst(context, asList(proxy1, proxy2), new DispatchedRepositories()), is(expectedResponse));
  }

  private void assertGetFirstNotFound(final List<Repository> repositories) throws Exception {
    Response response = underTest.getFirst(context, repositories, new DispatchedRepositories());
    assertThat(response.getStatus().getCode(), is(NOT_FOUND));
  }

  @Test
  public void setsRespondingRepositoryAttributeWhenMemberRespondsSuccessfully() throws Exception {
    Response ok1 = ok();
    setupDispatch(ok1, ok());

    underTest.getFirst(context, asList(proxy1, proxy2), new DispatchedRepositories());

    verify(context).setAttribute(eq(IQ_MEMBER_REPO_NAME), eq("Proxy 1"));
  }

  @Test
  public void setsRespondingRepositoryAttributeForSecondMemberWhenFirstFails() throws Exception {
    Response ok2 = ok();
    setupDispatch(notFound(), ok2);

    underTest.getFirst(context, asList(proxy1, proxy2), new DispatchedRepositories());

    verify(context).setAttribute(eq(IQ_MEMBER_REPO_NAME), eq("Proxy 2"));
  }

  @Test
  public void doesNotSetRespondingRepositoryAttributeWhenNoMemberRespondsSuccessfully() throws Exception {
    setupDispatch(notFound(), notFound());

    underTest.getFirst(context, asList(proxy1, proxy2), new DispatchedRepositories());

    verify(context, times(0)).setAttribute(eq(IQ_MEMBER_REPO_NAME), eq("Proxy 1"));
    verify(context, times(0)).setAttribute(eq(IQ_MEMBER_REPO_NAME), eq("Proxy 2"));
  }

  @Test
  public void setsRespondingRepositoryAttributeForUseDispatchedResponse() throws Exception {
    Response forbidden1 = forbidden();
    forbidden1.getAttributes().set(USE_DISPATCHED_RESPONSE, true);
    setupDispatch(forbidden1, ok());

    underTest.getFirst(context, asList(proxy1, proxy2), new DispatchedRepositories());

    verify(context).setAttribute(eq(IQ_MEMBER_REPO_NAME), eq("Proxy 1"));
  }

  @Test
  public void setsRespondingRepositoryAttributeForBypassHttpErrorsHeader() throws Exception {
    Response forbidden = forbidden();
    forbidden.getHeaders().set(BYPASS_HTTP_ERRORS_HEADER_NAME, BYPASS_HTTP_ERRORS_HEADER_VALUE);
    setupDispatch(forbidden, ok());

    underTest.getFirst(context, asList(proxy1, proxy2), new DispatchedRepositories());

    verify(context).setAttribute(eq(IQ_MEMBER_REPO_NAME), eq("Proxy 1"));
  }

  @Test
  public void doesNotSetRespondingRepositoryAttributeForHostedMember() throws Exception {
    Repository hosted = mock(Repository.class);
    Type hostedType = mock(Type.class);
    when(hostedType.getValue()).thenReturn(HostedType.NAME);
    when(hosted.getType()).thenReturn(hostedType);
    when(hosted.getName()).thenReturn("Hosted 1");
    ViewFacet hostedViewFacet = mock(ViewFacet.class);
    when(hosted.facet(ViewFacet.class)).thenReturn(hostedViewFacet);
    Configuration hostedConfig = mock(Configuration.class);
    when(hosted.getConfiguration()).thenReturn(hostedConfig);
    when(hostedConfig.isOnline()).thenReturn(true);

    Response ok = ok();
    when(hostedViewFacet.dispatch(request, context)).thenReturn(ok);

    underTest.getFirst(context, asList(hosted), new DispatchedRepositories());

    verify(context, times(0)).setAttribute(eq(IQ_MEMBER_REPO_NAME), any());
  }

  @Test
  public void skipsOfflineMemberRepositories() throws Exception {
    // Mark first proxy as OFFLINE
    when(configuration1.isOnline()).thenReturn(false);

    Response ok2 = ok();
    when(viewFacet2.dispatch(request, context)).thenReturn(ok2);

    assertGetFirst(ok2);

    // Verify first proxy was never dispatched to since it's offline
    verify(viewFacet1, times(0)).dispatch(request, context);
    // Verify second proxy was dispatched to
    verify(viewFacet2, times(1)).dispatch(request, context);
  }

  @Test
  public void returnsNotFoundWhenAllMembersAreOffline() throws Exception {
    // Mark both proxies as OFFLINE
    when(configuration1.isOnline()).thenReturn(false);
    when(configuration2.isOnline()).thenReturn(false);

    assertGetFirstNotFound(asList(proxy1, proxy2));

    // Verify neither proxy was dispatched to since both are offline
    verify(viewFacet1, times(0)).dispatch(request, context);
    verify(viewFacet2, times(0)).dispatch(request, context);
  }

  @Test
  public void getAllSkipsAlreadyDispatchedMembersWhenParentIsFresh() throws Exception {
    // Test that when isStale=false (parent cache is fresh), already-dispatched members are skipped
    Repository parentGroup = mock(Repository.class);
    when(parentGroup.getName()).thenReturn("Parent Group");
    when(context.getRepository()).thenReturn(parentGroup);

    Repository childGroup = mock(Repository.class);
    when(childGroup.getName()).thenReturn("Child Group");
    when(childGroup.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(childGroup.getType()).thenReturn(new org.sonatype.nexus.repository.types.GroupType());
    Configuration groupConfig = mock(Configuration.class);
    when(childGroup.getConfiguration()).thenReturn(groupConfig);
    when(groupConfig.isOnline()).thenReturn(true);

    DispatchedRepositories dispatched = new DispatchedRepositories();
    dispatched.add(childGroup); // Mark child group as already dispatched

    Response ok1 = ok();
    Response ok2 = ok();
    when(viewFacet1.dispatch(request, context)).thenReturn(ok1);
    when(viewFacet2.dispatch(request, context)).thenReturn(ok2);

    // Call getAll with isStale=false (parent is fresh)
    underTest.getAll(context, asList(childGroup, proxy2), dispatched, false);

    // Verify the already-dispatched group was skipped
    verify(viewFacet1, times(0)).dispatch(request, context);
    // Verify proxy2 was still dispatched
    verify(viewFacet2, times(1)).dispatch(request, context);
  }

  @Test
  public void getAllRefetchesAlreadyDispatchedMembersWhenParentIsStale() throws Exception {
    // Test that when isStale=true (parent cache is stale), already-dispatched members are re-fetched
    Repository parentGroup = mock(Repository.class);
    when(parentGroup.getName()).thenReturn("Parent Group");
    when(context.getRepository()).thenReturn(parentGroup);

    Repository childGroup = mock(Repository.class);
    when(childGroup.getName()).thenReturn("Child Group");
    when(childGroup.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(childGroup.getType()).thenReturn(new org.sonatype.nexus.repository.types.GroupType());
    Configuration groupConfig = mock(Configuration.class);
    when(childGroup.getConfiguration()).thenReturn(groupConfig);
    when(groupConfig.isOnline()).thenReturn(true);

    DispatchedRepositories dispatched = new DispatchedRepositories();
    dispatched.add(childGroup); // Mark child group as already dispatched

    Response ok1 = ok();
    Response ok2 = ok();
    when(viewFacet1.dispatch(request, context)).thenReturn(ok1);
    when(viewFacet2.dispatch(request, context)).thenReturn(ok2);

    // Call getAll with isStale=true (parent cache is stale)
    underTest.getAll(context, asList(childGroup, proxy2), dispatched, true);

    // Verify the already-dispatched group was re-fetched (not skipped)
    verify(viewFacet1, times(1)).dispatch(request, context);
    // Verify proxy2 was also dispatched
    verify(viewFacet2, times(1)).dispatch(request, context);
  }

  @Test
  public void getAllWithDefaultIsStaleBehavior() throws Exception {
    // Test that the default behavior (without isStale parameter) is isStale=false
    Repository parentGroup = mock(Repository.class);
    when(parentGroup.getName()).thenReturn("Parent Group");
    when(context.getRepository()).thenReturn(parentGroup);

    Repository childGroup = mock(Repository.class);
    when(childGroup.getName()).thenReturn("Child Group");
    when(childGroup.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(childGroup.getType()).thenReturn(new org.sonatype.nexus.repository.types.GroupType());
    Configuration groupConfig = mock(Configuration.class);
    when(childGroup.getConfiguration()).thenReturn(groupConfig);
    when(groupConfig.isOnline()).thenReturn(true);

    DispatchedRepositories dispatched = new DispatchedRepositories();
    dispatched.add(childGroup);

    Response ok1 = ok();
    Response ok2 = ok();
    when(viewFacet1.dispatch(request, context)).thenReturn(ok1);
    when(viewFacet2.dispatch(request, context)).thenReturn(ok2);

    // Call getAll without isStale parameter (should default to false)
    underTest.getAll(context, asList(childGroup, proxy2), dispatched);

    // Verify already-dispatched member was skipped (default behavior)
    verify(viewFacet1, times(0)).dispatch(request, context);
    verify(viewFacet2, times(1)).dispatch(request, context);
  }

  @Test
  public void getAllPreservesDispatchedTrackingWithIsStale() throws Exception {
    // Test that isStale=true still adds newly dispatched members to the tracker
    Response ok1 = ok();
    Response ok2 = ok();
    when(viewFacet1.dispatch(request, context)).thenReturn(ok1);
    when(viewFacet2.dispatch(request, context)).thenReturn(ok2);

    DispatchedRepositories dispatched = new DispatchedRepositories();

    // Call getAll with isStale=true
    underTest.getAll(context, asList(proxy1, proxy2), dispatched, true);

    // Verify both were added to dispatched tracker
    assertThat(dispatched.contains(proxy1), is(true));
    assertThat(dispatched.contains(proxy2), is(true));
  }
}
