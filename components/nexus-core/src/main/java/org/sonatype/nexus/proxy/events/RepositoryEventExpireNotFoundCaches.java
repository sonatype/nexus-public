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
package org.sonatype.nexus.proxy.events;

import java.util.Map;

import org.sonatype.nexus.proxy.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The event fired on Expiring the Not Found cache of the repository.
 *
 * @author cstamas
 * @since 2.0
 */
public class RepositoryEventExpireNotFoundCaches
    extends RepositoryMaintenanceEvent
{
  /**
   * From where it happened
   */
  private final String path;

  /**
   * Request initiating it
   */
  private final Map<String, Object> requestContext;

  /**
   * Flag marking was actually some entries removed or not
   */
  private final boolean cacheAltered;

  public RepositoryEventExpireNotFoundCaches(final Repository repository, final String path,
                                             final Map<String, Object> requestContext, final boolean cacheAltered)
  {
    super(repository);
    this.path = checkNotNull(path);
    this.requestContext = checkNotNull(requestContext);
    this.cacheAltered = cacheAltered;
  }

  /**
   * Returns the repository path against which expire proxy caches was invoked.
   */
  public String getPath() {
    return path;
  }

  /**
   * Returns the copy of the request context in the moment expire proxy caches was invoked.
   */
  public Map<String, Object> getRequestContext() {
    return requestContext;
  }

  /**
   * Returns true if expire not found caches actually did remove entries from the cache (cache alteration happened).
   */
  public boolean isCacheAltered() {
    return cacheAltered;
  }
}
