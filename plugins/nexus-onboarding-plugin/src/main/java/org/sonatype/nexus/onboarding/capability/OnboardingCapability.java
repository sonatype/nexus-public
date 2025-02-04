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
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;

import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityDescriptor.messages;

@Named(OnboardingCapability.TYPE_ID)
public class OnboardingCapability
    extends CapabilitySupport<OnboardingCapabilityConfiguration>

{
  public static final String TYPE_ID = "onboarding-wizard";

  public static final CapabilityType TYPE = CapabilityType.capabilityType(TYPE_ID);

  @Override
  protected OnboardingCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new OnboardingCapabilityConfiguration(properties);
  }

  @Nullable
  @Override
  protected String renderDescription() {
    return context().isActive() ? messages.enabled() : messages.disabled();
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical()
        .and(conditions().nexus().active(), conditions().capabilities().capabilityHasNoDuplicates(),
            conditions().capabilities().passivateCapabilityDuringUpdate());
  }

  public boolean isRegistrationStarted() {
    return getConfig().isRegistrationStarted();
  }

  public void setRegistrationStarted(final boolean registrationStarted) {
    getConfig().setRegistrationStarted(registrationStarted);
  }

  public boolean isRegistrationCompleted() {
    return getConfig().isRegistrationCompleted();
  }

  public void setRegistrationCompleted(final boolean registrationCompleted) {
    getConfig().setRegistrationCompleted(registrationCompleted);
  }
}
