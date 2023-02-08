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
package org.sonatype.nexus.rapture.internal.state;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.systemchecks.NodeSystemCheckResult;
import org.sonatype.nexus.systemchecks.SystemCheckService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * State contributor that indicates if any of the system Health checks are currently failing
 *
 * @since 3.16
 */
@Named
@Singleton
public class HealthCheckStateContributor
    extends ComponentSupport
    implements StateContributor
{
  @VisibleForTesting
  protected static final String HC_FAILED_KEY = "health_checks_failed";

  private SystemCheckService systemCheckService;

  @Inject
  public HealthCheckStateContributor(final SystemCheckService systemCheckService) {
    this.systemCheckService = checkNotNull(systemCheckService);
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {

    boolean failed = systemCheckService.getResults()
        .map(NodeSystemCheckResult::getResult)
        .map(Map::values)
        .flatMap(Collection::stream)
        .anyMatch(result -> !result.isHealthy());

    return ImmutableMap.of(HC_FAILED_KEY, failed);
  }
}
