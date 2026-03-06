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
package org.sonatype.nexus.repository.search.sql.query.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SingleValueTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;

/**
 * Appends the query parameters necessary for permission
 */
@Component
@Singleton
public class SqlSearchPermissionBuilder
    extends ComponentSupport
{
  private final RepositoryManager repositoryManager;

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  private final SearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator;

  @Inject
  public SqlSearchPermissionBuilder(
      final RepositoryManager repositoryManager,
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final SearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.contentSelectorFilterGenerator = checkNotNull(contentSelectorFilterGenerator);
  }

  /**
   * Adds authorization to a {@link SqlSearchQueryBuilderImpl} based on repository access, and the users requested
   * repositories.
   *
   * @param originalQuery the query without authorization
   * @param request the search request
   * @return the original query if no restriction is required, or a new query
   *         {@code (originalQuery) AND (hasPermission)}
   */
  public Optional<Expression> build(final SearchRequest request) {
    Set<Repository> specifiedRepositories = computeSpecifiedRepositories(getRepositoriesInRequest(request));

    if (specifiedRepositories.isEmpty()) {
      Set<Repository> allRepositories = getAllRepositories();
      Set<Repository> repositoriesWithAccess = computeRepositoriesWithAccess(allRepositories);

      if (allRepositories.size() == repositoriesWithAccess.size()) {
        return Optional.empty();
      }
      return addRepositoryConditions(allRepositories, repositoriesWithAccess, request);
    }
    return addRepositoryConditions(specifiedRepositories,
        computeRepositoriesWithAccess(specifiedRepositories), request);
  }

  private Optional<Expression> addRepositoryConditions(
      final Set<Repository> specifiedRepositories,
      Set<Repository> repositoriesWithCompleteAccess,
      final SearchRequest request)
  {
    // ContentSelector to applicable repositories we compute this here as we change repositoriesWithCompleteAccess later
    Map<SelectorConfiguration, List<Repository>> contentSelectorToRepositories =
        request.isCheckAuthorization()
            ? contentSelectorsApplicableFor(
                complement(specifiedRepositories, repositoriesWithCompleteAccess))
            : emptyMap();

    Optional<Expression> directAccessCondition = Optional.empty();
    Optional<Expression> contentAccessCondition = Optional.empty();

    if (!repositoriesWithCompleteAccess.isEmpty()) {
      // Expand the groups, we reassign here so we can filter the full leaf list from the content selectors later
      repositoriesWithCompleteAccess = repositoriesWithCompleteAccess.stream()
          .flatMap(this::expandIfGroup)
          .collect(Collectors.toCollection(LinkedHashSet::new));

      directAccessCondition = createRepositoryCondition(repositoriesWithCompleteAccess);
    }

    if (request.isCheckAuthorization() && !contentSelectorToRepositories.isEmpty()) {
      contentAccessCondition =
          createContentSelectorCondition(contentSelectorToRepositories, repositoriesWithCompleteAccess);
    }

    if (!directAccessCondition.isPresent() && !contentAccessCondition.isPresent()) {
      throw new SqlSearchPermissionException("User does not have permissions to required repositories.");
    }

    if (directAccessCondition.isPresent() && contentAccessCondition.isPresent()) {
      return Optional
          .of(SqlClause.create(Operand.OR, directAccessCondition.get(), contentAccessCondition.get()));
    }

    return directAccessCondition.isPresent() ? directAccessCondition : contentAccessCondition;
  }

  /*
   * Create a condition for the repositories the user has complete access to.
   */
  private Optional<Expression> createRepositoryCondition(final Set<Repository> repositories) {
    if (repositories.isEmpty()) {
      return Optional.empty();
    }

    Set<SingleValueTerm<?>> repositoryNames = repositories.stream()
        .map(Repository::getName)
        .distinct()
        .map(ExactTerm::new)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (log.isDebugEnabled()) {
      log.debug("Creating repository condition for {} repositories: {}",
          repositoryNames.size(),
          repositoryNames.stream()
              .map(term -> ((ExactTerm) term).term())
              .limit(10)
              .collect(Collectors.toList()) + (repositoryNames.size() > 10 ? "..." : ""));
    }

    SqlPredicate predicate =
        new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME, TermCollection.create(repositoryNames));

    return Optional.of(predicate);
  }

  /*
   * Create an SqlSearchQueryCondition based on the provided content selectors.
   */
  private Optional<Expression> createContentSelectorCondition(
      final Map<SelectorConfiguration, List<Repository>> selectorsToRepositories,
      final Set<Repository> fullAccessRepositories)
  {
    List<Expression> expressions = new ArrayList<>();

    for (Entry<SelectorConfiguration, List<Repository>> entry : selectorsToRepositories.entrySet()) {
      Set<String> applicableRepositories = entry.getValue()
          .stream()
          // expand to the members of the group
          .flatMap(this::expandIfGroup)
          // If the user has full access to the repository, we don't need to verify via a content selector
          .filter(repository -> !fullAccessRepositories.contains(repository))
          .map(Repository::getName)
          .collect(Collectors.toCollection(LinkedHashSet::new));

      if (!applicableRepositories.isEmpty()) {
        // Only create a condition if there are repositories after the previous operation
        contentSelectorFilterGenerator.createFilter(entry.getKey(), applicableRepositories)
            .ifPresent(expressions::add);
      }
    }

    if (expressions.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(SqlClause.create(Operand.OR, expressions));
  }

  /*
   * Computes the repositories specified by the user in the search request
   */
  private Set<Repository> computeSpecifiedRepositories(final Collection<String> repositories) {
    if (repositories.isEmpty()) {
      return emptySet();
    }

    Set<Repository> specifiedRepositories = repositories.stream()
        .flatMap(this::expandWildcards)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (!specifiedRepositories.isEmpty()) {
      return specifiedRepositories;
    }
    throw new UnknownRepositoriesException(repositories); // No repositories match
  }

  private Set<Repository> getAllRepositories() {
    Set<Repository> repositories = StreamSupport.stream(repositoryManager.browse().spliterator(), false)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    log.debug("getAllRepositories found {} repositories", repositories.size());
    return repositories;
  }

  private Collection<String> getRepositoriesInRequest(final SearchRequest request) {
    return Stream.concat(
        request.getRepositories().stream(),
        request.getSearchFilters()
            .stream()
            .filter(filter -> REPOSITORY_NAME.equals(filter.getProperty()))
            .map(SearchFilter::getValue)
            .flatMap(filter -> Stream.of(filter.split(" "))))
        // Some customers are sending requests with the repository query param but no value
        // we need to filter these out to avoid an error response
        .filter(Strings2::notBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /*
   * Given a repository name from a user, expands wildcards to matching repository names, and filters non-wildcard
   * repositories if they are unknown
   */
  private Stream<Repository> expandWildcards(final String repositoryName) {
    Stream<Repository> repositories = StreamSupport.stream(repositoryManager.browse().spliterator(), false);

    if (containsWildcards(repositoryName)) {
      Pattern repoNamePatterns = createPattern(repositoryName);

      return repositories.filter(repository -> repoNamePatterns.matcher(repository.getName()).matches());
    }
    return Optional.ofNullable(repositoryManager.get(repositoryName))
        .map(Stream::of)
        .orElseGet(Stream::empty);
  }

  /*
   * Returns a stream of distinct group members, or a stream containing only a single repository
   */
  private Stream<Repository> expandIfGroup(final Repository repository) {
    return repository.optionalFacet(GroupFacet.class)
        .map(GroupFacet::leafMembers)
        .map(Collection::stream)
        .orElseGet(() -> Stream.of(repository))
        .distinct();
  }

  /*
   * Compute repositories to which the user has direct access
   */
  private Set<Repository> computeRepositoriesWithAccess(final Set<Repository> repositories) {
    return repositories.stream()
        .filter(repository -> securityHelper.anyPermitted(
            new RepositoryViewPermission(repository.getFormat().getValue(), repository.getName(), BreadActions.BROWSE),
            new RepositoryViewPermission(repository.getFormat().getValue(), repository.getName(), BreadActions.READ)))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /*
   * Create a map of contentSelector -> list of applicable repositories.
   */
  private Map<SelectorConfiguration, List<Repository>> contentSelectorsApplicableFor(
      final Collection<Repository> repositories)
  {
    Map<SelectorConfiguration, List<Repository>> selectorToRepositories = new HashMap<>();
    repositories
        .forEach(repository -> selectorManager
            .browseActive(Collections.singleton(repository.getName()),
                Collections.singleton(repository.getFormat().getValue()))
            .stream()
            .forEach(
                selector -> selectorToRepositories.computeIfAbsent(selector, __ -> new ArrayList<>()).add(repository)));

    return selectorToRepositories;
  }

  /*
   * Create a Pattern from a simple wildcard filter
   */
  private static Pattern createPattern(final String filter) {
    return Pattern.compile(filter.replace("?", ".").replace("*", ".*"));
  }

  private static boolean containsWildcards(final String filter) {
    return filter.contains("*") || filter.contains("?");
  }

  /*
   * Returns a new Set containing the entries in setA which are not present in setB
   *
   * https://en.wikipedia.org/wiki/Complement_(set_theory)
   */
  private static <E> Set<E> complement(final Set<E> setA, final Set<E> setB) {
    return setA.stream()
        .filter(entry -> !setB.contains(entry))
        .collect(toSet());
  }
}
