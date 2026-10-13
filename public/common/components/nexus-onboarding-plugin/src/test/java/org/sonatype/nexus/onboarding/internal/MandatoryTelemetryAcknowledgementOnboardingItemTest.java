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
package org.sonatype.nexus.onboarding.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistribution;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.TelemetryEnforcement;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.onboarding.OnboardingItemPriority;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MandatoryTelemetryAcknowledgementOnboardingItemTest
{
  private static final String KEY = "nexus.telemetry.mandatory.acknowledgement";

  @Mock
  private ApplicationVersion mockApplicationVersion;

  @Mock
  private GlobalKeyValueStore mockGlobalKeyValueStore;

  @Mock
  private TelemetryEnforcement mockEnforcementChecker;

  private MandatoryTelemetryAcknowledgementOnboardingItem underTest;

  @BeforeEach
  void setUp() {
    // Detection reads the classpath, which in a unit test contains no distribution
    // marker at all. Force self-hosted so the edition and key-value branches are
    // what each test actually exercises.
    NexusDistribution.setTestOverride(NexusDistributionType.SELF_HOSTED);
    when(mockApplicationVersion.isAnyProEdition()).thenReturn(true);
    // A null enforcement checker is the Community and Core baseline, and the shape every
    // expectation below predates the opt-out gate was written against. Built explicitly rather
    // than with @InjectMocks: a mock here would answer false by default and silently turn every
    // assertTrue in this class into a test of the opt-out branch instead.
    underTest = itemWith(null);
  }

  @AfterEach
  void tearDown() {
    // Static JVM state — leaks into every later test in the module if not cleared.
    NexusDistribution.clearTestOverride();
  }

  @Test
  void testTypeIsTheStepRegistryContractString() {
    assertEquals("MandatoryTelemetryAcknowledgement", underTest.getType());
  }

  @Test
  void testPriorityFollowsAnonymousAccess() {
    assertEquals(OnboardingItemPriority.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT_ONBOARDING.intValue(),
        underTest.getPriority());
  }

  @Test
  void testAppliesWhenProAndNotAcknowledged() {
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.empty());
    assertTrue(underTest.applies());
  }

  @Test
  void testAppliesWhenCommunityAndNotAcknowledged() {
    when(mockApplicationVersion.isAnyProEdition()).thenReturn(false);
    when(mockApplicationVersion.isCommunityEdition()).thenReturn(true);
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.empty());
    assertTrue(underTest.applies());
  }

  @Test
  void testDoesNotApplyWhenAlreadyAcknowledged() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setValue(Map.of("acknowledgement", true));
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.of(kv));
    assertFalse(underTest.applies());
  }

  @Test
  void testAppliesWhenAcknowledgementFieldIsMissing() {
    // An empty or foreign-shaped row must not suppress the card.
    NexusKeyValue kv = new NexusKeyValue();
    kv.setValue(Map.of("somethingElse", true));
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.of(kv));
    assertTrue(underTest.applies());
  }

  @Test
  void testAppliesWhenAcknowledgementFieldIsNull() {
    // CommunityOnboardingItem:52 unboxes this case straight into an NPE. We must not.
    Map<String, Object> value = new HashMap<>();
    value.put("acknowledgement", null);
    NexusKeyValue kv = new NexusKeyValue();
    kv.setValue(value);
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.of(kv));
    assertTrue(underTest.applies());
  }

  @Test
  void testAppliesWhenAcknowledgementFieldIsNotABoolean() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setValue(Map.of("acknowledgement", "true"));
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.of(kv));
    assertTrue(underTest.applies());
  }

  @Test
  void testDoesNotApplyOnCloud() {
    NexusDistribution.setTestOverride(NexusDistributionType.CLOUD);
    assertFalse(underTest.applies());
  }

  @Test
  void testDoesNotApplyWhenDistributionIsUndetectable() {
    // Core/OSS ships no distribution marker, so detect() returns null there.
    NexusDistribution.clearTestOverride();
    assertFalse(underTest.applies());
  }

  @Test
  void testDoesNotApplyOnUnlicensedEdition() {
    when(mockApplicationVersion.isAnyProEdition()).thenReturn(false);
    when(mockApplicationVersion.isCommunityEdition()).thenReturn(false);
    assertFalse(underTest.applies());
  }

  @Test
  void testAppliesWhenNoEnforcementCheckerIsPresent() {
    // Community and Core never get the Pro-only bean. Absence must read as "telemetry required",
    // or the card disappears on the largest population of instances with no Pro test failing.
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.empty());
    assertTrue(itemWith(null).applies());
  }

  @Test
  void testAppliesWhenTelemetryIsRequired() {
    when(mockEnforcementChecker.isTelemetryRequired()).thenReturn(true);
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.empty());
    assertTrue(itemWith(mockEnforcementChecker).applies());
  }

  @Test
  void testDoesNotApplyWhenLicenseCarriesTheOptOut() {
    when(mockEnforcementChecker.isTelemetryRequired()).thenReturn(false);
    when(mockGlobalKeyValueStore.getKey(KEY)).thenReturn(Optional.empty());
    assertFalse(itemWith(mockEnforcementChecker).applies());
  }

  @Test
  void testOptOutIsDecidedWithoutReadingTheAcknowledgement() {
    // An opt-out license is never shown the card, so it never records an acknowledgement. The gate
    // must therefore stand on its own rather than depend on the absent record.
    when(mockEnforcementChecker.isTelemetryRequired()).thenReturn(false);

    assertFalse(itemWith(mockEnforcementChecker).applies());

    verifyNoInteractions(mockGlobalKeyValueStore);
  }

  private MandatoryTelemetryAcknowledgementOnboardingItem itemWith(
      final TelemetryEnforcement enforcementChecker)
  {
    return new MandatoryTelemetryAcknowledgementOnboardingItem(
        mockApplicationVersion, mockGlobalKeyValueStore, enforcementChecker);
  }
}
