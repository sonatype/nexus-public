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
package org.sonatype.nexus.repository.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.net.HttpHeaders;

/**
 * Tests {@link PartialFetchHandler}.
 */
public class PartialFetchHandlerTest
    extends TestSupport
{
  final PartialFetchHandler subject = new PartialFetchHandler(new RangeParser());

  final String payloadString = "testPayload";

  @Mock
  Context context;

  @Test
  public void contentRangeMustStartWithBytesPrefix() throws Exception {
    Request request = new Request.Builder().action(HttpMethods.GET).header(HttpHeaders.RANGE, "bytes=0-1").path("/foo").build();
    when(context.getRequest()).thenReturn(request);

    final Content content = new Content(new StringPayload(payloadString, "text/plain"));
    when(context.proceed()).thenReturn(HttpResponses.ok(content));

    final Response r = subject.handle(context);
    assertThat(r.getStatus().isSuccessful(), is(true));
    assertThat(r.getHeaders().get(HttpHeaders.CONTENT_RANGE), equalTo("bytes 0-1/" + content.getSize()));
  }
}
