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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.privilege.ApplicationPermission;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.security.BreadActions.READ;

public class RepositoryPermissionCheckerTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repositoryName";

  private static final String REPOSITORY_NAME_1 = "repositoryName1";

  private static final String REPOSITORY_NAME_2 = "repositoryName2";

  private static final String REPOSITORY_FORMAT = "maven2";

  private static final String SELECTOR_NAME = "theSelector";

  private static final String RECIPE_NAME = "maven2-proxy";

  private static final boolean HAS_REPOSITORY_PERMISSION = true;

  private static final boolean HAS_SELECTOR_PERMISSION = true;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private Configuration configuration;

  @Mock
  private Configuration configuration1;

  @Mock
  private Configuration configuration2;

  @Mock
  private Repository repository;

  @Mock
  private Repository repository1;

  @Mock
  private Repository repository2;

  @Mock
  private Recipe recipe;

  @Mock
  private Format format;

  @Mock
  private SelectorConfiguration selector;

  @Mock
  private Subject subject;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  private RepositoryPermissionChecker underTest;

  @Before
  public void setup() {
    when(recipe.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn(REPOSITORY_FORMAT);

    when(configuration.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(configuration.getRecipeName()).thenReturn(RECIPE_NAME);
    when(configuration1.getRepositoryName()).thenReturn(REPOSITORY_NAME_1);
    when(configuration1.getRecipeName()).thenReturn(RECIPE_NAME);
    when(configuration2.getRepositoryName()).thenReturn(REPOSITORY_NAME_2);
    when(configuration2.getRecipeName()).thenReturn(RECIPE_NAME);

    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(repository1.getName()).thenReturn(REPOSITORY_NAME_1);
    when(repository1.getFormat()).thenReturn(format);
    when(repository2.getName()).thenReturn(REPOSITORY_NAME_2);
    when(repository2.getFormat()).thenReturn(format);

    when(selector.getName()).thenReturn(SELECTOR_NAME);
    when(selectorManager.browse()).thenReturn(asList(selector));
    when(selectorManager.browseActive(Arrays.asList(REPOSITORY_NAME_1, REPOSITORY_NAME_2),
        Collections.singletonList(REPOSITORY_FORMAT))).thenReturn(asList(selector));

    when(securityHelper.isPermitted(same(subject), ArgumentMatchers.<Permission>any()))
        .thenReturn(new boolean[] { true, false, false });
    when(securityHelper.subject()).thenReturn(subject);

    underTest =
        new RepositoryPermissionChecker(securityHelper, selectorManager, Collections.singletonMap(RECIPE_NAME, recipe));
  }

  @Test
  public void testUserCanBrowseRepository() {
    verifyUserAccessOf(underTest::userCanReadOrBrowse);
  }

  @Test
  public void testUserCanBrowseRepositories() {
    when(securityHelper.anyPermitted(eq(subject), any(RepositoryContentSelectorPermission.class))).then(i -> {
      RepositoryContentSelectorPermission p = (RepositoryContentSelectorPermission) i.getArguments()[1];
      return REPOSITORY_NAME_2.equals(p.getName());
    });
    List<Repository> permittedRepositories = underTest.userCanBrowseRepositories(repository, repository1, repository2);

    assertThat(permittedRepositories, contains(repository, repository2));

    // Iterable version
    permittedRepositories = underTest.userCanBrowseRepositories(Arrays.asList(repository, repository1, repository2));

    assertThat(permittedRepositories, contains(repository, repository2));
  }

  @Test
  public void testUserCanBrowseRepositories_byConfigurations() {
    when(securityHelper.anyPermitted(eq(subject), any(RepositoryContentSelectorPermission.class))).then(i -> {
      RepositoryContentSelectorPermission p = (RepositoryContentSelectorPermission) i.getArguments()[1];
      return REPOSITORY_NAME_2.equals(p.getName());
    });
    List<Configuration> permittedConfigurations =
        underTest.userCanBrowseRepositories(configuration, configuration1, configuration2);

    assertThat(permittedConfigurations, contains(configuration, configuration2));
  }

  @Test
  public void testUserHasRepositoryAdminPermission() {
    List<Repository> permittedRepositories =
        underTest.userHasRepositoryAdminPermission(Arrays.asList(repository, repository1, repository2), READ);

    assertThat(permittedRepositories, contains(repository));

    verify(securityHelper).isPermitted(subject,
        createAdminPermissions(READ, RepositoryAdminPermission::new, repository, repository1, repository2));
  }

  @Test
  public void testUserHasRepositoryAdminPermissionFor() {
    List<Configuration> permittedRepositories =
        underTest.userHasRepositoryAdminPermissionFor(Arrays.asList(configuration, configuration1, configuration2), READ);

    assertThat(permittedRepositories, contains(configuration));

    verify(securityHelper).isPermitted(subject,
        createAdminPermissions(READ, RepositoryAdminPermission::new, repository, repository1, repository2));
  }

  @Test
  public void testEnsureUserHasAnyPermissionOrAdminAccess() {
    Permission[] repositoryPermissions =
        createAdminPermissions(READ, RepositoryAdminPermission::new, repository, repository1, repository2);
    ApplicationPermission appPerm = new ApplicationPermission("blobstores", READ);
    Iterable<Permission> appPermissions = singletonList(appPerm);
    Iterable<Repository> repositories = Arrays.asList(repository, repository1, repository2);

    when(securityHelper.anyPermitted(same(subject), eq(appPermissions))).thenReturn(true);
    underTest.ensureUserHasAnyPermissionOrAdminAccess(appPermissions, READ, repositories);
    verify(securityHelper, never()).ensureAnyPermitted(subject, repositoryPermissions);

    Iterable<Permission> multipleAppPermissions = Arrays
        .asList(appPerm, new ApplicationPermission("blobstores", DELETE));
    when(securityHelper.anyPermitted(same(subject), eq(multipleAppPermissions))).thenReturn(true);
    underTest.ensureUserHasAnyPermissionOrAdminAccess(multipleAppPermissions, READ, repositories);
    verify(securityHelper, never()).ensureAnyPermitted(subject, repositoryPermissions);

    when(securityHelper.anyPermitted(same(subject), eq(appPermissions))).thenReturn(false);
    underTest.ensureUserHasAnyPermissionOrAdminAccess(appPermissions, READ, repositories);
    verify(securityHelper).ensureAnyPermitted(subject, repositoryPermissions);
  }

  private Permission[] createAdminPermissions(
      final String action,
      final BiFunction<Repository, String[], Permission> constructor,
      final Repository... repositories)
  {
    List<Permission> permissions = new ArrayList<>();
    for (Repository repository : repositories) {
      permissions.add(constructor.apply(repository, new String[]{action}));
    }
    return permissions.toArray(new Permission[permissions.size()]);
  }

  private void verifyUserAccessOf(final Function<Repository, Boolean> accessCheck)
  {
    BiFunction<Boolean, Boolean, Boolean> userCanAccessRepositoryWhen =
        (hasRepositoryPermission, hasSelectorPermission) -> {
          setUpRepositoryPermission(hasRepositoryPermission);
          setUpSelectorPermission(hasSelectorPermission);
          return accessCheck.apply(repository);
        };

    assertTrue(userCanAccessRepositoryWhen.apply(HAS_REPOSITORY_PERMISSION, !HAS_SELECTOR_PERMISSION));
    assertTrue(userCanAccessRepositoryWhen.apply(!HAS_REPOSITORY_PERMISSION, HAS_SELECTOR_PERMISSION));
    assertTrue(userCanAccessRepositoryWhen.apply(HAS_REPOSITORY_PERMISSION, HAS_SELECTOR_PERMISSION));
    assertFalse(userCanAccessRepositoryWhen.apply(!HAS_REPOSITORY_PERMISSION, !HAS_SELECTOR_PERMISSION));
  }

  private void setUpRepositoryPermission(final boolean hasPermission) {
    Permission[] permissions = new Permission[] {new RepositoryViewPermission(REPOSITORY_FORMAT, REPOSITORY_NAME, BROWSE), new RepositoryViewPermission(REPOSITORY_FORMAT, REPOSITORY_NAME, READ)};
    when(securityHelper.anyPermitted(permissions))
        .thenReturn(hasPermission);
  }

  private void setUpSelectorPermission(final boolean hasPermission) {
    Permission[] permissions = new Permission[] { new RepositoryContentSelectorPermission(SELECTOR_NAME, REPOSITORY_FORMAT, REPOSITORY_NAME,
        ImmutableList.of(BROWSE)), new RepositoryContentSelectorPermission(SELECTOR_NAME, REPOSITORY_FORMAT, REPOSITORY_NAME,
        ImmutableList.of(READ))};
    when(securityHelper
        .anyPermitted(subject, permissions))
        .thenReturn(hasPermission);
  }
}
