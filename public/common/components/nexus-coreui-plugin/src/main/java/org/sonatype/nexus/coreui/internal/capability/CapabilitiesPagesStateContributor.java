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
package org.sonatype.nexus.coreui.internal.capability;

import java.util.Map;

import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.EXTJS_CAPABILITIES_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.EXTJS_CAPABILITIES_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.REACT_CAPABILITIES_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.REACT_CAPABILITIES_NAMED_VALUE;

@Component
public class CapabilitiesPagesStateContributor
    implements StateContributor
{
  private final boolean isExtjsCapabilitiesEnabled;

  private final boolean isReactCapabilitiesEnabled;

  @Autowired
  public CapabilitiesPagesStateContributor(
      @Value(EXTJS_CAPABILITIES_NAMED_VALUE) final boolean isExtjsCapabilitiesEnabled,
      @Value(REACT_CAPABILITIES_NAMED_VALUE) final boolean isReactCapabilitiesEnabled)
  {
    this.isExtjsCapabilitiesEnabled = isExtjsCapabilitiesEnabled;
    this.isReactCapabilitiesEnabled = isReactCapabilitiesEnabled;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        EXTJS_CAPABILITIES_ENABLED, isExtjsCapabilitiesEnabled,
        REACT_CAPABILITIES_ENABLED, isReactCapabilitiesEnabled);
  }
}
