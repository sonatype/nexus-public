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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.inject.DefaultBeanLocator;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link EventManagerImpl}.
 */
public class EventManagerImplTest
    extends TestSupport
{
  @Test
  public void dispatchOrder() {
    EventManager underTest = new EventManagerImpl(new DefaultBeanLocator(), newEventExecutor());
    ReentrantHandler handler = new ReentrantHandler(underTest);

    underTest.register(handler);
    underTest.post("a string");

    assertThat(handler.firstCalled, is("handle2"));
  }

  private class ReentrantHandler
  {
    private final EventManager eventManager;

    private String firstCalled = null;

    ReentrantHandler(final EventManager eventManager) {
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
  public void asyncInheritsIsReplicating() throws Exception {
    EventExecutor executor = newEventExecutor();
    EventManager underTest = new EventManagerImpl(new DefaultBeanLocator(), executor);
    AsyncReentrantHandler handler = new AsyncReentrantHandler(underTest);
    underTest.register(handler);

    executor.start(); // enable multi-threaded mode

    // non-replicating case
    FakeAlmightySubject.forUserId("testUser")
        .execute(
            () -> underTest.post("a string"));

    await().atMost(5, TimeUnit.SECONDS).until(underTest::isCalmPeriod);

    // handled two events, neither were replicating
    assertThat(handler.handledCount.get(), is(2));
    assertThat(handler.replicatingCount.get(), is(0));

    // replicating case
    FakeAlmightySubject.forUserId("testUser")
        .execute(
            () -> EventHelper.asReplicating(
                () -> underTest.post("a string")));

    await().atMost(5, TimeUnit.SECONDS).until(underTest::isCalmPeriod);

    // handled two more events, both were replicating
    assertThat(handler.handledCount.get(), is(4));
    assertThat(handler.replicatingCount.get(), is(2));

    executor.stop(); // go back to single-threaded mode
  }

  private class AsyncReentrantHandler
      implements Asynchronous
  {
    private final EventManager eventManager;

    private AtomicInteger handledCount = new AtomicInteger();

    private AtomicInteger replicatingCount = new AtomicInteger();

    AsyncReentrantHandler(final EventManager eventManager) {
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

  @Test
  public void singleThreadedOnShutdown() throws Exception {
    EventExecutor executor = newEventExecutor();
    EventManager underTest = new EventManagerImpl(new DefaultBeanLocator(), executor);
    AsyncHandler handler = new AsyncHandler();
    underTest.register(handler);

    FakeAlmightySubject.forUserId("testUser").execute(() -> underTest.post("first"));

    // executor is initially in single-threaded mode

    assertThat(handler.handledByThread, hasSize(1));
    assertThat(handler.handledByThread.get(0), is(Thread.currentThread()));

    executor.start(); // enable multi-threaded mode

    FakeAlmightySubject.forUserId("testUser").execute(() -> underTest.post("foo"));
    FakeAlmightySubject.forUserId("testUser").execute(() -> underTest.post("bar"));

    executor.stop(); // waits for threads to finish

    assertThat(handler.handledByThread, hasSize(3));
    assertThat(handler.handledByThread.get(1), is(not(Thread.currentThread())));
    assertThat(handler.handledByThread.get(2), is(not(Thread.currentThread())));
    assertThat(handler.handledByThread.get(1), is(not(handler.handledByThread.get(2))));

    // executor is now back in single-threaded mode

    FakeAlmightySubject.forUserId("testUser").execute(() -> underTest.post("last"));

    assertThat(handler.handledByThread, hasSize(4));
    assertThat(handler.handledByThread.get(3), is(Thread.currentThread()));
  }

  @Test
  public void singleThreadedOnShutdownWhenReplicating() throws Exception {
    EventHelper.asReplicating(() -> {
      try {
        singleThreadedOnShutdown();
      }
      catch (Exception e) {
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    });
  }

  private static EventExecutor newEventExecutor() {
    return new EventExecutor(false, 0, Time.seconds(0), false, false);
  }

  private class AsyncHandler
      implements Asynchronous
  {
    private List<Thread> handledByThread = new CopyOnWriteArrayList<>();

    @Subscribe
    public void handle(final String event) throws Exception {
      handledByThread.add(Thread.currentThread());
      Thread.sleep(100); // make sure events are handled by different threads from the pool
    }
  }
}
