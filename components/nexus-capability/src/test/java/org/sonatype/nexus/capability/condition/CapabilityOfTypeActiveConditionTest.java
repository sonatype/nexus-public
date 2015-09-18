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

import java.util.Arrays;
import java.util.Collections;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * {@link CapabilityOfTypeActiveCondition} UTs.
 *
 * @since capabilities 2.0
 */
public class CapabilityOfTypeActiveConditionTest
    extends EventBusTestSupport
{

  @Mock
  private CapabilityReference ref1;

  @Mock
  private CapabilityReference ref2;

  @Mock
  private CapabilityReference ref3;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  private CapabilityOfTypeActiveCondition underTest;

  @Before
  public final void setUpCapabilityOfTypeActiveCondition()
      throws Exception
  {
    final CapabilityType capabilityType = capabilityType(this.getClass().getName());

    when(ref1.context()).thenReturn(mock(CapabilityContext.class));
    when(ref1.context().type()).thenReturn(capabilityType);

    when(ref2.context()).thenReturn(mock(CapabilityContext.class));
    when(ref2.context().type()).thenReturn(capabilityType);

    when(ref3.context()).thenReturn(mock(CapabilityContext.class));
    when(ref3.context().type()).thenReturn(capabilityType(Capability.class.getName()));

    final CapabilityDescriptorRegistry descriptorRegistry = mock(CapabilityDescriptorRegistry.class);
    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);

    when(descriptor.name()).thenReturn(this.getClass().getSimpleName());
    when(descriptorRegistry.get(capabilityType)).thenReturn(descriptor);

    underTest = new CapabilityOfTypeActiveCondition(
        eventBus, descriptorRegistry, capabilityRegistry, capabilityType
    );
    underTest.bind();

    verify(eventBus).register(underTest);
  }

  /**
   * Initially, condition should be unsatisfied.
   */
  @Test
  public void initiallyNotSatisfied() {
    assertThat(underTest.isSatisfied(), is(false));
  }

  /**
   * Condition should not be satisfied if capability of specified type exists but is not active.
   */
  @Test
  public void capabilityOfTypeActive01() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(false);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(false));
  }

  /**
   * Condition should be satisfied if capability of specified type exists and is active.
   */
  @Test
  public void capabilityOfTypeActive02() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));

    verifyEventBusEvents(satisfied(underTest));
  }

  /**
   * Condition should not be re-satisfied if a new active capability of specified type is added.
   */
  @Test
  public void capabilityOfTypeActive03() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref1, ref2)).when(capabilityRegistry).getAll();
    when(ref2.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref2));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest));
  }

  /**
   * Condition should remain satisfied if another active capability of the specified type is removed.
   */
  @Test
  public void capabilityOfTypeActive04() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref1, ref2)).when(capabilityRegistry).getAll();
    when(ref2.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref2));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref2)).when(capabilityRegistry).getAll();
    underTest.handle(new CapabilityEvent.AfterRemove(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest));
  }

  /**
   * Condition should remain satisfied if another active capability of the specified type is passivated.
   */
  @Test
  public void capabilityOfTypeActive05() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref1, ref2)).when(capabilityRegistry).getAll();
    when(ref2.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref2));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref1, ref2)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.BeforePassivated(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    verifyEventBusEvents(satisfied(underTest));
  }

  /**
   * Condition should become unsatisfied when all capabilities have been removed.
   */
  @Test
  public void capabilityOfTypeActive06() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Collections.emptyList()).when(capabilityRegistry).getAll();
    underTest.handle(new CapabilityEvent.AfterRemove(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(false));

    verifyEventBusEvents(satisfied(underTest), unsatisfied(underTest));
  }

  /**
   * Condition should remain satisfied if a condition of a different type is passivated.
   */
  @Test
  public void capabilityOfTypeActive07() {
    doReturn(Arrays.asList(ref1)).when(capabilityRegistry).getAll();
    when(ref1.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref1));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref1, ref3)).when(capabilityRegistry).getAll();
    when(ref3.context().isActive()).thenReturn(true);
    underTest.handle(new CapabilityEvent.Created(capabilityRegistry, ref3));
    assertThat(underTest.isSatisfied(), is(true));

    doReturn(Arrays.asList(ref1, ref3)).when(capabilityRegistry).getAll();
    when(ref3.context().isActive()).thenReturn(false);
    underTest.handle(new CapabilityEvent.BeforePassivated(capabilityRegistry, ref3));
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
