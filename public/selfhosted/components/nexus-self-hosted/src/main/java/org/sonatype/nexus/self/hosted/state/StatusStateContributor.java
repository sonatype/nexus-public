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
package org.sonatype.nexus.self.hosted.state;

import java.util.Map;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.rapture.internal.state.StatusXO;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import org.springframework.stereotype.Component;

/**
 * Contributes {@code status} state.
 *
 * @since 3.0
 */
@Component
public class StatusStateContributor
    implements StateContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String STATE_ID = "status";

  private static final String DISTRIBUTION = "distribution";

  private static final String EDITION_ID = "editionId";

  private final ApplicationVersion applicationVersion;

  private final NexusEditionSelector nexusEditionSelector;

  @Autowired
  public StatusStateContributor(
      final ApplicationVersion applicationVersion,
      final NexusEditionSelector nexusEditionSelector)
  {
    this.applicationVersion = requireNonNull(applicationVersion);
    this.nexusEditionSelector = requireNonNull(nexusEditionSelector);
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {
    // Hardcoded literal safe: repository-cloud-aws/pom.xml:648 excludes nexus-self-hosted, so this bean only runs in
    // self-hosted builds.
    ImmutableMap.Builder<String, Object> state = ImmutableMap.<String, Object>builder()
        .put(STATE_ID, calculateStatus())
        .put(DISTRIBUTION, NexusDistributionType.SELF_HOSTED.getId());
    try {
      state.put(EDITION_ID, nexusEditionSelector.getCurrent().getId());
    }
    catch (IllegalStateException e) {
      log.warn("Omitting editionId from Rapture state: no active edition resolved", e);
    }
    return state.build();
  }

  private Object calculateStatus() {
    StatusXO result = new StatusXO();
    result.setVersion(applicationVersion.getVersion());
    // Report the real edition short name (PRO, PROPLUS, ENTERPRISE) rather than collapsing the Pro
    // family to "PRO", so the frontend can distinguish the tiers. The frontend still matches "PRO" by
    // strict equality and does not yet recognize "PROPLUS"/"ENTERPRISE"; that recognition is owned by a
    // separate frontend effort and must land before Pro+/Enterprise are shipped.
    result.setEdition(applicationVersion.getEdition());
    result.setBuildRevision(applicationVersion.getBuildRevision());
    result.setBuildTimestamp(applicationVersion.getBuildTimestamp());
    return result;
  }
}
