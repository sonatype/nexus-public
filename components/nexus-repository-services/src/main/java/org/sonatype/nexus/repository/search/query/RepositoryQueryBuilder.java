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
package org.sonatype.nexus.repository.search.query;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.repository.Repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

/**
 * Utilities to adapt search queries for various repository situations.
 *
 * @since 3.25.0
 */
public class RepositoryQueryBuilder
    extends QueryBuilder
{
  private final QueryBuilder query;

  List<SortBuilder> sort;

  Collection<String> repositoryNames;

  Duration timeout;

  boolean skipContentSelectors;

  private RepositoryQueryBuilder(final QueryBuilder query) {
    this.query = checkNotNull(query);
  }

  /**
   * Retrieve any repository search customizations.
   */
  public static RepositoryQueryBuilder repositoryQuery(final QueryBuilder query) {
    return query instanceof RepositoryQueryBuilder ? (RepositoryQueryBuilder) query : new RepositoryQueryBuilder(query);
  }

  /**
   * Apply sorting to this search.
   */
  public RepositoryQueryBuilder sortBy(final List<SortBuilder> sort) {
    this.sort = checkNotNull(sort);
    return this;
  }

  /**
   * Apply sorting to this search.
   */
  public RepositoryQueryBuilder sortBy(final SortBuilder... sort) {
    this.sort = ImmutableList.copyOf(sort);
    return this;
  }

  /**
   * Limit this search to the named repositories.
   */
  public RepositoryQueryBuilder inRepositories(final Collection<String> repositoryNames) {
    this.repositoryNames = checkNotNull(repositoryNames);
    return this;
  }

  /**
   * Limit this search to the named repositories.
   */
  public RepositoryQueryBuilder inRepositories(final String... repositoryNames) {
    this.repositoryNames = ImmutableList.copyOf(repositoryNames);
    return this;
  }

  /**
   * Limit this search to the named repositories.
   */
  public RepositoryQueryBuilder inRepositories(final Repository... repositories) {
    this.repositoryNames = stream(repositories).map(Repository::getName).collect(toImmutableList());
    return this;
  }

  /**
   * Limit this search to a particular duration.
   */
  public RepositoryQueryBuilder timeout(final Duration timeout) {
    this.timeout = checkNotNull(timeout);
    return this;
  }

  /**
   * Turn off content selector filtering for this search.
   */
  public RepositoryQueryBuilder unrestricted() {
    this.skipContentSelectors = true;
    return this;
  }

  /**
   * Turn off content selector filtering for this search.
   */
  public static RepositoryQueryBuilder unrestricted(final QueryBuilder query) {
    return repositoryQuery(query).unrestricted();
  }

  @Override
  public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
    // our customizations just alter the search request being built, the underlying query is left unchanged
    return query.toXContent(builder, params);
  }

  @Override
  protected void doXContent(final XContentBuilder builder, final Params params) throws IOException {
    // unused since we delegate to the underlying query builder
  }

  @VisibleForTesting
  public Collection<String> getRepositoryNames() {
    return ImmutableList.copyOf(repositoryNames);
  }
}
