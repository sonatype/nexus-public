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

import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.ConditionEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InversionCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class InversionConditionTest
    extends EventBusTestSupport
{

  @Mock
  private Condition condition;

  private InversionCondition underTest;

  @Before
  public final void setUpInversionCondition()
      throws Exception
  {
    underTest = new InversionCondition(eventBus, condition);
    underTest.bind();

    verify(eventBus).register(underTest);
  }

  /**
   * Condition is satisfied initially (because mock returns false).
   */
  @Test
  public void not01() {
    assertThat(underTest.isSatisfied(), is(true));
  }

  /**
   * Condition is not satisfied when negated is satisfied.
   */
  @Test
  public void not02() {
    when(condition.isSatisfied()).thenReturn(true);
    underTest.handle(new ConditionEvent.Satisfied(condition));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Condition is satisfied when negated is not satisfied.
   */
  @Test
  public void not03() {
    when(condition.isSatisfied()).thenReturn(false);
    underTest.handle(new ConditionEvent.Unsatisfied(condition));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest));
  }

  /**
   * Event bus handler is removed when releasing.
   */
  @Test
  public void releaseRemovesItselfAsHandler() {
    underTest.release();

    verify(eventBus).unregister(underTest);
  }

}
