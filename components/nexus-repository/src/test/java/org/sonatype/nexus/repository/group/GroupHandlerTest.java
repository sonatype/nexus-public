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
import org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.group.GroupHandler.USE_DISPATCHED_RESPONSE;
import static org.sonatype.nexus.repository.http.HttpResponses.forbidden;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;
import static org.sonatype.nexus.repository.http.HttpResponses.ok;
import static org.sonatype.nexus.repository.http.HttpResponses.serviceUnavailable;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;

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

  private GroupHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new GroupHandler();

    when(context.getRequest()).thenReturn(request);
    when(proxy1.getName()).thenReturn("Proxy 1");
    when(proxy1.facet(ViewFacet.class)).thenReturn(viewFacet1);
    when(proxy2.getName()).thenReturn("Proxy 2");
    when(proxy2.facet(ViewFacet.class)).thenReturn(viewFacet2);
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
}
