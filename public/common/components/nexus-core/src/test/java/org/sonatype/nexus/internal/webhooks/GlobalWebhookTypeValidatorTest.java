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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.webhooks.GlobalWebhook;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GlobalWebhookTypeValidator}.
 * <p>
 * The validator's sole responsibility is to confirm that every name in the configured list of
 * global webhook events corresponds to a discovered {@link GlobalWebhook} bean. It must NOT
 * gate any event on capability state — firewall quarantine events, for example, are gated at
 * fire time by {@code FirewallContributedHandler} against per-repository firewall configuration
 * (NEXUS-53667).
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GlobalWebhookTypeValidatorTest
{
  private static final String FIREWALL_QUARANTINE_WEBHOOK = "firewall_quarantine";

  private static final String KNOWN_WEBHOOK_A = "webhook_a";

  private static final String KNOWN_WEBHOOK_B = "webhook_b";

  private static final String UNKNOWN_WEBHOOK = "not_a_real_webhook";

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private GlobalWebhook firewallQuarantineWebhook;

  @Mock
  private GlobalWebhook webhookA;

  @Mock
  private GlobalWebhook webhookB;

  private GlobalWebhookTypeValidator underTest;

  @Before
  public void setup() {
    when(firewallQuarantineWebhook.getName()).thenReturn(FIREWALL_QUARANTINE_WEBHOOK);
    when(webhookA.getName()).thenReturn(KNOWN_WEBHOOK_A);
    when(webhookB.getName()).thenReturn(KNOWN_WEBHOOK_B);
    underTest = new GlobalWebhookTypeValidator(Arrays.asList(firewallQuarantineWebhook, webhookA, webhookB));
  }

  @Test
  public void nullNamesListIsValid() {
    assertThat(underTest.isValid(null, context), is(true));
  }

  @Test
  public void emptyNamesListIsValid() {
    assertThat(underTest.isValid(Collections.emptyList(), context), is(true));
  }

  @Test
  public void singleKnownNameIsValid() {
    assertThat(underTest.isValid(Collections.singletonList(KNOWN_WEBHOOK_A), context), is(true));
  }

  @Test
  public void allKnownNamesAreValid() {
    List<String> names = Arrays.asList(KNOWN_WEBHOOK_A, KNOWN_WEBHOOK_B, FIREWALL_QUARANTINE_WEBHOOK);
    assertThat(underTest.isValid(names, context), is(true));
  }

  @Test
  public void unknownNameIsInvalid() {
    assertThat(underTest.isValid(Collections.singletonList(UNKNOWN_WEBHOOK), context), is(false));
  }

  @Test
  public void mixOfKnownAndUnknownIsInvalid() {
    List<String> names = Arrays.asList(KNOWN_WEBHOOK_A, UNKNOWN_WEBHOOK);
    assertThat(underTest.isValid(names, context), is(false));
  }

  /**
   * Regression test for NEXUS-53667: the {@code firewall_quarantine} subscription must not be gated
   * on any capability state. Firewall configuration lives on repositories now (see
   * {@code FirewallCapabilityToRepositoryConfigMigrationStep_2_143}); the event pipeline gates
   * itself at fire time in {@code FirewallContributedHandler} using
   * {@code FirewallConfigurationHelper.isFirewallEnabled(repository)} and
   * {@code clmConnector.isEnabled()}. Configuring a subscription is therefore always safe.
   */
  @Test
  public void firewallQuarantineWebhookIsValidRegardlessOfCapabilityState() {
    // No CapabilityRegistry is consulted at all — the validator has no such dependency.
    // A subscription to firewall_quarantine must be accepted purely because the webhook bean
    // exists in the discovered list.
    assertThat(underTest.isValid(Collections.singletonList(FIREWALL_QUARANTINE_WEBHOOK), context), is(true));
  }

  /**
   * Pins that no historical special-casing of {@code firewall_quarantine} can short-circuit the
   * unknown-name rejection. A list mixing a known firewall event with an unknown name must fail.
   */
  @Test
  public void mixWithFirewallQuarantineAndUnknownIsInvalid() {
    List<String> names = Arrays.asList(FIREWALL_QUARANTINE_WEBHOOK, UNKNOWN_WEBHOOK);
    assertThat(underTest.isValid(names, context), is(false));
  }

  @Test
  public void firewallQuarantineWebhookIsRejectedIfBeanNotDiscovered() {
    // Given a validator with no firewall_quarantine bean in the discovered list...
    GlobalWebhookTypeValidator validatorWithoutFirewall =
        new GlobalWebhookTypeValidator(Arrays.asList(webhookA, webhookB));

    // ...the name is unknown and must fail (defensive: nothing else in the system should be
    // sending events with that name if the bean isn't installed).
    assertThat(
        validatorWithoutFirewall.isValid(Collections.singletonList(FIREWALL_QUARANTINE_WEBHOOK), context),
        is(false));
  }
}
