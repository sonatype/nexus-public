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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link AuditCapability.Configuration}.
 */
class AuditCapabilityConfigurationTest
{
  private static final int DEFAULT_MAX = 3650;

  private static final int CLOUD_MAX = 1095;

  @Test
  void defaultsTo90WhenMissing() {
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(new HashMap<>(), DEFAULT_MAX);
    assertThat(cfg.getRetentionDays(), is(90));
  }

  @Test
  void defaultsTo90WhenNull() {
    Map<String, String> props = new HashMap<>();
    props.put(AuditCapability.Configuration.RETENTION_DAYS, null);
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(props, DEFAULT_MAX);
    assertThat(cfg.getRetentionDays(), is(90));
  }

  @Test
  void defaultsTo90WhenEmpty() {
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(
        Map.of(AuditCapability.Configuration.RETENTION_DAYS, ""), DEFAULT_MAX);
    assertThat(cfg.getRetentionDays(), is(90));
  }

  @Test
  void parsesConfiguredValue() {
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(
        Map.of(AuditCapability.Configuration.RETENTION_DAYS, "30"), DEFAULT_MAX);
    assertThat(cfg.getRetentionDays(), is(30));
  }

  @Test
  void rejectsNonNumeric() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new AuditCapability.Configuration(
            Map.of(AuditCapability.Configuration.RETENTION_DAYS, "abc"), DEFAULT_MAX));
    assertThat(e.getMessage(), containsString("retentionDays"));
    assertThat(e.getMessage(), containsString("'abc'"));
    assertThat(e.getCause(), instanceOf(NumberFormatException.class));
  }

  @Test
  void rejectsZero() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new AuditCapability.Configuration(
            Map.of(AuditCapability.Configuration.RETENTION_DAYS, "0"), DEFAULT_MAX));
    assertThat(e.getMessage(), containsString("retentionDays"));
    assertThat(e.getMessage(), containsString("0"));
  }

  @Test
  void rejectsNegative() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new AuditCapability.Configuration(
            Map.of(AuditCapability.Configuration.RETENTION_DAYS, "-5"), DEFAULT_MAX));
    assertThat(e.getMessage(), containsString("retentionDays"));
  }

  @Test
  void acceptsDefaultMax() {
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(
        Map.of(AuditCapability.Configuration.RETENTION_DAYS, String.valueOf(DEFAULT_MAX)), DEFAULT_MAX);
    assertThat(cfg.getRetentionDays(), is(DEFAULT_MAX));
  }

  @Test
  void rejectsAboveDefaultMax() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new AuditCapability.Configuration(
            Map.of(AuditCapability.Configuration.RETENTION_DAYS, String.valueOf(DEFAULT_MAX + 1)),
            DEFAULT_MAX));
    assertThat(e.getMessage(), containsString(String.valueOf(DEFAULT_MAX)));
  }

  @Test
  void acceptsCloudMax() {
    AuditCapability.Configuration cfg = new AuditCapability.Configuration(
        Map.of(AuditCapability.Configuration.RETENTION_DAYS, String.valueOf(CLOUD_MAX)), CLOUD_MAX);
    assertThat(cfg.getRetentionDays(), is(CLOUD_MAX));
  }

  @Test
  void rejectsAboveCloudMax() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new AuditCapability.Configuration(
            Map.of(AuditCapability.Configuration.RETENTION_DAYS, String.valueOf(CLOUD_MAX + 1)),
            CLOUD_MAX));
    assertThat(e.getMessage(), containsString(String.valueOf(CLOUD_MAX)));
  }
}
