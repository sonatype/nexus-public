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

import org.sonatype.nexus.repository.ConcurrentOperationException;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ExceptionHandlerTest
{
  @Mock
  private Context context;

  @Mock
  private Request request;

  private ExceptionHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ExceptionHandler();
    when(context.getRepository()).thenReturn(null);
    when(context.getRequest()).thenReturn(request);
    when(request.getAction()).thenReturn("PUT");
    when(request.getPath()).thenReturn("/some/path");
  }

  @Test
  public void concurrentOperationException_returns409() throws Exception {
    when(context.proceed()).thenThrow(new ConcurrentOperationException("Component was concurrently deleted"));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(HttpStatus.CONFLICT));
  }

  @Test
  public void concurrentOperationException_includesMessageInResponse() throws Exception {
    when(context.proceed()).thenThrow(new ConcurrentOperationException("Component was concurrently deleted"));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getMessage(), is("Component was concurrently deleted"));
  }

  @Test
  public void successfulProceed_propagatesResponse() throws Exception {
    when(context.proceed()).thenReturn(
        new Response.Builder().status(org.sonatype.nexus.repository.view.Status.success(HttpStatus.OK)).build());

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(HttpStatus.OK));
  }
}
