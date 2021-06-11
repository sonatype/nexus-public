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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.onboarding.OnboardingItem;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * Shows analytics opt-in screen for OSS if Analytics Capability is not present.
 *
 * @since 3.31
 */
@Named
@Singleton
@FeatureFlag(name = "nexus.analytics.enabled", enabledByDefault = true)
public class ConfigureAnalyticsCollectionItem
    extends ComponentSupport
    implements OnboardingItem
{
  protected static final String OSS = "OSS";

  private static final String ANALYTICS_CONFIGURATION = "analytics-configuration";

  private final ApplicationVersion applicationVersion;

  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public ConfigureAnalyticsCollectionItem(
      final ApplicationVersion applicationVersion,
      final CapabilityRegistry capabilityRegistry)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  @Override
  public String getType() {
    return "ConfigureAnalyticsCollection";
  }

  @Override
  public int getPriority() {
    return 64;
  }

  @Override
  public boolean applies() {
    return OSS.equals(applicationVersion.getEdition()) && analyticsCapabilityAbsent();
  }

  private boolean analyticsCapabilityAbsent() {
    CapabilityType capabilityType = capabilityType(ANALYTICS_CONFIGURATION);
    return capabilityRegistry.get(new CapabilityReferenceFilter().withType(capabilityType)).isEmpty();
  }
}
