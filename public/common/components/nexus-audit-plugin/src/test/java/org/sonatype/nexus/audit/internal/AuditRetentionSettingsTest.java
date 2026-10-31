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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link AuditRetentionSettings}.
 */
class AuditRetentionSettingsTest
{
  private static final int DEFAULT_MAX = 3650;

  private static final int CLOUD_MAX = 1095;

  @Test
  void defaultsTo90() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    assertThat(settings.getRetentionDays(), is(90));
  }

  @Test
  void setAndGetRetainsValue() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    settings.setRetentionDays(30);
    assertThat(settings.getRetentionDays(), is(30));
  }

  @Test
  void crossThreadUpdatesAreVisible() throws Exception {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    Thread t = new Thread(() -> settings.setRetentionDays(7));
    t.start();
    t.join();
    assertThat(settings.getRetentionDays(), is(7));
  }

  @Test
  void rejectsZero() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    assertThrows(IllegalArgumentException.class, () -> settings.setRetentionDays(0));
  }

  @Test
  void rejectsNegative() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    assertThrows(IllegalArgumentException.class, () -> settings.setRetentionDays(-1));
  }

  @Test
  void acceptsMaxValueAtDefaultCeiling() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    settings.setRetentionDays(DEFAULT_MAX);
    assertThat(settings.getRetentionDays(), is(DEFAULT_MAX));
  }

  @Test
  void rejectsAboveDefaultCeiling() {
    AuditRetentionSettings settings = new AuditRetentionSettings(DEFAULT_MAX);
    assertThrows(IllegalArgumentException.class, () -> settings.setRetentionDays(DEFAULT_MAX + 1));
  }

  @Test
  void acceptsMaxValueAtCloudCeiling() {
    AuditRetentionSettings settings = new AuditRetentionSettings(CLOUD_MAX);
    settings.setRetentionDays(CLOUD_MAX);
    assertThat(settings.getRetentionDays(), is(CLOUD_MAX));
  }

  @Test
  void rejectsAboveCloudCeiling() {
    AuditRetentionSettings settings = new AuditRetentionSettings(CLOUD_MAX);
    assertThrows(IllegalArgumentException.class, () -> settings.setRetentionDays(CLOUD_MAX + 1));
  }

  @Test
  void getMaxRetentionDaysReflectsInjectedValue() {
    assertThat(new AuditRetentionSettings(DEFAULT_MAX).getMaxRetentionDays(), is(DEFAULT_MAX));
    assertThat(new AuditRetentionSettings(CLOUD_MAX).getMaxRetentionDays(), is(CLOUD_MAX));
  }

  @Test
  void rejectsInvalidInjectedMax() {
    assertThrows(IllegalArgumentException.class, () -> new AuditRetentionSettings(0));
    assertThrows(IllegalArgumentException.class, () -> new AuditRetentionSettings(-5));
  }
}
