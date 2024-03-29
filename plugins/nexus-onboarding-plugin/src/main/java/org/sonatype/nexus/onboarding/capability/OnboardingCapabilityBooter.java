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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityBooterSupport;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.common.app.ManagedLifecycle;

import com.google.common.collect.ImmutableMap;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityConfiguration.DEFAULT_PRO_STARTER_INFO_PAGE_COMPLETED;
import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityConfiguration.DEFAULT_REGISTRATION_COMPLETED;
import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityConfiguration.DEFAULT_REGISTRATION_STARTED;
import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityConfiguration.PRO_STARTER_INFO_PAGE_COMPLETED;
import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityConfiguration.REGISTRATION_COMPLETED;
import static org.sonatype.nexus.onboarding.capability.OnboardingCapabilityConfiguration.REGISTRATION_STARTED;

@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class OnboardingCapabilityBooter
    extends CapabilityBooterSupport
{
  @Override
  protected void boot(final CapabilityRegistry registry) throws Exception {
    maybeAddCapability(
        registry,
        OnboardingCapability.TYPE,
        true,
        null,
        ImmutableMap.of(
            PRO_STARTER_INFO_PAGE_COMPLETED, String.valueOf(DEFAULT_PRO_STARTER_INFO_PAGE_COMPLETED),
            REGISTRATION_STARTED, String.valueOf(DEFAULT_REGISTRATION_STARTED),
            REGISTRATION_COMPLETED, String.valueOf(DEFAULT_REGISTRATION_COMPLETED)
        )
    );
  }
}
