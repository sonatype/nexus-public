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
package org.sonatype.nexus.internal.webhooks;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.condition.CapabilityConditions;
import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.crypto.secrets.SecretsService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GlobalWebhookCapability#activationCondition()}.
 * <p>
 * Regression coverage for NEXUS-53667: the activation condition must be the plain
 * {@code passivateCapabilityDuringUpdate()} regardless of the subscribed event names. Previously
 * the capability added a {@code capabilityOfTypeActive("firewall.audit")} clause whenever
 * {@code firewall_quarantine} was in the subscription list, which made the capability
 * permanently inactive on any instance where STL-381 had migrated firewall configuration to
 * per-repository config (deleting the {@code firewall.audit} capability rows).
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GlobalWebhookCapabilityTest
{
  @Mock
  private Conditions conditions;

  @Mock
  private CapabilityConditions capabilityConditions;

  @Mock
  private CapabilityContext context;

  @Mock
  private Condition passivateCondition;

  @Mock
  private SecretsService secretsService;

  private GlobalWebhookCapability underTest;

  @Before
  public void setup() {
    when(conditions.capabilities()).thenReturn(capabilityConditions);
    when(capabilityConditions.passivateCapabilityDuringUpdate()).thenReturn(passivateCondition);

    underTest = new GlobalWebhookCapability();
    underTest.installConditionComponents(conditions);
  }

  @Test
  public void activationConditionIsPassivateDuringUpdateWhenNoConfigLoaded() {
    // No onCreate/onLoad has been called — getConfig() would throw. The condition must not
    // depend on config state at all.
    Condition result = underTest.activationCondition();

    assertThat(result, sameInstance(passivateCondition));
  }

  @Test
  public void activationConditionIsPassivateDuringUpdateWithoutFirewallQuarantine() throws Exception {
    loadConfig("some_event,another_event");

    Condition result = underTest.activationCondition();

    assertThat(result, sameInstance(passivateCondition));
  }

  /**
   * Regression test for NEXUS-53667: prior to the fix, this call returned an AND condition combining
   * the passivate condition with {@code capabilityOfTypeActive("firewall.audit")} — a clause that
   * is permanently false post-STL-381 and prevented the webhook capability from ever activating.
   * The condition must now be the passivate condition and nothing else.
   */
  @Test
  public void activationConditionIgnoresFirewallQuarantineSubscription() throws Exception {
    loadConfig("firewall_quarantine");

    Condition result = underTest.activationCondition();

    assertThat(result, sameInstance(passivateCondition));

    // No firewall.audit lookup, no logical AND.
    verify(capabilityConditions, never()).capabilityOfTypeActive(any(CapabilityType.class));
    verify(conditions, never()).logical();
  }

  @Test
  public void activationConditionIgnoresFirewallQuarantineEvenWhenMixedWithOthers() throws Exception {
    loadConfig("some_event,firewall_quarantine,another_event");

    Condition result = underTest.activationCondition();

    assertThat(result, sameInstance(passivateCondition));
    verify(capabilityConditions, never()).capabilityOfTypeActive(any(CapabilityType.class));
  }

  private void loadConfig(final String namesCsv) throws Exception {
    Map<String, String> props = new HashMap<>();
    props.put("names", namesCsv);
    props.put("url", "http://example.com/webhook");
    when(context.properties()).thenReturn(props);
    underTest.init(context, secretsService);
    underTest.onCreate();
  }
}
