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
package org.sonatype.nexus.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @since 3.20
 */
public class NexusForkJoinWorkerThreadFactoryTest
{
  private NexusForkJoinWorkerThreadFactory nexusForkJoinWorkerThreadFactory;

  @Test
  public void prefixIsAddedToThread() {
    nexusForkJoinWorkerThreadFactory = new NexusForkJoinWorkerThreadFactory("prefix-test");
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    ForkJoinWorkerThread thread = nexusForkJoinWorkerThreadFactory.newThread(forkJoinPool);
    assertThat(thread.getName(), CoreMatchers.containsString("prefix-test"));
  }

}
