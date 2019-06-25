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
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.VariableSource;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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

  private SelectorManagerImpl manager;

  private List<SelectorConfiguration> selectorConfigurations;

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    this.manager = new SelectorManagerImpl(store, securitySystem, selectorFactory);

    when(securitySystem.getAuthorizationManager(DEFAULT_SOURCE)).thenReturn(authorizationManager);
    when(securitySystem.currentUser()).thenReturn(user);

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
    SelectorConfiguration nestedRoleConfig = createSelectorConfiguration("nestedRoleId", "nestedRolePrivilegeId",
        "nestedRoleSelectorName", ALL);
    authorizationManager.getRole("roleId").getRoles().add("nestedRoleId");
    when(user.getRoles()).thenReturn(newHashSet(new RoleIdentifier("", "roleId")));

    List<SelectorConfiguration> selectors = manager.browseActive(asList("anyRepository"), asList("anyFormat"));

    assertThat(selectors, contains(is(nestedRoleConfig)));
  }

  @Test
  public void browseActiveReturnsNoContentSelectorsForNonMatchingFormats() throws Exception {
    String repositoryFormat = "format";
    createSelectorConfiguration("roleId", "rolePrivilegeId", "roleSelectorName", ALL + '-' + repositoryFormat);
    createSelectorConfiguration("nestedRoleId", "nestedRolePrivilegeId", "nestedRoleSelectorName",
        ALL + '-' + repositoryFormat);
    authorizationManager.getRole("roleId").getRoles().add("nestedRoleId");
    when(user.getRoles()).thenReturn(newHashSet(new RoleIdentifier("", "roleId")));

    List<SelectorConfiguration> selectors = manager.browseActive(asList("repository"), asList("unknownFormat"));

    assertThat(selectors.size(), is(0));
  }

  @Test
  public void browseActiveReturnsRepositorySpecificContentSelectorsForMatchingNestedRoles() throws Exception {
    String repositoryName = "repository";
    String repositoryFormat = "format";
    SelectorConfiguration roleConfig = createSelectorConfiguration("roleId", "rolePrivilegeId",
        "roleSelectorName", repositoryName);
    SelectorConfiguration nestedRoleConfig = createSelectorConfiguration("nestedRoleId",
        "nestedRolePrivilegeId", "nestedRoleSelectorName", repositoryName);
    authorizationManager.getRole("roleId").getRoles().add("nestedRoleId");
    when(user.getRoles()).thenReturn(newHashSet(new RoleIdentifier("", "roleId")));

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
    SelectorConfiguration selectorConfiguration = new SelectorConfiguration();
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

    verifyZeroInteractions(store);
  }

  private SelectorConfiguration getSelectorConfiguration(final String type, final String expression) {
    SelectorConfiguration selectorConfiguration = new SelectorConfiguration();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("expression", expression);
    selectorConfiguration.setAttributes(attributes);
    selectorConfiguration.setType(type);
    return selectorConfiguration;
  }

  private SelectorConfiguration createSelectorConfiguration(String roleId,
                                                            String rolePrivilegeId,
                                                            String roleSelectorName,
                                                            String repositoryName) throws Exception
  {
    createRole(roleId, rolePrivilegeId);
    createRepositoryContentSelectorPrivilege(rolePrivilegeId, roleSelectorName, repositoryName);

    SelectorConfiguration selectorConfiguration = new SelectorConfiguration();
    selectorConfiguration.setName(roleSelectorName);

    selectorConfigurations.add(selectorConfiguration);

    return selectorConfiguration;
  }

  private Privilege createRepositoryContentSelectorPrivilege(String privilegeId,
                                                             String selectorConfigurationName,
                                                             String repositoryName)
      throws Exception
  {
    Privilege privilege = new Privilege();
    privilege.setId(privilegeId);
    privilege.setType(RepositoryContentSelectorPrivilegeDescriptor.TYPE);
    privilege.getProperties()
        .put(RepositoryContentSelectorPrivilegeDescriptor.P_CONTENT_SELECTOR, selectorConfigurationName);
    privilege.getProperties().put(P_REPOSITORY, repositoryName);

    when(authorizationManager.getPrivilege(privilegeId)).thenReturn(privilege);
    when(securitySystem.listPrivileges()).thenReturn(Collections.singleton(privilege));

    return privilege;
  }

  private Role createRole(String roleId, String privilegeId) throws Exception {
    Role role = new Role();
    role.setRoleId(roleId);
    role.getPrivileges().add(privilegeId);

    when(authorizationManager.getRole(roleId)).thenReturn(role);

    return role;
  }
}
