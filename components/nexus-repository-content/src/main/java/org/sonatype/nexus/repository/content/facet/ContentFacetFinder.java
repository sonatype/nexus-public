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
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.RepositoryStoppedEvent;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.group.GroupFacet;

import com.google.common.collect.ImmutableList;
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
  private final Map<String, Repository> repositoriesByContentId = new ConcurrentHashMap<>();

  /**
   * Finds the repository the given content was uploaded to.
   */
  public Optional<Repository> findRepository(final String format, final RepositoryContent content) {
    return findRepository(format, contentRepositoryId(content));
  }

  /**
   * Finds the {@link ContentFacet} of the repository the given content was uploaded to.
   */
  public Optional<ContentFacet> findContentFacet(final String format, final RepositoryContent content) {
    return findContentFacet(format, contentRepositoryId(content));
  }

  /**
   * Finds the repository with the given contentRepositoryId.
   */
  public Optional<Repository> findRepository(final String format, final int contentRepositoryId) {
    return ofNullable(repositoriesByContentId.get(cacheKey(format, contentRepositoryId)));
  }

  /**
   * Finds the {@link ContentFacet} of the repository with the given contentRepositoryId.
   */
  public Optional<ContentFacet> findContentFacet(final String format, final int contentRepositoryId) {
    return findRepository(format, contentRepositoryId).map(r -> r.facet(ContentFacet.class));
  }

  /**
   * Finds the upper-most {@link ContentFacet}s contained within the given repository
   */
  public static Stream<ContentFacet> findContentFacets(final Repository repository) {

    // repository at this level has a content facet, use that
    Optional<ContentFacet> contentFacet = repository.optionalFacet(ContentFacet.class);
    if (contentFacet.isPresent()) {
      return Stream.of(contentFacet.get());
    }

    // otherwise repeat this search for any next-level repositories and merge the results
    return repository.optionalFacet(GroupFacet.class)
        .map(GroupFacet::members)
        .orElse(ImmutableList.of())
        .stream()
        .flatMap(ContentFacetFinder::findContentFacets);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final RepositoryStartedEvent event) {
    Repository repository = event.getRepository();
    String format = repository.getFormat().getValue();

    repository.optionalFacet(ContentFacet.class).ifPresent(
        facet -> repositoriesByContentId.put(cacheKey(format, facet.contentRepositoryId()), repository));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final RepositoryStoppedEvent event) {
    Repository repository = event.getRepository();
    String format = repository.getFormat().getValue();

    repository.optionalFacet(ContentFacet.class).ifPresent(
        facet -> repositoriesByContentId.remove(cacheKey(format, facet.contentRepositoryId())));
  }

  /**
   * Bind format and repository id to get a unique key across all formats.
   */
  private String cacheKey(final String format, final int contentRepositoryId) {
    return format + ':' + contentRepositoryId;
  }
}
