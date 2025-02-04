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
package org.sonatype.nexus.onboarding.capability;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

public class OnboardingCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  public static final String PRO_STARTER_INFO_PAGE_COMPLETED = "proStarterInfoPageCompleted";

  public static final boolean DEFAULT_PRO_STARTER_INFO_PAGE_COMPLETED = false;

  public static final boolean DEFAULT_REGISTRATION_STARTED = false;

  public static final String REGISTRATION_STARTED = "registrationStarted";

  public static final boolean DEFAULT_REGISTRATION_COMPLETED = false;

  public static final String REGISTRATION_COMPLETED = "registrationCompleted";

  private boolean proStarterInfoPageCompleted;

  private boolean registrationStarted;

  private boolean registrationCompleted;

  public OnboardingCapabilityConfiguration(final Map<String, String> properties) {
    checkNotNull(properties);
    this.proStarterInfoPageCompleted =
        parseBoolean(properties.get(PRO_STARTER_INFO_PAGE_COMPLETED), DEFAULT_PRO_STARTER_INFO_PAGE_COMPLETED);
    this.registrationStarted = parseBoolean(properties.get(REGISTRATION_STARTED), DEFAULT_REGISTRATION_STARTED);
    this.registrationCompleted = parseBoolean(properties.get(REGISTRATION_COMPLETED), DEFAULT_REGISTRATION_COMPLETED);
  }

  public boolean isRegistrationStarted() {
    return registrationStarted;
  }

  public OnboardingCapabilityConfiguration setRegistrationStarted(final boolean registrationStarted) {
    this.registrationStarted = registrationStarted;
    return this;
  }

  public boolean isRegistrationCompleted() {
    return registrationCompleted;
  }

  public OnboardingCapabilityConfiguration setRegistrationCompleted(final boolean registrationCompleted) {
    this.registrationCompleted = registrationCompleted;
    return this;
  }

  public Map<String, String> asMap() {
    final Map<String, String> properties = Maps.newHashMap();
    properties.put(PRO_STARTER_INFO_PAGE_COMPLETED, String.valueOf(proStarterInfoPageCompleted));
    properties.put(REGISTRATION_STARTED, String.valueOf(registrationStarted));
    properties.put(REGISTRATION_COMPLETED, String.valueOf(registrationCompleted));
    return properties;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "proStarterInfoPageCompleted=" + proStarterInfoPageCompleted + "; " +
        "registrationStarted=" + registrationStarted + "; " + "registrationCompleted=" + registrationCompleted + "; " +
        "}";
  }
}
