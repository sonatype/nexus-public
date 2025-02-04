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
import java.util.Map;
import java.util.Optional;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class CommunityOnboardingItem
    implements OnboardingItem
{
  private final String COMMUNITY = "COMMUNITY";

  private final String EULA_KEY = "nexus.community.eula.accepted";

  protected final ApplicationVersion applicationVersion;

  protected final GlobalKeyValueStore globalKeyValueStore;

  @Inject
  public CommunityOnboardingItem(
      final ApplicationVersion applicationVersion,
      final GlobalKeyValueStore globalKeyValueStore)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.globalKeyValueStore = checkNotNull(globalKeyValueStore);
  }

  @Override
  public boolean applies() {
    boolean isCommunity = applicationVersion.getEdition().equalsIgnoreCase(COMMUNITY);
    boolean accepted = false;

    Optional<NexusKeyValue> eulaStatusOptional = globalKeyValueStore.getKey(EULA_KEY);
    if (eulaStatusOptional.isPresent()) {
      NexusKeyValue eulaStatus = eulaStatusOptional.get();
      Map<String, Object> eulaObject = eulaStatus.value();
      accepted = (Boolean) eulaObject.get("accepted");
    }

    return isCommunity && !accepted;
  }
}
