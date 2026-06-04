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
package org.sonatype.nexus.coreui.internal;

import java.util.Map;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;

import static org.sonatype.nexus.common.app.FeatureFlags.CLUSTERED_ZERO_DOWNTIME_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.CLUSTERED_ZERO_DOWNTIME_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE;
import org.springframework.stereotype.Component;

@Component
public class ClusteredModeStateContributor
    implements StateContributor
{
  private final boolean clusteredModeEnabled;

  private final boolean zeroDowntimeEnabled;

  @Autowired
  public ClusteredModeStateContributor(
      @Value(DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean clusteredModeEnabled,
      @Value(CLUSTERED_ZERO_DOWNTIME_ENABLED_NAMED_VALUE) final boolean zeroDowntimeEnabled)
  {
    this.clusteredModeEnabled = clusteredModeEnabled;
    this.zeroDowntimeEnabled = zeroDowntimeEnabled;
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        DATASTORE_CLUSTERED_ENABLED, clusteredModeEnabled,
        CLUSTERED_ZERO_DOWNTIME_ENABLED, zeroDowntimeEnabled);
  }
}
