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
package org.sonatype.nexus.repository.content.facet;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.RepositoryStoppedEvent;
import org.sonatype.nexus.repository.content.RepositoryContent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.content.store.InternalIds.contentRepositoryId;

/**
 * Provides various methods for finding {@link ContentFacet}s and their repositories.
 *
 * @since 3.26
 */
@Named
@Singleton
public class ContentFacetFinder
    extends ComponentSupport
    implements EventAware
{
  private final Map<Integer, Repository> repositoriesByContentId = new ConcurrentHashMap<>();

  /**
   * Finds the repository the given content was uploaded to.
   */
  public Optional<Repository> findRepository(final RepositoryContent content) {
    return findRepository(contentRepositoryId(content));
  }

  /**
   * Finds the {@link ContentFacet} of the repository the given content was uploaded to.
   */
  public Optional<ContentFacet> findContentFacet(final RepositoryContent content) {
    return findContentFacet(contentRepositoryId(content));
  }

  /**
   * Finds the repository with the given contentRepositoryId.
   */
  public Optional<Repository> findRepository(final int contentRepositoryId) {
    return ofNullable(repositoriesByContentId.get(contentRepositoryId));
  }

  /**
   * Finds the {@link ContentFacet} of the repository with the given contentRepositoryId.
   */
  public Optional<ContentFacet> findContentFacet(final int contentRepositoryId) {
    return findRepository(contentRepositoryId).map(r -> r.facet(ContentFacet.class));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final RepositoryStartedEvent event) {
    event.getRepository().optionalFacet(ContentFacet.class)
        .ifPresent(facet -> repositoriesByContentId.put(facet.contentRepositoryId(), event.getRepository()));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final RepositoryStoppedEvent event) {
    event.getRepository().optionalFacet(ContentFacet.class)
        .ifPresent(facet -> repositoriesByContentId.remove(facet.contentRepositoryId()));
  }
}
