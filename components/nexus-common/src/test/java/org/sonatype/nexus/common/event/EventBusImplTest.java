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

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.eventbus.ReentrantEventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link EventBusImpl}.
 */
public class EventBusImplTest
    extends TestSupport
{
  @Test
  public void dispatchOrder() {
    final EventBus underTest = new EventBusImpl(new ReentrantEventBus());
    final Handler handler = new Handler(underTest);
    underTest.register(handler);
    underTest.post("a string");
    assertThat(handler.firstCalled, is("handle2"));
  }

  private class Handler
  {
    private final EventBus eventBus;

    private String firstCalled = null;

    Handler(final EventBus eventBus) {
      this.eventBus = eventBus;
    }

    @Subscribe
    public void handle1(final String event) {
      eventBus.post(1);
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
}
