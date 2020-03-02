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
package org.sonatype.nexus.repository.view.handlers;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UT for {@link HandlerContributor}.
 */
public class HandlerContributorTest
    extends TestSupport
{
  @Test
  public void addContributedHandlers() throws Exception {
    final AttributesMap attributes = new AttributesMap();

    final Request request = mock(Request.class);
    final Context context = mock(Context.class);

    when(context.getRequest()).thenReturn(request);
    when(context.getAttributes()).thenReturn(attributes);
    when(context.proceed()).thenReturn(HttpResponses.ok(new Content(new StringPayload("test", "text/plain"))));

    final ContributedHandler handlerA = mock(ContributedHandler.class);
    final ContributedHandler handlerB = mock(ContributedHandler.class);
    final HandlerContributor underTest = new HandlerContributor(asList(handlerA, handlerB));
    final Response response = underTest.handle(context);

    assertThat(response.getStatus().isSuccessful(), is(true));
    assertThat(attributes.get(HandlerContributor.EXTENDED_MARKER), is(Boolean.TRUE));

    // Handle a second time to ensure the contributed handlers aren't injected twice
    underTest.handle(context);

    ArgumentCaptor<ContributedHandler> handlerCaptor = ArgumentCaptor.forClass(ContributedHandler.class);
    verify(context, times(2)).insertHandler(handlerCaptor.capture());
    assertThat(handlerCaptor.getAllValues(), is(asList(handlerB, handlerA))); // Intentionally added in "reverse" order
  }
}
