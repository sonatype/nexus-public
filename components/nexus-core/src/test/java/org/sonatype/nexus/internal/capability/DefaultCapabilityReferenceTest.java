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
package org.sonatype.nexus.internal.capability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.ConditionEvent;
import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.capability.condition.CryptoConditions;
import org.sonatype.nexus.capability.condition.internal.CapabilityConditionsImpl;
import org.sonatype.nexus.capability.condition.internal.ConditionsImpl;
import org.sonatype.nexus.capability.condition.internal.LogicalConditionsImpl;
import org.sonatype.nexus.capability.condition.internal.NexusConditionsImpl;
import org.sonatype.nexus.capability.condition.internal.NexusIsActiveCondition;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.capability.CapabilityIdentity.capabilityIdentity;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import static org.sonatype.nexus.internal.capability.DefaultCapabilityReference.sameProperties;

/**
 * {@link DefaultCapabilityReference} UTs.
 */
public class DefaultCapabilityReferenceTest
    extends TestSupport
{
  static final Map<String, String> NULL_PROPERTIES = null;

  private EventManager eventManager;

  private NexusIsActiveCondition activeCondition;

  @Mock
  private Capability capability;

  @Mock
  private Condition activationCondition;

  @Mock
  private Condition validityCondition;

  @Mock
  private ActivationConditionHandlerFactory achf;

  @Mock
  private ValidityConditionHandlerFactory vchf;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  private DefaultCapabilityReference underTest;

  @Before
  public void setUp() {
    eventManager = new SimpleEventManager();

    activeCondition = new NexusIsActiveCondition(eventManager);

    final Conditions conditions = new ConditionsImpl(
        new LogicalConditionsImpl(eventManager),
        new CapabilityConditionsImpl(eventManager, mock(CapabilityDescriptorRegistry.class),
            mock(CapabilityRegistry.class)),
        new NexusConditionsImpl(activeCondition),
        mock(CryptoConditions.class));

    activeCondition.start();

    when(activationCondition.isSatisfied()).thenReturn(true);
    when(capability.activationCondition()).thenReturn(activationCondition);

    when(validityCondition.isSatisfied()).thenReturn(true);
    when(capability.validityCondition()).thenReturn(validityCondition);

    when(achf.create(any(DefaultCapabilityReference.class))).thenAnswer(
        new Answer<ActivationConditionHandler>()
        {
          @Override
          public ActivationConditionHandler answer(final InvocationOnMock invocation) throws Throwable {
            return new ActivationConditionHandler(
                eventManager, conditions, (DefaultCapabilityReference) invocation.getArguments()[0]);
          }
        });

    when(vchf.create(any(DefaultCapabilityReference.class))).thenAnswer(
        new Answer<ValidityConditionHandler>()
        {
          @Override
          public ValidityConditionHandler answer(final InvocationOnMock invocation) throws Throwable {
            return new ValidityConditionHandler(
                eventManager, capabilityRegistry, conditions,
                (DefaultCapabilityReference) invocation.getArguments()[0]);
          }
        });

    underTest = new DefaultCapabilityReference(
        capabilityRegistry,
        eventManager,
        achf,
        vchf,
        capabilityIdentity("test"),
        capabilityType("TEST"),
        mock(CapabilityDescriptor.class),
        capability);

    underTest.create(Collections.<String, String>emptyMap(), Collections.emptyMap());
  }

  /**
   * Capability is enabled and enable flag is set.
   */
  @Test
  public void enableWhenNotEnabled() {
    assertThat(underTest.isEnabled(), is(false));
    underTest.enable();
    assertThat(underTest.isEnabled(), is(true));
    verify(activationCondition).bind();
  }

  /**
   * Capability is disabled and enable flag is set.
   */
  @Test
  public void disableWhenEnabled() {
    assertThat(underTest.isEnabled(), is(false));
    underTest.enable();
    assertThat(underTest.isEnabled(), is(true));
    underTest.disable();
    assertThat(underTest.isEnabled(), is(false));

    verify(activationCondition).release();
  }

  /**
   * Capability is activated and active flag is set on activate.
   */
  @Test
  public void activateWhenNotActive() throws Exception {
    underTest.enable();
    underTest.activate();
    assertThat(underTest.isActive(), is(true));
    verify(capability).onActivate();
  }

  /**
   * Capability is not activated activated again once it has been activated.
   */
  @Test
  public void activateWhenActive() throws Exception {
    underTest.enable();
    underTest.activate();
    assertThat(underTest.isActive(), is(true));
    verify(capability).onActivate();

    doThrow(new AssertionError("Activate not expected to be called")).when(capability).onActivate();
    underTest.activate();
    assertThat(underTest.isActive(), is(true));
  }

  /**
   * Capability is not passivated when is not active.
   */
  @Test
  public void passivateWhenNotActive() throws Exception {
    assertThat(underTest.isActive(), is(false));
    underTest.enable();
    underTest.passivate();
    assertThat(underTest.isActive(), is(false));

    doThrow(new AssertionError("Passivate not expected to be called")).when(capability).onPassivate();
    underTest.passivate();
  }

  /**
   * Capability is passivated when is active.
   */
  @Test
  public void passivateWhenActive() throws Exception {
    underTest.enable();
    underTest.activate();
    assertThat(underTest.isActive(), is(true));

    underTest.passivate();
    assertThat(underTest.isActive(), is(false));
    verify(capability).onPassivate();
  }

  /**
   * When activation fails, active state is false and capability remains enabled.
   * Calling passivate will do nothing.
   */
  @Test
  public void activateProblem() throws Exception {
    doThrow(new UnsupportedOperationException("Expected")).when(capability).onActivate();

    underTest.enable();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(false));

    underTest.activate();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(false));
    assertThat(underTest.hasFailure(), is(true));
    verify(capability).onActivate();

    doThrow(new AssertionError("Passivate not expected to be called")).when(capability).onPassivate();
    underTest.passivate();
  }

  /**
   * When passivation fails, active state is false and capability remains enabled.
   */
  @Test
  public void passivateProblem() throws Exception {
    doThrow(new UnsupportedOperationException("Expected")).when(capability).onPassivate();

    underTest.enable();
    underTest.activate();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(true));

    underTest.passivate();
    verify(capability).onPassivate();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(false));
    assertThat(underTest.hasFailure(), is(true));
  }

  /**
   * When update fails and capability is not active, no exception is propagated and passivate is not called.
   */
  @Test
  public void updateProblemWhenNotActive() throws Exception {
    final HashMap<String, String> properties = new HashMap<String, String>();
    properties.put("p", "p");
    final HashMap<String, String> previousProperties = new HashMap<String, String>();
    doThrow(new UnsupportedOperationException("Expected")).when(capability).onUpdate();
    doThrow(new AssertionError("Passivate not expected to be called")).when(capability).onPassivate();

    underTest.enable();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(false));

    underTest.update(properties, previousProperties, properties);
    verify(capability).onUpdate();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(false));
  }

  /**
   * When update fails and capability is active, no exception is propagated and capability is passivated.
   */
  @Test
  public void updateProblemWhenActive() throws Exception {
    final HashMap<String, String> properties = new HashMap<String, String>();
    properties.put("p", "p");
    final HashMap<String, String> previousProperties = new HashMap<String, String>();
    doThrow(new UnsupportedOperationException("Expected")).when(capability).onUpdate();

    underTest.enable();
    underTest.activate();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(true));

    underTest.update(properties, previousProperties, properties);
    verify(capability).onUpdate();
    assertThat(underTest.isEnabled(), is(true));
    assertThat(underTest.isActive(), is(false));
    verify(capability).onPassivate();
  }

  /**
   * Calling create forwards to capability (no need to call create as it is done in setup).
   */
  @Test
  public void createIsForwardedToCapability() throws Exception {
    verify(capability).onCreate();
  }

  /**
   * Calling load forwards to capability.
   */
  @Test
  public void loadIsForwardedToCapability() throws Exception {
    underTest = new DefaultCapabilityReference(
        capabilityRegistry,
        eventManager,
        achf,
        vchf,
        capabilityIdentity("test"),
        capabilityType("TEST"),
        mock(CapabilityDescriptor.class),
        capability);
    final HashMap<String, String> properties = new HashMap<String, String>();
    underTest.load(properties, properties);

    verify(capability).onLoad();
  }

  /**
   * Calling update forwards to capability if properties are different.
   */
  @Test
  public void updateIsForwardedToCapability() throws Exception {
    final HashMap<String, String> properties = new HashMap<String, String>();
    properties.put("p", "p");
    final HashMap<String, String> previousProperties = new HashMap<String, String>();
    underTest.update(properties, previousProperties, properties);
    verify(capability).onUpdate();
  }

  /**
   * Calling update does not forwards to capability if properties are same.
   */
  @SuppressWarnings("java:S2699") // sonar wants an assertion, but we're asserting by throwing an exception
  @Test
  public void updateIsNotForwardedToCapabilityIfSameProperties() throws Exception {
    final HashMap<String, String> properties = new HashMap<String, String>();
    final HashMap<String, String> previousProperties = new HashMap<String, String>();
    doThrow(new AssertionError("Update not expected to be called")).when(capability).onUpdate();
    underTest.update(properties, previousProperties, properties);
  }

  /**
   * Calling remove forwards to capability and handlers are removed.
   */
  @Test
  public void removeIsForwardedToCapability() throws Exception {
    underTest.enable();
    underTest.remove();
    verify(capability).onRemove();
  }

  @Test
  public void samePropertiesWhenBothNull() {
    assertThat(sameProperties(NULL_PROPERTIES, NULL_PROPERTIES), is(true));
  }

  @Test
  public void samePropertiesWhenOldAreNull() {
    final HashMap<String, String> p2 = new HashMap<String, String>();
    p2.put("p2", "p2");
    assertThat(sameProperties(NULL_PROPERTIES, p2), is(false));
  }

  @Test
  public void samePropertiesWhenNewAreNull() {
    final HashMap<String, String> p1 = new HashMap<String, String>();
    p1.put("p1", "p1");
    assertThat(sameProperties(p1, NULL_PROPERTIES), is(false));
  }

  @Test
  public void samePropertiesWhenBothAreSame() {
    final HashMap<String, String> p1 = new HashMap<String, String>();
    p1.put("p", "p");
    final HashMap<String, String> p2 = new HashMap<String, String>();
    p2.put("p", "p");
    assertThat(sameProperties(p1, p2), is(true));
  }

  @Test
  public void samePropertiesWhenDifferentValueSameKey() {
    final HashMap<String, String> p1 = new HashMap<String, String>();
    p1.put("p", "p1");
    final HashMap<String, String> p2 = new HashMap<String, String>();
    p2.put("p", "p2");
    assertThat(sameProperties(p1, p2), is(false));
  }

  @Test
  public void samePropertiesWhenDifferentSize() {
    final HashMap<String, String> p1 = new HashMap<String, String>();
    p1.put("p1.1", "p1.1");
    p1.put("p1.2", "p1.2");
    final HashMap<String, String> p2 = new HashMap<String, String>();
    p2.put("p2", "p2");
    assertThat(sameProperties(p1, p2), is(false));
  }

  @Test
  public void samePropertiesWhenDifferentKeys() {
    final HashMap<String, String> p1 = new HashMap<String, String>();
    p1.put("p1", "p");
    final HashMap<String, String> p2 = new HashMap<String, String>();
    p2.put("p2", "p");
    assertThat(sameProperties(p1, p2), is(false));
  }

  /**
   * When validity condition becomes unsatisfied, capability is automatically removed.
   */
  @Test
  public void automaticallyRemoveWhenValidityConditionIsUnsatisfied() throws Exception {
    eventManager.post(new ConditionEvent.Unsatisfied(validityCondition));
    verify(capabilityRegistry).remove(underTest.context().id());
  }

  /**
   * When Nexus is shutdown capability is passivated.
   */
  @Test
  public void passivateWhenNexusIsShutdown() throws Exception {
    underTest.enable();
    underTest.activate();

    activeCondition.stop();

    verify(capability).onPassivate();
  }
}
