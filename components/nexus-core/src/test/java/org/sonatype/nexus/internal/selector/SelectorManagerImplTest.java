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
package org.sonatype.nexus.internal.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import javax.cache.configuration.Factory;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor.P_REPOSITORY;
import static org.sonatype.nexus.repository.security.RepositorySelector.ALL;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

public class SelectorManagerImplTest
    extends TestSupport
{
  @Mock
  private SelectorConfigurationStore store;

  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private SelectorFactory selectorFactory;

  @Mock
  private AuthorizationManager authorizationManager;

  @Mock
  private User user;

  @Mock
  private VariableSource variableSource;

  @Mock
  private CacheHelper cacheHelper;

  @Mock
  private Time userCacheTimeout;

  @Mock
  private Subject subject;

  @Mock
  private Cache<Object, Object> userCache;

  private SelectorManagerImpl manager;

  private List<SelectorConfiguration> selectorConfigurations;

  private final List<Privilege> privileges = new ArrayList<>();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    this.manager = new SelectorManagerImpl(store, securitySystem, selectorFactory, cacheHelper, userCacheTimeout);

    when(securitySystem.getAuthorizationManager(DEFAULT_SOURCE)).thenReturn(authorizationManager);
    when(securitySystem.listRoles(UserManager.DEFAULT_SOURCE)).thenReturn(new HashSet<>());
    when(securitySystem.getSubject()).thenReturn(subject);

    when(subject.isAuthenticated()).thenReturn(true);
    when(subject.getPrincipal()).thenReturn("user");
    when(subject.getPrincipals()).thenReturn(new SimplePrincipalCollection("user", "default"));

    when(cacheHelper.maybeCreateCache(anyString(), any(Factory.class))).thenReturn(userCache);

    when(userCache.get(any())).thenReturn(user);

    selectorConfigurations = new ArrayList<>();

    when(store.browse()).thenReturn(selectorConfigurations);

    Selector alwaysTrueSelector = mock(Selector.class);
    when(alwaysTrueSelector.evaluate(variableSource)).thenReturn(true);
    Selector alwaysFalseSelector = mock(Selector.class);
    when(alwaysFalseSelector.evaluate(variableSource)).thenReturn(false);

    when(selectorFactory.createSelector(JexlSelector.TYPE, "true")).thenReturn(alwaysTrueSelector);
    when(selectorFactory.createSelector(JexlSelector.TYPE, "false")).thenReturn(alwaysFalseSelector);
  }

  @Test
  public void testEvaluate_True() throws Exception {
    SelectorConfiguration selectorConfiguration = getSelectorConfiguration(JexlSelector.TYPE, "true");
    assertThat(manager.evaluate(selectorConfiguration, variableSource), is(true));
  }

  @Test
  public void testEvaluate_False() throws Exception {
    SelectorConfiguration selectorConfiguration = getSelectorConfiguration(JexlSelector.TYPE, "false");
    assertThat(manager.evaluate(selectorConfiguration, variableSource), is(false));
  }

  @Test(expected = SelectorEvaluationException.class)
  public void testEvaluate_InvalidSelectorType() throws Exception {
    SelectorConfiguration selectorConfiguration = getSelectorConfiguration("junk", "");
    manager.evaluate(selectorConfiguration, variableSource);
  }

  @Test
  public void testBrowseJexl() {
    List<SelectorConfiguration> configs = asList(getSelectorConfiguration(JexlSelector.TYPE, "true"),
        getSelectorConfiguration(CselSelector.TYPE, "bar"));
    selectorConfigurations.addAll(configs);
    assertThat(manager.browseJexl(), is(asList(getSelectorConfiguration(JexlSelector.TYPE, "true"))));
  }

  @Test
  public void browseActiveReturnsAllContentSelectorsForMatchingNestedRoles() throws Exception {
    createSelectorConfiguration("roleId", "rolePrivilegeId", "roleSelectorName", "repository");
    securitySystem.listRoles(UserManager.DEFAULT_SOURCE).stream().findFirst().get().getRoles().add("nestedRoleId");
    SelectorConfiguration nestedRoleConfig = createSelectorConfiguration("nestedRoleId", "nestedRolePrivilegeId",
        "nestedRoleSelectorName", ALL);
    when(user.getRoles()).thenReturn(newHashSet(new RoleIdentifier("", "roleId")));
    when(authorizationManager.getPrivileges(ImmutableSet.of("rolePrivilegeId", "nestedRolePrivilegeId")))
        .thenReturn(privileges);

    List<SelectorConfiguration> selectors = manager.browseActive(asList("anyRepository"), asList("anyFormat"));

    assertThat(selectors, contains(is(nestedRoleConfig)));
  }

  @Test
  public void browseActiveCachesRolesList()  throws Exception{
    // initial setup calls securitySystem.listRoles twice times
    createSelectorConfiguration("roleId", "rolePrivilegeId", "roleSelectorName", "repository");
    securitySystem.listRoles(UserManager.DEFAULT_SOURCE).stream().findFirst().get().getRoles().add("nestedRoleId");
    verify(securitySystem, times(2)).listRoles(UserManager.DEFAULT_SOURCE);

    manager.browseActive(asList("anyRepository"), asList("anyFormat"));
    verify(securitySystem, times(3)).listRoles(UserManager.DEFAULT_SOURCE);

    manager.browseActive(asList("anyRepository"), asList("anyFormat"));
    verify(securitySystem, times(3)).listRoles(UserManager.DEFAULT_SOURCE);

    // configuration change clears cache
    createSelectorConfiguration("nestedRoleId", "nestedRolePrivilegeId",
        "nestedRoleSelectorName", ALL);
    manager.browseActive(asList("anyRepository"), asList("anyFormat"));
    verify(securitySystem, times(4)).listRoles(UserManager.DEFAULT_SOURCE);

    manager.browseActive(asList("anyRepository"), asList("anyFormat"));
    verify(securitySystem, times(4)).listRoles(UserManager.DEFAULT_SOURCE);
  }

  @Test
  public void browseActiveReturnsNoContentSelectorsForNonMatchingFormats() throws Exception {
    String repositoryFormat = "format";
    createSelectorConfiguration("roleId", "rolePrivilegeId", "roleSelectorName", ALL + '-' + repositoryFormat);
    createSelectorConfiguration("nestedRoleId", "nestedRolePrivilegeId", "nestedRoleSelectorName",
        ALL + '-' + repositoryFormat);
    when(user.getRoles()).thenReturn(newHashSet(new RoleIdentifier("", "roleId")));
    when(authorizationManager.getPrivileges(ImmutableSet.of("rolePrivilegeId", "nestedRolePrivilegeId")))
        .thenReturn(privileges);

    List<SelectorConfiguration> selectors = manager.browseActive(asList("repository"), asList("unknownFormat"));

    assertThat(selectors.size(), is(0));
  }

  @Test
  public void browseActiveReturnsRepositorySpecificContentSelectorsForMatchingNestedRoles() throws Exception {
    String repositoryName = "repository";
    String repositoryFormat = "format";
    SelectorConfiguration roleConfig = createSelectorConfiguration("roleId", "rolePrivilegeId",
        "roleSelectorName", repositoryName);
    securitySystem.listRoles(UserManager.DEFAULT_SOURCE).stream().findFirst().get().getRoles().add("nestedRoleId");
    SelectorConfiguration nestedRoleConfig = createSelectorConfiguration("nestedRoleId",
        "nestedRolePrivilegeId", "nestedRoleSelectorName", repositoryName);
    when(user.getRoles()).thenReturn(newHashSet(new RoleIdentifier("", "roleId")));
    when(authorizationManager.getPrivileges(ImmutableSet.of("rolePrivilegeId", "nestedRolePrivilegeId")))
        .thenReturn(privileges);

    List<SelectorConfiguration> selectors = manager.browseActive(asList(repositoryName), asList(repositoryFormat));

    assertThat(selectors, containsInAnyOrder(is(roleConfig), is(nestedRoleConfig)));
  }

  @Test
  public void browseActiveReturnsNoContentSelectorsWhenAnonymousAccessDisabled() throws Exception {
    when(securitySystem.currentUser()).thenReturn(null);

    List<SelectorConfiguration> selectors = manager.browseActive(asList("anyRepository"), asList("anyFormat"));

    assertThat(selectors.size(), is(0));
  }

  @Test
  public void testDelete_Succeeds() {
    SelectorConfiguration selectorConfiguration = new SelectorConfigurationData();
    selectorConfiguration.setName("selector");
    selectorConfiguration.setType(CselSelector.TYPE);

    manager.create(selectorConfiguration);
    verify(store).create(selectorConfiguration);

    manager.delete(selectorConfiguration);
    verify(store).delete(selectorConfiguration);
  }

  @Test
  public void testDelete_FailsWhenContentSelectorIsUsedByPrivilege() throws Exception {
    SelectorConfiguration selector = createSelectorConfiguration("role", "privilege", "selector", "repository");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Content selector selector is in use and cannot be deleted");

    manager.delete(selector);

    verifyNoInteractions(store);
  }

  @Test
  public void findByNameReturnsEmptyOptional() throws Exception {
    Optional<SelectorConfiguration> configuration = manager.findByName("invalidSelectorName");
    assertThat(configuration.isPresent(), is(false));
  }

  @Test
  public void findByNameReturnsExpectedSelector() throws Exception {
    SelectorConfiguration expectedSelector = getSelectorConfiguration(CselSelector.TYPE, "bar");
    List<SelectorConfiguration> configs = asList(expectedSelector);
    selectorConfigurations.addAll(configs);

    Optional<SelectorConfiguration> configuration = manager.findByName(expectedSelector.getName());

    assertThat(configuration.get(), is(expectedSelector));
  }

  @Test
  public void testUserIsTakenFromCache() throws UserNotFoundException {
    manager.browseActive(null, null);

    verify(userCache, atLeastOnce()).get(any());
    verify(securitySystem, never()).currentUser();
  }

  @Test
  public void testUserIsTakenFromSystemIfNoValueInCache() throws UserNotFoundException {
    when(userCache.get(any())).thenReturn(null);
    when(securitySystem.currentUser()).thenReturn(user);

    manager.browseActive(null, null);

    verify(securitySystem, atMostOnce()).currentUser();
    verify(userCache, atLeastOnce()).put(any(), any());
  }

  @Test
  public void testAnonymousUserIsTakenFromSystemIfNoValueInCache() throws UserNotFoundException {
    when(subject.isAuthenticated()).thenReturn(false);
    when(subject.getPrincipals()).thenReturn(new AnonymousPrincipalCollection("anonymous", "default"));
    when(userCache.get(any())).thenReturn(null);
    when(securitySystem.currentUser()).thenReturn(user);

    manager.browseActive(null, null);

    verify(securitySystem, atMostOnce()).currentUser();
    verify(userCache, atLeastOnce()).put(any(), any());
  }

  private SelectorConfiguration getSelectorConfiguration(final String type, final String expression) {
    SelectorConfiguration selectorConfiguration = new SelectorConfigurationData();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("expression", expression);
    selectorConfiguration.setAttributes(attributes);
    selectorConfiguration.setType(type);
    return selectorConfiguration;
  }

  private SelectorConfiguration createSelectorConfiguration(
      final String roleId,
      final String rolePrivilegeId,
      final String roleSelectorName,
      final String repositoryName) throws Exception
  {
    createRole(roleId, rolePrivilegeId);
    createRepositoryContentSelectorPrivilege(rolePrivilegeId, roleSelectorName, repositoryName);

    SelectorConfiguration selectorConfiguration = new SelectorConfigurationData();
    selectorConfiguration.setName(roleSelectorName);

    selectorConfigurations.add(selectorConfiguration);

    return selectorConfiguration;
  }

  private void createRepositoryContentSelectorPrivilege(final String privilegeId,
                                                        final String selectorConfigurationName,
                                                        final String repositoryName)
  {
    Privilege privilege = new Privilege();
    privilege.setId(privilegeId);
    privilege.setType(RepositoryContentSelectorPrivilegeDescriptor.TYPE);
    privilege.getProperties()
        .put(RepositoryContentSelectorPrivilegeDescriptor.P_CONTENT_SELECTOR, selectorConfigurationName);
    privilege.getProperties().put(P_REPOSITORY, repositoryName);
    privileges.add(privilege);

    when(securitySystem.listPrivileges()).thenReturn(Collections.singleton(privilege));
  }

  private Role createRole(final String roleId, final String privilegeId) throws Exception {
    Role role = new Role();
    role.setRoleId(roleId);
    role.getPrivileges().add(privilegeId);

    securitySystem.listRoles(UserManager.DEFAULT_SOURCE).add(role);
    when(authorizationManager.getRole(roleId)).thenReturn(role);

    return role;
  }
}
