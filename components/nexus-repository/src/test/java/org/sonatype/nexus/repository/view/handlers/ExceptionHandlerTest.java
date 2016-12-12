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
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;

import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.orient.testsupport.OrientExceptionMocker.mockOrientException;

public class ExceptionHandlerTest
    extends TestSupport
{
  ExceptionHandler underTest;

  Exception frozenException;

  @Mock
  Context context;

  @Mock
  Request request;

  @Before
  public void setUp() throws Exception {
    when(context.getRequest()).thenReturn(request);
    when(request.getAction()).thenReturn("GET");
    when(request.getPath()).thenReturn("/test");

    underTest = new ExceptionHandler();

    frozenException = mockOrientException(OModificationOperationProhibitedException.class);
  }

  @Test
  public void handleIllegalOperation() throws Exception {
    when(context.proceed()).thenThrow(new IllegalOperationException("That operation was illegal."));
    assertThat(underTest.handle(context).getStatus().getCode(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void handleInvalidContent() throws Exception {
    when(context.proceed()).thenThrow(new InvalidContentException("That content was invalid"));
    assertThat(underTest.handle(context).getStatus().getCode(), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void handleInvalidContentPut() throws Exception {
    when(request.getAction()).thenReturn("PUT");
    when(context.proceed()).thenThrow(new InvalidContentException("That content was invalid"));
    assertThat(underTest.handle(context).getStatus().getCode(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void handleOModificationOperationProhibited() throws Exception {
    when(context.proceed()).thenThrow(frozenException);
    assertThat(underTest.handle(context).getStatus().getCode(), is(HttpStatus.SERVICE_UNAVAILABLE));
  }
}
