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
package org.sonatype.nexus.capability.condition.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.condition.internal.CapabilityConditionsImpl;
import org.sonatype.nexus.capability.condition.internal.CapabilityOfTypeActiveCondition;
import org.sonatype.nexus.capability.condition.internal.CapabilityOfTypeExistsCondition;
import org.sonatype.nexus.capability.condition.internal.PassivateCapabilityDuringUpdateCondition;
import org.sonatype.nexus.common.event.EventManager;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * {@link CapabilityConditionsImpl} UTs.
 *
 * @since capabilities 2.0
 */
public class CapabilityConditionsImplTest
    extends TestSupport
{

  private CapabilityConditionsImpl underTest;

  @Before
  public final void setUpCapabilityConditions() {
    final EventManager eventManager = mock(EventManager.class);
    final CapabilityDescriptorRegistry descriptorRegistry = mock(CapabilityDescriptorRegistry.class);
    final CapabilityRegistry capabilityRegistry = mock(CapabilityRegistry.class);
    underTest = new CapabilityConditionsImpl(eventManager, descriptorRegistry, capabilityRegistry);
  }

  /**
   * capabilityOfTypeExists() factory method returns expected condition.
   */
  @Test
  public void capabilityOfTypeExists() {
    assertThat(
        underTest.capabilityOfTypeExists(capabilityType("test")),
        is(Matchers.<Condition>instanceOf(CapabilityOfTypeExistsCondition.class))
    );
  }

  /**
   * capabilityOfTypeActive() factory method returns expected condition.
   */
  @Test
  public void capabilityOfTypeActive() {
    assertThat(
        underTest.capabilityOfTypeActive(capabilityType("test")),
        is(Matchers.<Condition>instanceOf(CapabilityOfTypeActiveCondition.class))
    );
  }

  /**
   * reactivateCapabilityOnUpdate() factory method returns expected condition.
   */
  @Test
  public void reactivateCapabilityOnUpdate() {
    assertThat(
        underTest.passivateCapabilityDuringUpdate(),
        is(Matchers.<Condition>instanceOf(PassivateCapabilityDuringUpdateCondition.class))
    );
  }

}
