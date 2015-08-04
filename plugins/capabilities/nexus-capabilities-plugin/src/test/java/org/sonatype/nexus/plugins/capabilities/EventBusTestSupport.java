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
package org.sonatype.nexus.plugins.capabilities;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Support for tests using event bus.
 *
 * @since capabilities 2.0
 */
public class EventBusTestSupport
    extends TestSupport
{

  @Mock
  protected EventBus eventBus;

  protected List<Object> eventBusEvents;

  @Before
  public final void setUpEventBus()
      throws Exception
  {
    eventBusEvents = new ArrayList<Object>();

    doAnswer(new Answer<Object>()
    {

      @Override
      public Object answer(final InvocationOnMock invocation)
          throws Throwable
      {
        eventBusEvents.add(invocation.getArguments()[0]);
        return null;
      }

    }).when(eventBus).post(any());
  }

  protected void verifyEventBusEvents(final Matcher... matchers) {
    assertThat(eventBusEvents, contains(matchers));
  }

  protected void verifyNoEventBusEvents() {
    assertThat(eventBusEvents, empty());
  }

  protected static Matcher<Object> satisfied(final Condition condition) {
    return allOf(
        instanceOf(ConditionEvent.Satisfied.class),
        new ArgumentMatcher<Object>()
        {
          @Override
          public boolean matches(final Object argument) {
            return ((ConditionEvent.Satisfied) argument).getCondition() == condition;
          }
        }
    );
  }

  protected static Matcher<Object> unsatisfied(final Condition condition) {
    return allOf(
        instanceOf(ConditionEvent.Unsatisfied.class),
        new ArgumentMatcher<Object>()
        {
          @Override
          public boolean matches(final Object argument) {
            return ((ConditionEvent.Unsatisfied) argument).getCondition() == condition;
          }
        }
    );
  }

}
