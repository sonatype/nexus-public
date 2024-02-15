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
package org.sonatype.nexus.repository.routing.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RoutingRuleHandlerTest
    extends TestSupport
{
  private static final String SOME_PATH = "/some/path";

  private RoutingRuleHandler underTest;

  @Mock
  private RoutingRuleHelper routingRuleHelper;

  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private Response contextResponse;

  @Mock
  private Repository repository;

  @Before
  public void setup() throws Exception {
    underTest = new RoutingRuleHandler(routingRuleHelper);

    when(request.getPath()).thenReturn(SOME_PATH);
    when(context.getRequest()).thenReturn(request);
    when(request.getParameters()).thenReturn(new Parameters());
    when(context.proceed()).thenReturn(contextResponse);
    when(context.getRepository()).thenReturn(repository);
  }

  @Test
  public void testHandle_allowed() throws Exception {
    when(routingRuleHelper.isAllowed(nullable(Repository.class), eq(SOME_PATH))).thenReturn(true);

    Response response = underTest.handle(context);
    assertThat(response, is(contextResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_blocked() throws Exception {
    when(routingRuleHelper.isAllowed(nullable(Repository.class), eq(SOME_PATH))).thenReturn(false);

    Type typeMock = mock(Type.class);
    when(repository.getType()).thenReturn(typeMock);
    when(typeMock.getValue()).thenReturn("repository-type");
    when(repository.getName()).thenReturn("repository-name");

    Response response = underTest.handle(context);

    assertNotNull(response);
    assertThat(response.getStatus().getCode(), is(403));
    verify(context, times(0)).proceed();
  }

  @Test
  public void testHandle_parameters() throws Exception {
    ListMultimap<String, String> params = LinkedListMultimap.create();
    params.put("foo", "bar");
    params.put("bar", "foo");
    when(request.getParameters()).thenReturn(new Parameters(params));
    when(routingRuleHelper.isAllowed(nullable(Repository.class), eq("/some/path?foo=bar&bar=foo"))).thenReturn(true);

    Response response = underTest.handle(context);
    assertThat(response, is(contextResponse));
    verify(context).proceed();
    verify(routingRuleHelper).isAllowed(nullable(Repository.class), eq("/some/path?foo=bar&bar=foo"));
  }
}
