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
package org.sonatype.nexus.repository.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.google.common.collect.Iterables;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.security.BreadActions.READ;
import org.springframework.stereotype.Component;

/**
 * Repository permission checker.
 *
 * @since 3.10
 */
@Component
@Singleton
public class RepositoryPermissionChecker
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  private final Map<String, Recipe> recipes;

  @Inject
  public RepositoryPermissionChecker(
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final List<Recipe> recipesList)
  {
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.recipes = QualifierUtil.buildQualifierBeanMap(checkNotNull(recipesList));
  }

  /**
   * WARNING: This method should _only_ be used to check a single repository to prevent performance problems with large
   * numbers of content selectors. Use userCanBrowseRepositories instead to check multiple repositories.
   *
   * @return true if the user can browse or read the repository or if the user has a content selector granting access
   */
  public boolean userCanReadOrBrowse(final Repository repository) {
    return userHasRepositoryViewPermissionTo(repository, BROWSE, READ)
        || userHasAnyContentSelectorAccessTo(repository, BROWSE, READ);
  }

  /**
   * @param repository
   * @return true if user can delete anything within the repository based on repository or content selector privilege
   *
   * @since 3.15
   */
  public boolean userCanDeleteInRepository(final Repository repository) {
    return userHasRepositoryViewPermissionTo(DELETE, repository)
        || userHasAnyContentSelectorAccessTo(repository, DELETE);
  }

  private boolean userHasRepositoryViewPermissionTo(final Repository repository, final String... actions) {
    return securityHelper.anyPermitted(permissionsFor(repository, actions));
  }

  private boolean userHasRepositoryViewPermissionTo(final String action, final Repository repository) {
    return securityHelper.anyPermitted(new RepositoryViewPermission(repository, action));
  }

  private static Permission[] permissionsFor(final Repository repository, final String... actions) {
    return Arrays.stream(actions)
        .map(action -> new RepositoryViewPermission(repository, action))
        .toArray(Permission[]::new);
  }

  /**
   * @since 3.13
   * @param repositories to test against browse permissions and content selector permissions
   * @return the repositories which the user has access to browse
   */
  public List<Repository> userCanBrowseRepositories(final Repository... repositories) {
    Subject subject = securityHelper.subject();
    List<Repository> filteredRepositories = new ArrayList<>(Arrays.asList(repositories));
    List<Repository> permittedRepositories =
        userHasPermission(r -> new RepositoryViewPermission(r, BROWSE), repositories);
    filteredRepositories.removeAll(permittedRepositories);

    if (!filteredRepositories.isEmpty()) {
      permittedRepositories.addAll(subjectHasAnyContentSelectorAccessTo(subject, filteredRepositories));
    }

    return permittedRepositories;
  }

  /**
   * @param repositories to test against browse permissions and content selector permissions
   * @return the repositories which the user has access to browse
   */
  public List<Configuration> userCanBrowseRepositories(final Configuration... repositories) {
    Subject subject = securityHelper.subject();
    // Filter out repositories with unavailable recipes first - they should not be accessible
    Configuration[] availableRepositories = Arrays.stream(repositories)
        .filter(this::hasAvailableRecipe)
        .toArray(Configuration[]::new);

    List<Configuration> filteredRepositories = new ArrayList<>(Arrays.asList(availableRepositories));
    List<Configuration> permittedRepositories =
        userHasPermission(c -> new RepositoryViewPermission(toFormat(c), c.getRepositoryName(), BROWSE),
            availableRepositories);
    filteredRepositories.removeAll(permittedRepositories);

    if (!filteredRepositories.isEmpty()) {
      permittedRepositories.addAll(subjectHasAnyContentSelectorAccessToConfiguration(subject, filteredRepositories));
    }

    return permittedRepositories;
  }

  /**
   * Ensures the user has any of the supplied permissions, or a RepositoryAdminPermission with the action to any
   * of the repositories. Throws an AuthorizationException if the user does not have the required permission.
   *
   * @since 3.17
   * @param permissions the permissions to check first
   * @param action the action to use in the admin permission
   * @param repositories the repositories to check the action against
   * @throws AuthorizationException if the user doesn't have permission
   */
  public void ensureUserHasAnyPermissionOrAdminAccess(
      final Iterable<Permission> permissions,
      final String action,
      final Iterable<Repository> repositories)
  {
    Subject subject = securityHelper.subject();
    if (securityHelper.anyPermitted(subject, permissions)) {
      return;
    }

    Permission[] actionPermissions = StreamSupport.stream(repositories.spliterator(), false)
        .map(r -> new RepositoryAdminPermission(r, action))
        .toArray(Permission[]::new);
    securityHelper.ensureAnyPermitted(subject, actionPermissions);
  }

  /**
   * @since 3.17
   * @param repositories to test against browse permissions and content selector permissions
   * @return the repositories which the user has access to browse
   */
  public List<Repository> userCanBrowseRepositories(final Iterable<Repository> repositories) {
    return userCanBrowseRepositories(Iterables.toArray(repositories, Repository.class));
  }

  /**
   * @param repository to test against admin permissions
   * @param actions the admin actions to test the user for
   * @return true if the user has permission to perform the admin actions on the repository
   */
  public boolean userHasRepositoryAdminPermission(final Repository repository, final String... actions) {
    return !userHasPermission(r -> new RepositoryAdminPermission(r, actions), repository).isEmpty();
  }

  /**
   * @since 3.17
   * @param repositories to test the actions permission against
   * @param actions the repository-admin actions
   * @return the repositories which the user is permitted the admin action
   */
  public List<Repository> userHasRepositoryAdminPermission(
      final Iterable<Repository> repositories,
      final String... actions)
  {
    Repository[] repos = Iterables.toArray(repositories, Repository.class);
    return userHasPermission(r -> new RepositoryAdminPermission(r, actions), repos);
  }

  /**
   * @param configurations to test the actions permission against
   * @param actions the repository-admin actions
   * @return the repositories which the user is permitted the admin action
   */
  public List<Configuration> userHasRepositoryAdminPermissionFor(
      final Iterable<Configuration> configurations,
      final String... actions)
  {
    // Filter out configurations with unavailable recipes
    Configuration[] repos = StreamSupport.stream(configurations.spliterator(), false)
        .filter(this::hasAvailableRecipe)
        .toArray(Configuration[]::new);
    return userHasPermission(c -> new RepositoryAdminPermission(toFormat(c), c.getRepositoryName(), actions), repos);
  }

  /**
   * Ensures that the current user has an administrative privilege with the given action to the given repository.
   *
   * @since 3.20
   *
   * @throws AuthorizationException
   */
  public void ensureUserCanAdmin(final String action, final Repository repository) {
    securityHelper.ensurePermitted(
        new RepositoryAdminPermission(repository.getFormat().getValue(), repository.getName(), singletonList(action)));
  }

  /**
   * @since 3.20
   */
  public void ensureUserCanAdmin(final String action, final String format, final String repositoryName) {
    securityHelper.ensurePermitted(new RepositoryAdminPermission(format, repositoryName, singletonList(action)));
  }

  private <U> List<U> userHasPermission(
      final Function<U, Permission> permissionSupplier,
      final U... repositories)
  {
    if (repositories.length == 0) {
      return Collections.emptyList();
    }
    Subject subject = securityHelper.subject();
    Permission[] permissions = Arrays.stream(repositories).map(permissionSupplier).toArray(Permission[]::new);
    boolean[] results = securityHelper.isPermitted(subject, permissions);

    List<U> permittedRepositories = new ArrayList<>();

    for (int i = 0; i < results.length; i++) {
      if (results[i]) {
        permittedRepositories.add(repositories[i]);
      }
    }

    return permittedRepositories;
  }

  private List<Repository> subjectHasAnyContentSelectorAccessTo(
      final Subject subject,
      final List<Repository> repositories)
  {
    List<String> repositoryNames = repositories.stream().map(r -> r.getName()).collect(Collectors.toList());
    List<String> formats = repositories.stream()
        .map(r -> r.getFormat().getValue())
        .distinct()
        .collect(Collectors.toList());
    List<SelectorConfiguration> selectors = selectorManager.browseActive(repositoryNames, formats);

    if (selectors.isEmpty()) {
      return Collections.emptyList();
    }

    List<Repository> permittedRepositories = new ArrayList<>();
    for (Repository repository : repositories) {
      Permission[] permissions = selectors.stream()
          .map(s -> new RepositoryContentSelectorPermission(s, repository, singletonList(BROWSE)))
          .toArray(Permission[]::new);
      if (securityHelper.anyPermitted(subject, permissions)) {
        permittedRepositories.add(repository);
      }
    }

    return permittedRepositories;
  }

  private List<Configuration> subjectHasAnyContentSelectorAccessToConfiguration(
      final Subject subject,
      final List<Configuration> configurations)
  {
    // Filter configurations to only those with available recipes
    List<Configuration> availableConfigurations = configurations.stream()
        .filter(this::hasAvailableRecipe)
        .collect(Collectors.toList());

    if (availableConfigurations.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> repositoryNames = availableConfigurations.stream()
        .map(Configuration::getRepositoryName)
        .collect(Collectors.toList());
    List<String> formats = availableConfigurations.stream()
        .map(this::toFormat)
        .filter(format -> format != null) // Defensive: filter out null formats
        .distinct()
        .collect(Collectors.toList());

    if (formats.isEmpty()) {
      return Collections.emptyList();
    }

    List<SelectorConfiguration> selectors = selectorManager.browseActive(repositoryNames, formats);

    if (selectors.isEmpty()) {
      return Collections.emptyList();
    }

    List<Configuration> permittedRepositories = new ArrayList<>();
    for (Configuration configuration : availableConfigurations) {
      String format = toFormat(configuration);
      if (format == null) {
        continue; // Skip configurations with unavailable formats
      }
      Permission[] permissions = selectors.stream()
          .map(s -> new RepositoryContentSelectorPermission(s.getName(), format,
              configuration.getRepositoryName(), singletonList(BROWSE)))
          .toArray(Permission[]::new);
      if (securityHelper.anyPermitted(subject, permissions)) {
        permittedRepositories.add(configuration);
      }
    }

    return permittedRepositories;
  }

  /**
   * Converts a repository configuration to its format name.
   *
   * @param configuration the repository configuration
   * @return the format name, or null if the recipe is not available
   */
  private String toFormat(final Configuration configuration) {
    Recipe recipe = recipes.get(configuration.getRecipeName());
    if (recipe == null) {
      log.warn(
          "Recipe '{}' not available for repository '{}'. This repository will be excluded from permission checks. "
              + "This may occur if the format plugin is not loaded or if the repository was configured for an unavailable edition.",
          configuration.getRecipeName(), configuration.getRepositoryName());
      return null;
    }
    return recipe.getFormat().getValue();
  }

  /**
   * Checks if a configuration has an available recipe.
   *
   * @param configuration the repository configuration to check
   * @return true if the recipe is available, false otherwise
   */
  private boolean hasAvailableRecipe(final Configuration configuration) {
    boolean available = recipes.containsKey(configuration.getRecipeName());
    if (!available) {
      log.debug("Recipe '{}' not available for repository '{}' - filtering out",
          configuration.getRecipeName(), configuration.getRepositoryName());
    }
    return available;
  }

  private boolean userHasAnyContentSelectorAccessTo(final Repository repository, final String... actions) {
    Subject subject = securityHelper.subject();
    List<String> repositoryNames = singletonList(repository.getName());
    List<String> formats = singletonList(repository.getFormat().getValue());

    return selectorManager.browseActive(repositoryNames, formats)
        .stream()
        .anyMatch(selector -> {
          boolean hasPrivilege = securityHelper.anyPermitted(subject,
              Arrays.stream(actions)
                  .map(action -> new RepositoryContentSelectorPermission(selector, repository, singletonList(action)))
                  .toArray(Permission[]::new));

          boolean selectorMatches = evaluateSelectorExpression(selector, repository);

          return hasPrivilege && selectorMatches;
        });
  }

  private boolean evaluateSelectorExpression(final SelectorConfiguration selector, final Repository repository) {
    try {
      VariableSourceBuilder builder = new VariableSourceBuilder();
      builder.addResolver(new ConstantVariableResolver(repository.getFormat().getValue(), "format"));
      builder.addResolver(new ConstantVariableResolver(repository.getName(), "repository"));

      VariableSource variableSource = builder.build();

      return selectorManager.evaluate(selector, variableSource);
    }
    catch (SelectorEvaluationException e) {
      // Check if this is due to missing 'path' variable (asset-level variable at repository-level evaluation)
      if (e.getCause() != null && e.getCause().getMessage() != null &&
          e.getCause().getMessage().contains("undefined variable path")) {
        log.debug(
            "Selector '{}' references 'path' variable which is not available at repository level for repository '{}'. "
                + "Deferring evaluation to asset level where path information is available.",
            selector.getName(), repository.getName());
        return true; // Allow at repository level, will be properly evaluated at asset level with path variable
      }

      log.warn("Failed to evaluate selector '{}' for repository '{}'", selector.getName(), repository.getName(), e);
      return false;
    }
  }
}
