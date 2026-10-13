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
package org.sonatype.nexus.repository.capability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonatype.nexus.repository.capability.StorageSettingsCapabilityConfiguration.LAST_DOWNLOADED_INTERVAL;
import static org.sonatype.nexus.repository.capability.StorageSettingsCapabilityConfiguration.MINIMUM_LAST_DOWNLOADED_INTERVAL_HOURS;

public class StorageSettingsCapabilityConfigurationTest
{
  private Validator validator;

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void validIntervalPassesValidation() {
    Map<String, String> properties = new HashMap<>();
    properties.put(LAST_DOWNLOADED_INTERVAL, "12");

    StorageSettingsCapabilityConfiguration config = new StorageSettingsCapabilityConfiguration(properties);
    Set<ConstraintViolation<StorageSettingsCapabilityConfiguration>> violations = validator.validate(config);

    assertThat(violations, is(empty()));
    assertThat(config.getLastDownloadedInterval(), is(12));
  }

  @Test
  public void zeroIntervalFailsValidation() {
    Map<String, String> properties = new HashMap<>();
    properties.put(LAST_DOWNLOADED_INTERVAL, "0");

    StorageSettingsCapabilityConfiguration config = new StorageSettingsCapabilityConfiguration(properties);
    Set<ConstraintViolation<StorageSettingsCapabilityConfiguration>> violations = validator.validate(config);

    assertThat(violations, hasSize(1));
    ConstraintViolation<StorageSettingsCapabilityConfiguration> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is("lastDownloadedInterval"));
  }

  @Test
  public void negativeIntervalFailsValidation() {
    Map<String, String> properties = new HashMap<>();
    properties.put(LAST_DOWNLOADED_INTERVAL, "-1");

    StorageSettingsCapabilityConfiguration config = new StorageSettingsCapabilityConfiguration(properties);
    Set<ConstraintViolation<StorageSettingsCapabilityConfiguration>> violations = validator.validate(config);

    assertThat(violations, hasSize(1));
    ConstraintViolation<StorageSettingsCapabilityConfiguration> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is("lastDownloadedInterval"));
  }

  @Test
  public void nonNumericIntervalThrowsAtConstruction() {
    Map<String, String> properties = new HashMap<>();
    properties.put(LAST_DOWNLOADED_INTERVAL, "abc");

    assertThrows(NumberFormatException.class,
        () -> new StorageSettingsCapabilityConfiguration(properties));
  }

  @Test
  public void absentIntervalUsesDefaultAndPassesValidation() {
    StorageSettingsCapabilityConfiguration config =
        new StorageSettingsCapabilityConfiguration(Collections.emptyMap());
    Set<ConstraintViolation<StorageSettingsCapabilityConfiguration>> violations = validator.validate(config);

    assertThat(violations, is(empty()));
    assertThat(config.getLastDownloadedInterval(), is(12));
  }

  @Test
  public void emptyIntervalUsesDefaultAndPassesValidation() {
    Map<String, String> properties = new HashMap<>();
    properties.put(LAST_DOWNLOADED_INTERVAL, "");

    StorageSettingsCapabilityConfiguration config = new StorageSettingsCapabilityConfiguration(properties);
    Set<ConstraintViolation<StorageSettingsCapabilityConfiguration>> violations = validator.validate(config);

    assertThat(violations, is(empty()));
    assertThat(config.getLastDownloadedInterval(), is(12));
  }

  @Test
  public void minimumAllowedIntervalPassesValidation() {
    Map<String, String> properties = new HashMap<>();
    properties.put(LAST_DOWNLOADED_INTERVAL, String.valueOf(MINIMUM_LAST_DOWNLOADED_INTERVAL_HOURS));

    StorageSettingsCapabilityConfiguration config = new StorageSettingsCapabilityConfiguration(properties);
    Set<ConstraintViolation<StorageSettingsCapabilityConfiguration>> violations = validator.validate(config);

    assertThat(violations, is(empty()));
    assertThat(config.getLastDownloadedInterval(), is(MINIMUM_LAST_DOWNLOADED_INTERVAL_HOURS));
  }
}
