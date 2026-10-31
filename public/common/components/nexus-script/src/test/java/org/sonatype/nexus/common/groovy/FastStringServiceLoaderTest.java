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
package org.sonatype.nexus.common.groovy;

import org.apache.groovy.json.internal.FastStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;

public class FastStringServiceLoaderTest
{
  private final FastStringServiceLoader underTest = new FastStringServiceLoader();

  @Test
  public void doStartCompletesWithoutThrowing() throws Exception {
    underTest.doStart();
  }

  @Test
  public void fastStringUtilsStillWorksAfterDoStart() throws Exception {
    underTest.doStart();

    assertArrayEquals(new char[]{'a', 'b'}, FastStringUtils.toCharArray("ab"));
  }

  @Test
  public void doStartIsIdempotentAndCanBeInvokedTwice() throws Exception {
    underTest.doStart();
    underTest.doStart();

    // the observable effect must still hold after a repeated start
    assertArrayEquals(new char[]{'a', 'b'}, FastStringUtils.toCharArray("ab"));
  }

  @Test
  public void doStartRestoresThreadContextClassLoader() throws Exception {
    ClassLoader before = Thread.currentThread().getContextClassLoader();

    underTest.doStart();

    // doStart only mutates the TCCL inside a TcclBlock, which must restore it on exit
    assertSame(before, Thread.currentThread().getContextClassLoader());
  }
}
