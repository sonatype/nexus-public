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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.onboarding.capability.OnboardingCapability;
import org.sonatype.nexus.onboarding.capability.OnboardingCapabilityHelper;
import org.sonatype.nexus.security.anonymous.AnonymousManager;

import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class InstanceStatus
{
  private final AnonymousManager anonymousManager;

  private final OnboardingCapabilityHelper onboardingCapabilityHelper;

  @Inject
  public InstanceStatus(
      final AnonymousManager anonymousManager,
      final OnboardingCapabilityHelper onboardingCapabilityHelper)
  {
    this.anonymousManager = requireNonNull(anonymousManager);
    this.onboardingCapabilityHelper = requireNonNull(onboardingCapabilityHelper);
  }

  public boolean isNew() {
    if (!anonymousManager.isConfigured()) {
      return true;
    }
    OnboardingCapability onboardingCapability = onboardingCapabilityHelper.getOnboardingCapability();
    return onboardingCapability.isRegistrationStarted() && !onboardingCapability.isRegistrationCompleted();
  }

  public boolean isUpgraded() {
    OnboardingCapability onboardingCapability = onboardingCapabilityHelper.getOnboardingCapability();
    return anonymousManager.isConfigured() && !onboardingCapability.isRegistrationStarted();
  }
}
