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
package org.sonatype.nexus.proxy.cache;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Event inspector that listens for repository registry removals, and purges the {@link PathCache} belonging to given
 * repository. This event inspector is synchronous intentionally, as it relies on configuration of the repository, but
 * is also "short operation". Prolonging the repository removal procedure is also not an issue (unlike prolonging
 * artifact serving). Related to issue NEXUS-5109.
 *
 * @author cstamas
 * @since 2.1
 */
@Named
@Singleton
public class PathCacheEventInspector
    extends ComponentSupport
    implements EventSubscriber
{
  private final CacheManager cacheManager;

  @Inject
  public PathCacheEventInspector(final CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryRegistryEventRemove removedRepositoryEvent) {
    final Repository removedRepository = removedRepositoryEvent.getRepository();
    final PathCache pathCache = cacheManager.getPathCache(removedRepository.getId());
    if (log.isDebugEnabled()) {
      log.debug(
          "Purging NFC PathCache of repository {}",
          RepositoryStringUtils.getHumanizedNameString(removedRepository));
    }
    pathCache.purge();
  }
}
