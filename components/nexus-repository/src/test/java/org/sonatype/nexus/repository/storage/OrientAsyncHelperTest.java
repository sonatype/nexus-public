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
package org.sonatype.nexus.repository.storage;

import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.OrientAsyncHelper.QueueConsumingIterable;
import org.sonatype.nexus.repository.storage.OrientAsyncHelper.QueueFeedingResultListener;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OrientAsyncHelperTest
    extends TestSupport
{
  @Mock
  private ODocument result;

  private BlockingQueue<ODocument> newQueue(ODocument... elements) {
    return new ArrayBlockingQueue<>(Math.max(1, elements.length), false, asList(elements));
  }

  @Test
  public void testIteratorHasNext_NonEmptyQueue() {
    QueueConsumingIterable underTest = new QueueConsumingIterable(1, newQueue(result));
    assertThat(underTest.hasNext(), is(true));
  }

  @Test
  public void testIteratorHasNext_EmptyQueue() {
    QueueConsumingIterable underTest = new QueueConsumingIterable(1, newQueue(OrientAsyncHelper.SENTINEL));
    assertThat(underTest.hasNext(), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void testIteratorHasNext_Timeout() {
    QueueConsumingIterable underTest = new QueueConsumingIterable(1, newQueue());
    underTest.hasNext();
  }

  @Test
  public void testIteratorNext_NonEmptyQueue() {
    QueueConsumingIterable underTest = new QueueConsumingIterable(1, newQueue(result));
    assertThat(underTest.next(), is(result));
  }

  @Test(expected = NoSuchElementException.class)
  public void testIteratorNext_EmptyQueue() {
    QueueConsumingIterable underTest = new QueueConsumingIterable(1, newQueue(OrientAsyncHelper.SENTINEL));
    underTest.next();
  }

  @Test(expected = IllegalStateException.class)
  public void testIteratorNext_Timeout() {
    QueueConsumingIterable underTest = new QueueConsumingIterable(1, newQueue());
    underTest.next();
  }

  @Test
  public void testCommandResultListenerResult_NonFullQueue() {
    BlockingQueue<ODocument> queue = newQueue();
    QueueFeedingResultListener underTest = new QueueFeedingResultListener(1, queue);
    assertThat(underTest.result(result), is(true));
    assertThat(queue.poll(), is(result));
  }

  @Test
  public void testCommandResultListenerResult_FullQueue() {
    QueueFeedingResultListener underTest = new QueueFeedingResultListener(1, newQueue(result));
    assertThat(underTest.result(result), is(false));
  }

  @Test
  public void testCommandResultListenerEnd() {
    BlockingQueue<ODocument> queue = newQueue();
    QueueFeedingResultListener underTest = new QueueFeedingResultListener(1, queue);
    underTest.end();
    assertThat(queue.poll(), is(OrientAsyncHelper.SENTINEL));
  }

  @Test
  public void testCustomOrientQueryTimeoutApplied() {
    System.getProperties().setProperty("nexus.orient.query.timeout.seconds", "220");
    assertThat(OrientAsyncHelper.queryTimeout, is(220L));
  }

}
