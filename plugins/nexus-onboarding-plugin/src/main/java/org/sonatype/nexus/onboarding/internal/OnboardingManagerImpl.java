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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.onboarding.OnboardingConfiguration;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.onboarding.OnboardingManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.17
 */
@Named
@Singleton
public class OnboardingManagerImpl
    extends ComponentSupport
    implements OnboardingManager
{
  private final OnboardingConfiguration onboardingConfiguration;

  private final Set<OnboardingItem> onboardingItems;

  @Inject
  public OnboardingManagerImpl(final Set<OnboardingItem> onboardingItems,
                               final OnboardingConfiguration onboardingConfiguration)
  {
    this.onboardingItems = checkNotNull(onboardingItems);
    this.onboardingConfiguration = checkNotNull(onboardingConfiguration);
  }

  @Override
  public boolean needsOnboarding() {
    return !getOnboardingItems().isEmpty();
  }

  @Override
  public List<OnboardingItem> getOnboardingItems() {
    if (onboardingConfiguration.isEnabled()) {
      return onboardingItems.stream().filter(OnboardingItem::applies)
          .sorted(Comparator.comparingInt(OnboardingItem::getPriority)).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
