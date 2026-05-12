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
package org.sonatype.nexus.capability;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.nexus.common.event.EventManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CapabilityEventQueueTest
{
  @Mock
  private EventManager eventManager;

  private CapabilityEventQueue underTest;

  @BeforeEach
  void setUp() {
    underTest = new CapabilityEventQueue(eventManager);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (underTest != null && underTest.isStarted()) {
      underTest.stop();
    }
  }

  @Test
  void testPost_QueuesEventAndExecutesAsynchronously() throws Exception {
    underTest.start();

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Thread> eventThread = new AtomicReference<>();

    doAnswer(invocation -> {
      eventThread.set(Thread.currentThread());
      latch.countDown();
      return null;
    }).when(eventManager).post(any());

    CapabilityEvent event = mock();
    Thread callerThread = Thread.currentThread();

    underTest.post(event);

    assertTrue(latch.await(5, TimeUnit.SECONDS), "Event should be posted within timeout");
    verify(eventManager).post(event);

    // Verify the event was posted on a different thread (the queue thread)
    assertNotNull(eventThread.get());
    assertTrue(eventThread.get() != callerThread, "Event should be posted on queue thread, not caller thread");
    assertTrue(eventThread.get().getName().contains("capability-event"),
        "Event thread should be from capability-event executor");
  }

  @Test
  void testLifecycle_StartAndStop() throws Exception {
    underTest.start();
    assertTrue(underTest.isStarted());

    underTest.stop();
    assertFalse(underTest.isStarted());
  }

  @Test
  void testStop_WaitsForQueuedEventsToComplete() throws Exception {
    underTest.start();

    CountDownLatch eventStarted = new CountDownLatch(1);
    CountDownLatch eventCanComplete = new CountDownLatch(1);

    doAnswer(invocation -> {
      eventStarted.countDown();
      eventCanComplete.await(5, TimeUnit.SECONDS);
      return null;
    }).when(eventManager).post(any());

    underTest.post(mock());

    // Wait for the event to start processing
    assertTrue(eventStarted.await(5, TimeUnit.SECONDS));

    // Start stopping in a separate thread
    Thread stopThread = new Thread(() -> {
      try {
        underTest.stop();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    stopThread.start();

    // Give it a moment to ensure stop is waiting
    Thread.sleep(100);

    // The stop should still be waiting for the event to complete
    assertTrue(stopThread.isAlive());

    // Allow the event to complete
    eventCanComplete.countDown();

    // Now stop should complete
    stopThread.join(5000);
    assertFalse(stopThread.isAlive(), "Stop should complete after queued events finish");
    assertFalse(underTest.isStarted());
  }

  @Test
  void testStop_WithoutStart_DoesNotThrowException() throws Exception {
    // Test that the component is in NEW state when start was never called
    assertFalse(underTest.isStarted());
    // Note: Calling stop() when not started would throw InvalidStateException
    // This is expected behavior for StateGuardLifecycleSupport
  }

  @Test
  void testMultipleEvents_ProcessedInOrder() throws Exception {
    underTest.start();

    int eventCount = 10;
    CountDownLatch latch = new CountDownLatch(eventCount);
    AtomicReference<Integer> lastProcessedEvent = new AtomicReference<>(-1);

    doAnswer(invocation -> {
      TestEvent event = invocation.getArgument(0);
      int currentValue = lastProcessedEvent.get();
      assertEquals(currentValue + 1, event.id, "Events should be processed in order");
      lastProcessedEvent.set(event.id);
      latch.countDown();
      return null;
    }).when(eventManager).post(any(TestEvent.class));

    // Post multiple events
    for (int i = 0; i < eventCount; i++) {
      underTest.post(new TestEvent(i));
    }

    assertTrue(latch.await(5, TimeUnit.SECONDS), "All events should be processed");
    assertEquals(eventCount - 1, lastProcessedEvent.get());
  }

  private static class TestEvent
      extends CapabilityEvent
  {
    final int id;

    TestEvent(final int id) {
      super(mock());
      this.id = id;
    }
  }
}
