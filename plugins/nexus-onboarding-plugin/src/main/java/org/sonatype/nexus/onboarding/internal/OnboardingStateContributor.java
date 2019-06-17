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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.onboarding.OnboardingConfiguration;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.17
 */
@Singleton
@Named
public class OnboardingStateContributor
    implements StateContributor
{
  private final OnboardingConfiguration onboardingConfiguration;

  private final OnboardingManager onboardingManager;

  private final AdminPasswordFileManager adminPasswordFileManager;

  private boolean needsOnboarding = true;

  @Inject
  public OnboardingStateContributor(final OnboardingConfiguration onboardingConfiguration,
                                    final OnboardingManager onboardingManager,
                                    final AdminPasswordFileManager adminPasswordFileManager)
  {
    this.onboardingConfiguration = checkNotNull(onboardingConfiguration);
    this.onboardingManager = checkNotNull(onboardingManager);
    this.adminPasswordFileManager = checkNotNull(adminPasswordFileManager);
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {
    //cache the onboarding flag, once it's false there is no longer a need to check anymore
    needsOnboarding = needsOnboarding && onboardingConfiguration.isEnabled() && onboardingManager.needsOnboarding();

    HashMap<String, Object> properties = new HashMap<>();

    if (needsOnboarding) {
      properties.put("onboarding.required", true);
    }

    if (adminPasswordFileManager.exists()) {
      properties.put("admin.password.file", adminPasswordFileManager.getPath());
    }

    return properties.isEmpty() ? null : properties;
  }
}
