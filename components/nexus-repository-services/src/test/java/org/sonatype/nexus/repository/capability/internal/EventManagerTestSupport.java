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
package org.sonatype.nexus.repository.capability.internal;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.ConditionEvent.Satisfied;
import org.sonatype.nexus.capability.ConditionEvent.Unsatisfied;
import org.sonatype.nexus.common.event.EventManager;

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
public class EventManagerTestSupport
    extends TestSupport
{

  @Mock
  protected EventManager eventManager;

  protected List<Object> eventManagerEvents;

  @Before
  public final void setUpEventManager()
      throws Exception
  {
    eventManagerEvents = new ArrayList<Object>();

    doAnswer(new Answer<Object>()
    {

      @Override
      public Object answer(final InvocationOnMock invocation)
          throws Throwable
      {
        eventManagerEvents.add(invocation.getArguments()[0]);
        return null;
      }

    }).when(eventManager).post(any());
  }

  protected void verifyEventManagerEvents(final Matcher... matchers) {
    assertThat(eventManagerEvents, contains(matchers));
  }

  protected void verifyNoEventManagerEvents() {
    assertThat(eventManagerEvents, empty());
  }

  protected static Matcher<Object> satisfied(final Condition condition) {
    return allOf(
        instanceOf(Satisfied.class),
        new ArgumentMatcher<Object>()
        {
          @Override
          public boolean matches(final Object argument) {
            return ((Satisfied) argument).getCondition() == condition;
          }
        }
    );
  }

  protected static Matcher<Object> unsatisfied(final Condition condition) {
    return allOf(
        instanceOf(Unsatisfied.class),
        new ArgumentMatcher<Object>()
        {
          @Override
          public boolean matches(final Object argument) {
            return ((Unsatisfied) argument).getCondition() == condition;
          }
        }
    );
  }

}
