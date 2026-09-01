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
package org.sonatype.nexus.audit.internal;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.nexus.audit.AuditRecorder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests that {@link AuditCapability} propagates the retention configuration to
 * {@link AuditRetentionSettings} through its lifecycle callbacks.
 */
@ExtendWith(MockitoExtension.class)
class AuditCapabilityWiringTest
{
  private static final int DEFAULT_MAX = 3650;

  @Mock
  private AuditRecorder auditRecorder;

  @Test
  void onActivateAppliesRetentionDays() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    TestableAuditCapability capability = new TestableAuditCapability(auditRecorder, settings);
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(
        Map.of(AuditCapability.Configuration.RETENTION_DAYS, "30"), DEFAULT_MAX);

    capability.invokeOnActivate(cfg);

    assertThat(settings.getRetentionDays(), is(30));
  }

  @Test
  void onUpdateAppliesRetentionDays() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    TestableAuditCapability capability = new TestableAuditCapability(auditRecorder, settings);
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(
        Map.of(AuditCapability.Configuration.RETENTION_DAYS, "7"), DEFAULT_MAX);

    capability.invokeOnUpdate(cfg);

    assertThat(settings.getRetentionDays(), is(7));
  }

  @Test
  void legacyConfigWithoutRetentionDaysFallsBackToDefault() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    settings.setRetentionDays(30);
    TestableAuditCapability capability = new TestableAuditCapability(auditRecorder, settings);
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(Map.of(), DEFAULT_MAX);

    capability.invokeOnActivate(cfg);

    assertThat(settings.getRetentionDays(), is(AuditRetentionSettings.DEFAULT_RETENTION_DAYS));
  }

  /**
   * Test seam that exposes the protected {@code onActivate}/{@code onUpdate} callbacks
   * without invoking {@code CapabilitySupport}'s state machinery.
   */
  private static final class TestableAuditCapability
      extends AuditCapability
  {
    TestableAuditCapability(final AuditRecorder recorder, final AuditRetentionSettings settings) {
      super(recorder, settings);
    }

    void invokeOnActivate(final Configuration cfg) {
      onActivate(cfg);
    }

    void invokeOnUpdate(final Configuration cfg) {
      onUpdate(cfg);
    }
  }
}
