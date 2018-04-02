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
package org.sonatype.nexus.repository.npm.internal.search.legacy;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * npm search index invalidation event, fired by npm repositories when {@link NpmSearchIndexFacet#invalidateCachedSearchIndex()}
 * is invoked. Used to propagate the invalidation event to groups where repository with invalidated index cache is
 * member.
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
public class NpmSearchIndexInvalidatedEvent
{
  private final Repository repository;

  public NpmSearchIndexInvalidatedEvent(final Repository repository) {
    this.repository = checkNotNull(repository);
  }

  /**
   * The npm repository who's cached npm index was invalidated.
   */
  @Nonnull
  public Repository getRepository() {
    return repository;
  }
}
