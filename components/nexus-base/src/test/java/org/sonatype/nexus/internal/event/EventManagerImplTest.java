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
package org.sonatype.nexus.internal.event;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;

import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.inject.DefaultBeanLocator;
import org.junit.Test;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link EventManagerImpl}.
 */
public class EventManagerImplTest
    extends TestSupport
{
  @Test
  public void dispatchOrder() {
    final EventManager underTest = new EventManagerImpl(new DefaultBeanLocator());
    final Handler handler = new Handler(underTest);
    underTest.register(handler);
    underTest.post("a string");
    assertThat(handler.firstCalled, is("handle2"));
  }

  private class Handler
  {
    private final EventManager eventManager;

    private String firstCalled = null;

    Handler(final EventManager eventManager) {
      this.eventManager = eventManager;
    }

    @Subscribe
    public void handle1(final String event) {
      eventManager.post(1);
      if (firstCalled == null) {
        firstCalled = "handle1";
      }
    }

    @Subscribe
    public void handle2(final Integer event) {
      if (firstCalled == null) {
        firstCalled = "handle2";
      }
    }
  }

  @Test
  public void asyncInheritsIsReplicating() {
    final EventManager underTest = new EventManagerImpl(new DefaultBeanLocator());
    final AsyncHandler handler = new AsyncHandler(underTest);
    underTest.register(handler);

    // non-replicating case
    FakeAlmightySubject.forUserId("testUser").execute(
        () -> underTest.post("a string"));

    await().atMost(5, TimeUnit.SECONDS).until(underTest::isCalmPeriod);

    // handled two events, neither were replicating
    assertThat(handler.handledCount.get(), is(2));
    assertThat(handler.replicatingCount.get(), is(0));

    // replicating case
    FakeAlmightySubject.forUserId("testUser").execute(
        () -> EventHelper.asReplicating(
            () -> underTest.post("a string")));

    await().atMost(5, TimeUnit.SECONDS).until(underTest::isCalmPeriod);

    // handled two more events, both were replicating
    assertThat(handler.handledCount.get(), is(4));
    assertThat(handler.replicatingCount.get(), is(2));
  }

  private class AsyncHandler
      implements Asynchronous
  {
    private final EventManager eventManager;

    private AtomicInteger handledCount = new AtomicInteger();

    private AtomicInteger replicatingCount = new AtomicInteger();

    AsyncHandler(final EventManager eventManager) {
      this.eventManager = eventManager;
    }

    @Subscribe
    public void handle1(final String event) {
      eventManager.post(2.0f);

      handledCount.incrementAndGet();
      if (EventHelper.isReplicating()) {
        replicatingCount.incrementAndGet();
      }
    }

    @Subscribe
    public void handle2(final Float event) {
      handledCount.incrementAndGet();
      if (EventHelper.isReplicating()) {
        replicatingCount.incrementAndGet();
      }
    }
  }
}
