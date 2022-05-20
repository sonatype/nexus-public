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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.Iterables;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * Repository permission checker.
 *
 * @since 3.10
 */
@Named
@Singleton
public class RepositoryPermissionChecker
    extends ComponentSupport
{
  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  private final Map<String, Recipe> recipes;

  @Inject
  public RepositoryPermissionChecker(
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager,
      final Map<String, Recipe> recipes)
  {
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
    this.recipes = checkNotNull(recipes);
  }

  /**
   * WARNING: This method should _only_ be used to check a single repository to prevent performance problems with large
   * numbers of content selectors. Use userCanBrowseRepositories instead to check multiple repositories.
   *
   * @return true if the user can browse or read the repository or if the user has a content selector granting access
   */
  public boolean userCanReadOrBrowse(final Repository repository) {
    return userHasRepositoryViewPermissionTo(repository, BROWSE, READ) || userHasAnyContentSelectorAccessTo(repository, BROWSE, READ);
  }

  /**
   * @param repository
   * @return true if user can delete anything within the repository based on repository or content selector privilege
   *
   * @since 3.15
   */
  public boolean userCanDeleteInRepository(final Repository repository) {
    return userHasRepositoryViewPermissionTo(DELETE, repository) || userHasAnyContentSelectorAccessTo(repository, DELETE);
  }

  private boolean userHasRepositoryViewPermissionTo(final Repository repository, final String... actions) {
    return securityHelper.anyPermitted(permissionsFor(repository, actions));
  }

  private boolean userHasRepositoryViewPermissionTo(final String action, final Repository repository) {
    return securityHelper.anyPermitted(new RepositoryViewPermission(repository, action));
  }

  private static Permission[] permissionsFor(final Repository repository, final String... actions) {
    return Arrays.stream(actions).map(action -> new RepositoryViewPermission(repository, action)).toArray(Permission[]::new);
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
    List<Configuration> filteredRepositories = new ArrayList<>(Arrays.asList(repositories));
    List<Configuration> permittedRepositories =
        userHasPermission(c -> new RepositoryViewPermission(toFormat(c), c.getRepositoryName(), BROWSE), repositories);
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
    Configuration[] repos = Iterables.toArray(configurations, Configuration.class);
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
    securityHelper.ensurePermitted(new RepositoryAdminPermission(repository.getFormat().getValue(), repository.getName(), singletonList(action)));
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

  private List<Repository> subjectHasAnyContentSelectorAccessTo(final Subject subject,
                                                                final List<Repository> repositories)
  {
    List<String> repositoryNames = repositories.stream().map(r -> r.getName()).collect(Collectors.toList());
    List<String> formats = repositories.stream().map(r -> r.getFormat().getValue()).distinct()
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
    List<String> repositoryNames = configurations.stream()
        .map(Configuration::getRepositoryName)
        .collect(Collectors.toList());
    List<String> formats = configurations.stream()
        .map(this::toFormat)
        .distinct()
        .collect(Collectors.toList());
    List<SelectorConfiguration> selectors = selectorManager.browseActive(repositoryNames, formats);

    if (selectors.isEmpty()) {
      return Collections.emptyList();
    }

    List<Configuration> permittedRepositories = new ArrayList<>();
    for (Configuration configuration : configurations) {
      Permission[] permissions = selectors.stream()
          .map(s -> new RepositoryContentSelectorPermission(s.getName(), toFormat(configuration),
              configuration.getRepositoryName(), singletonList(BROWSE)))
          .toArray(Permission[]::new);
      if (securityHelper.anyPermitted(subject, permissions)) {
        permittedRepositories.add(configuration);
      }
    }

    return permittedRepositories;
  }

  private String toFormat(final Configuration configuration) {
    return Optional.ofNullable(recipes.get(configuration.getRecipeName()))
        .map(Recipe::getFormat)
        .map(Format::getValue)
        .orElseThrow(() -> new IllegalArgumentException("Unknown repository type: " + configuration.getRecipeName()));
  }

  private boolean userHasAnyContentSelectorAccessTo(final Repository repository, final String... actions) {
    Subject subject = securityHelper.subject(); //Getting the subject a single time improves performance
    return selectorManager.browse().stream().anyMatch(selector -> securityHelper.anyPermitted(subject,
        Arrays.stream(actions)
            .map(action -> new RepositoryContentSelectorPermission(selector, repository, singletonList(action)))
            .toArray(Permission[]::new)));
  }
}
