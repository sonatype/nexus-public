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
package org.sonatype.nexus.blobstore;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PerformanceLoggingInputStreamTest
    extends TestSupport
{
  @Mock
  private InputStream source;

  @Mock
  private PerformanceLogger logger;

  @InjectMocks
  private PerformanceLoggingInputStream underTest;

  @Test
  public void shouldPassReadsAndCloseToUnderlyingInputStream() throws IOException {
    byte[] buffer1 = new byte[10];
    byte[] buffer2 = new byte[10];

    when(source.read()).thenReturn(123);
    when(source.read(buffer1)).thenReturn(99);
    when(source.read(buffer2, 7, 29)).thenReturn(29);

    assertThat(underTest.read(), is(123));
    assertThat(underTest.read(buffer1), is(99));
    assertThat(underTest.read(buffer2, 7, 29), is(29));
    underTest.close();
    verify(source).close();
  }

  @Test
  public void performanceDataIsLoggedOnClose() throws IOException {
    underTest.close();
    verify(logger).logRead(0, 0);
  }
}
