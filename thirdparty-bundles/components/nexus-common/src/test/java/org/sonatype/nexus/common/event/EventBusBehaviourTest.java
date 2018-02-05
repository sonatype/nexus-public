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
package org.sonatype.nexus.common.event;

import java.util.ArrayList;
import java.util.List;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.sonatype.nexus.common.event.EventBusFactory.reentrantEventBus;

/**
 * Tests different EventBus behaviour.
 */
public class EventBusBehaviourTest
{
  EventBus eventBus;

  List<String> recorded;

  @Before
  public void setUp() {
    recorded = new ArrayList<>();
  }

  @After
  public void tearDown() {
    recorded = null;
  }

  synchronized void recordEnter(Object event) {
    recorded.add(event.getClass().getSimpleName() + " -->");
  }

  synchronized void recordLeave(Object event) {
    recorded.add("<-- " + event.getClass().getSimpleName());
  }

  @Test
  public void verifyStandardEventBusBehaviour() {

    eventBus = new EventBus();
    eventBus.register(new Subscriber1());
    eventBus.register(new Subscriber2());
    eventBus.post(new EventA());

    assertThat(recorded, contains("EventA -->", "<-- EventA", "EventB -->", "<-- EventB", "EventC -->", "<-- EventC"));
  }

  @Test
  public void verifyReentrantEventBusBehaviour() {

    eventBus = reentrantEventBus("test");
    eventBus.register(new Subscriber1());
    eventBus.register(new Subscriber2());
    eventBus.post(new EventA());

    assertThat(recorded, contains("EventA -->", "EventB -->", "EventC -->", "<-- EventC", "<-- EventB", "<-- EventA"));
  }

  static class EventA
  {
    // empty
  }

  static class EventB
  {
    // empty
  }

  static class EventC
  {
    // empty
  }

  class Subscriber1
  {
    @Subscribe
    public void on(EventA event) {
      recordEnter(event);
      eventBus.post(new EventB());
      recordLeave(event);
    }

    @Subscribe
    public void on(EventC event) {
      recordEnter(event);
      recordLeave(event);
    }
  }

  class Subscriber2
  {
    @Subscribe
    public void on(EventB event) {
      recordEnter(event);
      eventBus.post(new EventC());
      recordLeave(event);
    }
  }
}
