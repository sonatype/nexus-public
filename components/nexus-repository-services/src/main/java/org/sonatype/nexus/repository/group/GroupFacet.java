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
package org.sonatype.nexus.repository.group;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;

/**
 * Group facet.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface GroupFacet
    extends Facet
{
  /**
   * Check if given repository is a member of the group.
   */
  boolean member(String repositoryName);

  /**
   * Check if given repository is a member of the group.
   */
  boolean member(Repository repository);

  /**
   * Return list of all member repositories including transitive
   *
   * @since 3.6.1
   */
  List<Repository> allMembers();

  /**
   * Return list of (non-transitive) member repositories.
   */
  List<Repository> members();

  /**
   * Return the full list of members, including the members of groups, but excluding groups.
   */
  List<Repository> leafMembers();

  /**
   * Removes all entries from the group cache and the member caches.
   */
  void invalidateGroupCaches();

  /**
   * Returns {@code true} if the content is considered stale; otherwise {@code false}.
   */
  boolean isStale(@Nullable final Content content);

  /**
   * Maintains the latest cache information in the given content's attributes.
   */
  void maintainCacheInfo(final AttributesMap attributesMap);
}
