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
package org.sonatype.nexus.repository.p2.internal.util;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class TempBlobConverterTest
    extends TestSupport
{
  private TempBlobConverter underTest;
  private static final String REAL_PACK_GZ = "com.google.code.atinject.tck_1.1.0.v20160901-1938.jar.pack.gz";

  @Mock
  private TempBlob tempBlob;

  @Mock
  private InputStream inputStream;

  @Before
  public void setUp() throws Exception {
    underTest = new TempBlobConverter();
  }

  @Test
  public void getInputStreamFromPackGzTempBlob() throws Exception {
    InputStream is = getClass().getResourceAsStream(REAL_PACK_GZ);
    when(tempBlob.get()).thenReturn(is);
    InputStream result = underTest.getJarFromPackGz(tempBlob);
    assertThat(result, is(instanceOf(InputStream.class)));
  }

  @Test(expected = IOException.class)
  public void getIOExceptionFromTempBlob() throws Exception {
    when(tempBlob.get()).thenReturn(inputStream);
    when(inputStream.read()).thenThrow(new IOException());
    underTest.getJarFromPackGz(tempBlob);
  }
}
