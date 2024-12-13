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
package org.sonatype.nexus.repository.content.search.elasticsearch;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.repository.content.search.SearchEventHandler;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static org.sonatype.nexus.common.app.FeatureFlags.ELASTIC_SEARCH_ENABLED;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

@FeatureFlag(name = ELASTIC_SEARCH_ENABLED, enabledByDefault = true)
@ManagedLifecycle(phase = SERVICES)
@Named
@Singleton
public class DefaultSearchEventHandler
    extends SearchEventHandler
{
  @Inject
  public DefaultSearchEventHandler(
      final RepositoryManager repositoryManager,
      final PeriodicJobService periodicJobService,
      @Named("${" + FLUSH_ON_COUNT_KEY + ":-100}") final int flushOnCount,
      @Named("${" + FLUSH_ON_SECONDS_KEY + ":-2}") final int flushOnSeconds,
      @Named("${" + NO_PURGE_DELAY_KEY + ":-true}") final boolean noPurgeDelay,
      @Named("${" + FLUSH_POOL_SIZE + ":-128}") final int poolSize)
  {
    super(repositoryManager, periodicJobService, flushOnCount, flushOnSeconds, noPurgeDelay, poolSize);
  }
}
