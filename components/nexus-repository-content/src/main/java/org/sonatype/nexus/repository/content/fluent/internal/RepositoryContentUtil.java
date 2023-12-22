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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.constraints.FluentQueryConstraint;
import org.sonatype.nexus.repository.types.GroupType;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

/**
 * Utility methods used by both {@link FluentAssetsImpl} and {@link FluentComponentsImpl}
 *
 * @since 3.27
 */
final class RepositoryContentUtil
{
  private RepositoryContentUtil() {
  }

  static boolean isGroupRepository(final Repository repository) {
    return GroupType.NAME.equals(repository.getType().getValue());
  }

  static Set<Integer> getRepositoryIds(
      @Nullable final List<FluentQueryConstraint> constraints,
      final ContentFacet contentFacet,
      final Repository repository)
  {
    // no constraints supplied, just use the repository of the contentFacet supplied
    if (constraints == null || constraints.isEmpty()) {
      return singleton(contentFacet.contentRepositoryId());
    }

    Set<Integer> repositoryIds = constraints.stream()
        .map(constraint -> constraint.getRepositoryIds(repository))
        .flatMap(Collection::stream)
        .collect(toSet());

    constraints.forEach(constraint -> repositoryIds.addAll(constraint.getRepositoryIds(repository)));

    // if we get to this point and no repository has been selected based on constraints, fallback to the repository
    // of the contentFacet supplied
    if (repositoryIds.isEmpty()) {
      repositoryIds.add(contentFacet.contentRepositoryId());
    }

    return repositoryIds;
  }
}
