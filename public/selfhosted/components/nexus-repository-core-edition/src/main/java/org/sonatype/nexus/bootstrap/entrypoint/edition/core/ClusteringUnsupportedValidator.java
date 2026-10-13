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
package org.sonatype.nexus.bootstrap.entrypoint.edition.core;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE;

/**
 * Guards against Nexus Repository Core starting with {@code nexus.datastore.clustered.enabled=true}.
 * <p>
 * This module is loaded only by the OSS Core assembly, which can never cluster - there is no edition or license to
 * check here, unlike the equivalent guard shipped with the self-hosted distributions. Without this guard an OSS
 * Core instance boots happily with the flag set, producing a silently non-functional "cluster" rather than a clear
 * failure.
 */
@Component
public class ClusteringUnsupportedValidator
{
  private final boolean clusteredModeEnabled;

  @Autowired
  public ClusteringUnsupportedValidator(
      @Value(DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean clusteredModeEnabled)
  {
    this.clusteredModeEnabled = clusteredModeEnabled;
  }

  @PostConstruct
  public void enforceClusteringIsNotEnabled() {
    if (!clusteredModeEnabled) {
      return;
    }
    throw new IllegalStateException(
        "High Availability clustering is not supported by Nexus Repository Core. Set "
            + "nexus.datastore.clustered.enabled=false (or remove it) in nexus.properties - or, if it was set via "
            + "the DATASTORE_CLUSTERED_ENABLED environment variable, unset that instead.");
  }
}
