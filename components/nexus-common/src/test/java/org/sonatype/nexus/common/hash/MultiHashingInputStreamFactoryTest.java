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
package org.sonatype.nexus.common.hash;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MultiHashingInputStreamFactoryTest
{
  @Before
  public void teardown() {
    MultiHashingInputStreamFactory.enableParallel();
    MultiHashingInputStreamFactory.setThreshold(-1);
  }

  @Test
  public void testDisableParallel() {
    MultiHashingInputStreamFactory.disableParallel();

    assertThat(MultiHashingInputStreamFactory.input(Collections.emptyList(), in()).getClass(),
        is((Object) MultiHashingInputStream.class));
  }

  @Test
  public void testEnableParallel() {
    MultiHashingInputStreamFactory.disableParallel();
    MultiHashingInputStreamFactory.enableParallel();

    assertThat(MultiHashingInputStreamFactory.input(Collections.emptyList(), in()).getClass(),
        is((Object) ParallelMultiHashingInputStream.class));
  }

  @Test
  public void testThreshold() {
    // default value should result in a parallel
    assertThat(MultiHashingInputStreamFactory.input(Collections.emptyList(), in()).getClass(),
        is((Object) ParallelMultiHashingInputStream.class));

    // White box - we know the threshold is multiplied by parallism resulting in zero, and zero isn't less than the
    // expected zero queued tasks (or if the JVM is using the pool a positive number)
    MultiHashingInputStreamFactory.setThreshold(0);

    assertThat(MultiHashingInputStreamFactory.input(Collections.emptyList(), in()).getClass(),
        is((Object) MultiHashingInputStream.class));
  }

  private static ByteArrayInputStream in() {
    return new ByteArrayInputStream(new byte[0]);
  }
}
