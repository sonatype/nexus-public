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
package org.sonatype.nexus.capability.condition;

import org.sonatype.nexus.common.app.NexusStartedEvent;
import org.sonatype.nexus.common.app.NexusStoppedEvent;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * {@link NexusIsActiveCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class NexusIsActiveConditionTest
    extends EventBusTestSupport
{

  private NexusIsActiveCondition underTest;

  @Before
  public final void setUpNexusIsActiveCondition()
      throws Exception
  {
    underTest = new NexusIsActiveCondition(eventBus);
  }

  /**
   * Condition is not satisfied initially.
   */
  @Test
  public void notSatisfiedInitially() {
    assertThat(underTest.isSatisfied(), is(false));
  }

  /**
   * Condition is satisfied when Nexus is started.
   */
  @Test
  public void satisfiedWhenNexusStarted() {
    underTest.handle(new NexusStartedEvent(this));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest));
  }

  /**
   * Condition is satisfied when negated is not satisfied.
   */
  @Test
  public void unsatisfiedWhenNexusStopped() {
    underTest.handle(new NexusStartedEvent(this));
    underTest.handle(new NexusStoppedEvent(this));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
  }

}
