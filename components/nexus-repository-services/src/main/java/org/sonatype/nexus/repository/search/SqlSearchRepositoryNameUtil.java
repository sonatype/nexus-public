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
package org.sonatype.nexus.repository.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.repository.types.GroupType;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder.replaceWildcards;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport.maybeTrimQuotes;

/**
 * Expands repository_name {@link SearchFilter} (where provided) into leaf repository names (where applicable). If the
 * repository_name {@link SearchFilter} is not provided, it fetches repository names for the specified format.
 *
 * @since 3.next
 */
@Named
@Singleton
public class SqlSearchRepositoryNameUtil
    extends ComponentSupport
{
  public static final String SPACE = " ";

  private static final String DEFAULT_REGEX = "[^\\s\"]+|\"[^\"]+\"";

  private static final Pattern PATTERN = Pattern.compile(DEFAULT_REGEX);

  private final RepositoryManager repositoryManager;

  private final ConfigurationStore configurationStore;

  @Inject
  public SqlSearchRepositoryNameUtil(
      final RepositoryManager repositoryManager,
      final ConfigurationStore configurationStore)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.configurationStore = checkNotNull(configurationStore);
  }

  /**
   * Splits by space and expands the specified <code>repositoryNameFilter</code> into leaf repository names (where
   * applicable). Wildcard repository names are evaluated and the matching repositories are expanded into their leaf
   * repository names (where applicable).
   */
  public Set<String> getRepositoryNames(@Nullable final String repositoryNameFilter) {
    Set<String> repositories = ofNullable(repositoryNameFilter)
        .map(this::split)
        .orElse(new HashSet<>());

    Set<String> wildcards = getWildcardRepositoryNames(repositories);
    if (!wildcards.isEmpty()) {
      repositories.removeAll(wildcards);
      repositories.addAll(getMatchingRepositoryNames(wildcards));
    }
    return unmodifiableSet(expandRepositories(repositories));
  }

  /**
   * Fetches the leaf repository names for the specified format.
   */
  public Set<String> getFormatRepositoryNames(final String format) {
    if (isBlank(format)) {
      return emptySet();
    }

    return unmodifiableSet(expandRepositories(getFormatRepositories(format)));
  }

  private Set<String> getMatchingRepositoryNames(final Set<String> wildcards) {
    return configurationStore.readByNames(replaceWildcards(wildcards)).stream()
        .map(Configuration::getRepositoryName)
        .collect(toSet());
  }

  private Set<String> getWildcardRepositoryNames(final Set<String> repositories) {
    return repositories.stream().filter(SqlSearchQueryConditionBuilder::isWildcard).collect(toSet());
  }

  private Set<String> getFormatRepositories(final String format) {
    return StreamSupport.stream(repositoryManager.browse().spliterator(), false)
        .filter(repository -> repository.getFormat().getValue().equals(format))
        .map(Repository::getName)
        .collect(toSet());
  }

  private Set<String> expandRepositories(final Set<String> values) {
    Set<String> repositories = getLeafMembers(values);
    repositories.addAll(excludeGroupRepositories(values));
    return repositories;
  }

  private Set<String> excludeGroupRepositories(final Set<String> values) {
    Set<String> repositoryNames = new HashSet<>();
    for (String repositoryName : values) {
      Repository repository = repositoryManager.get(repositoryName);
      if (repository == null || isNotGroupRepository(repository)) {
        repositoryNames.add(repositoryName);
      }
    }
    return repositoryNames;
  }

  private boolean isNotGroupRepository(final Repository repository) {
    return !isGroupRepository(repository);
  }

  private boolean isGroupRepository(final Repository repository) {
    return GroupType.NAME.equals(repository.getType().getValue());
  }

  private Set<String> getLeafMembers(final Set<String> values) {
    return values.stream()
        .map(repositoryManager::get)
        .filter(Objects::nonNull)
        .filter(this::isGroupRepository)
        .map(repository -> repository.optionalFacet(GroupFacet.class))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(GroupFacet::leafMembers)
        .flatMap(Collection::stream)
        .map(Repository::getName)
        .collect(toSet());
  }

  private Set<String> split(String value) {
    if (isBlank(value)) {
      return emptySet();
    }

    Matcher matcher = PATTERN.matcher(value);
    Set<String> matches = new HashSet<>();
    while (matcher.find()) {
      matches.add(maybeTrimQuotes(matcher.group()));
    }
    return matches;
  }
}
