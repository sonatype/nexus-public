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

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.capability.CapabilityIdentity.capabilityIdentity;

/**
 * {@link PassivateCapabilityDuringUpdateCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class PassivateCapabilityDuringUpdateConditionTest
    extends EventBusTestSupport
{

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private CapabilityReference reference;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  private PassivateCapabilityDuringUpdateCondition underTest;

  @Before
  public final void setUpPassivateCapabilityDuringUpdateCondition()
      throws Exception
  {
    final CapabilityIdentity id = capabilityIdentity("test");

    final CapabilityContext context = mock(CapabilityContext.class);
    when(context.id()).thenReturn(id);

    when(reference.context()).thenReturn(context);

    underTest = new PassivateCapabilityDuringUpdateCondition(eventBus);
    underTest.setContext(context);
    underTest.bind();

    verify(eventBus).register(underTest);
  }

  /**
   * Condition should become unsatisfied before update and satisfied after update.
   */
  @Test
  public void passivateDuringUpdate() {
    underTest.handle(new CapabilityEvent.BeforeUpdate(
        capabilityRegistry, reference, Maps.<String, String>newHashMap(), Maps.<String, String>newHashMap()
    ));
    underTest.handle(new CapabilityEvent.AfterUpdate(
        capabilityRegistry, reference, Maps.<String, String>newHashMap(), Maps.<String, String>newHashMap()
    ));

    verifyEventBusEvents(unsatisfied(underTest), satisfied(underTest));
  }

  /**
   * Event bus handler is removed when releasing.
   */
  @Test
  public void releaseRemovesItselfAsHandler() {
    underTest.release();

    verify(eventBus).unregister(underTest);
  }

  /**
   * Verify that binding fails if id was not set before.
   */
  @Test
  public void bindWithoutIdBeingSet() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Capability identity not specified");
    new PassivateCapabilityDuringUpdateCondition(eventBus).bind();
  }

  /**
   * Verify that binding succeeds after contextualization.
   */
  @Test
  public void bindAfterContextualization() {
    new PassivateCapabilityDuringUpdateCondition(eventBus).setContext(reference.context()).bind();
  }

  /**
   * Verify that contextualization fails if already bounded.
   */
  @Test
  public void contextualizationWhenAlreadyBounded() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Cannot contextualize when already bounded");
    underTest.setContext(reference.context());
  }

  /**
   * Verify that contextualization fails if already contextualized.
   */
  @Test
  public void contextualizationWhenAlreadyContextualized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Already contextualized");
    new PassivateCapabilityDuringUpdateCondition(eventBus)
        .setContext(reference.context())
        .setContext(reference.context());
  }

}
