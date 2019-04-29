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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoutingRulesResourceTest
  extends TestSupport
{
  private RoutingRulesResource underTest;

  @Mock
  RoutingRuleStore routingRuleStore;

  @Mock
  RoutingRuleHelper routingRuleHelper;

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker;

  private RoutingRule rule1 = routingRule("rule1");

  private RoutingRule rule2 = routingRule("rule2");

  private RoutingRule rule3 = routingRule("rule3");

  private Repository repository1 = repository("repository1");

  private Repository repository2 = repository("repository2");

  private Repository repository3 = repository("repository3");

  @Before
  public void setup() {
    underTest = new RoutingRulesResource(routingRuleStore, routingRuleHelper, repositoryPermissionChecker);
  }

  @Test
  public void testGetRoutingRules_AssignedRepositories() {
    when(routingRuleStore.list()).thenReturn(Arrays.asList(rule1, rule2, rule3));

    Map<EntityId,List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), Collections.singletonList(repository1));
    ruleRepoMap.put(new DetachedEntityId("rule2"), Collections.singletonList(repository2));
    ruleRepoMap.put(new DetachedEntityId("rule3"), Collections.singletonList(repository3));

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(Arrays.asList(repository1), "read"))
        .thenReturn(Arrays.asList(repository1));

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(Arrays.asList(repository2), "read"))
        .thenReturn(Arrays.asList(repository2));

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(Arrays.asList(repository3), "read"))
        .thenReturn(Arrays.asList(repository3));

    when(repositoryPermissionChecker.userCanBrowseRepository(any())).thenReturn(true);

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 1, "repository1");
    assertXO(xos.get(1), "rule2", 1, "repository2");
    assertXO(xos.get(2), "rule3", 1, "repository3");
  }

  @Test
  public void testGetRoutingRules_NoAssignedRepositories() {
    when(routingRuleStore.list()).thenReturn(Arrays.asList(rule1, rule2, rule3));

    Map<EntityId,List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), Collections.emptyList());
    ruleRepoMap.put(new DetachedEntityId("rule2"), Collections.emptyList());
    ruleRepoMap.put(new DetachedEntityId("rule3"), Collections.emptyList());

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 0);
    assertXO(xos.get(1), "rule2", 0);
    assertXO(xos.get(2), "rule3", 0);
  }

  @Test
  public void testGetRoutingRules_AssignedRepositoriesHiddenByPerms() {
    when(routingRuleStore.list()).thenReturn(Arrays.asList(rule1, rule2, rule3));

    Map<EntityId,List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), Collections.singletonList(repository1));
    ruleRepoMap.put(new DetachedEntityId("rule2"), Collections.singletonList(repository2));
    ruleRepoMap.put(new DetachedEntityId("rule3"), Collections.singletonList(repository3));

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(Arrays.asList(repository1), "read"))
        .thenReturn(Arrays.asList(repository1));

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(Arrays.asList(repository2), "read"))
        .thenReturn(Collections.emptyList());

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(Arrays.asList(repository3), "read"))
        .thenReturn(Arrays.asList(repository3));

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 1, "repository1");
    assertXO(xos.get(1), "rule2", 1);
    assertXO(xos.get(2), "rule3", 1, "repository3");
  }

  @Test
  public void testGetRoutingRules_AssignedRepositoriesMultipleHiddenByPerms() {
    when(routingRuleStore.list()).thenReturn(Arrays.asList(rule1, rule2, rule3));

    Map<EntityId,List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), Arrays.asList(repository1, repository2, repository3));
    ruleRepoMap.put(new DetachedEntityId("rule2"), Collections.emptyList());
    ruleRepoMap.put(new DetachedEntityId("rule3"), Collections.emptyList());

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    when(repositoryPermissionChecker
        .userHasRepositoryAdminPermission(Arrays.asList(repository1, repository2, repository3), "read"))
        .thenReturn(Arrays.asList(repository1, repository3));

    when(repositoryPermissionChecker.userCanBrowseRepository(repository1)).thenReturn(true);
    when(repositoryPermissionChecker.userCanBrowseRepository(repository2)).thenReturn(false);
    when(repositoryPermissionChecker.userCanBrowseRepository(repository3)).thenReturn(true);

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 3, "repository1", "repository3");
    assertXO(xos.get(1), "rule2", 0);
    assertXO(xos.get(2), "rule3", 0);
  }

  private RoutingRule routingRule(String name) {
    RoutingRule routingRule = new RoutingRule().name(name).mode(RoutingMode.ALLOW).matchers(Collections.singletonList(".*"));

    EntityMetadata entityMetadata = mock(EntityMetadata.class);
    when(entityMetadata.getId()).thenReturn(new DetachedEntityId(name));

    routingRule.setEntityMetadata(entityMetadata);

    return routingRule;
  }

  private Repository repository(String name) {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);
    return repository;
  }

  private void assertXO(final RoutingRuleXO xo, final String name, final int count, final String... repositoryNames) {
    assertThat(xo.getName(), is(name));
    assertThat(xo.getId(), is(name));
    assertThat(xo.getAssignedRepositoryCount(), is(count));
    if (repositoryNames == null || repositoryNames.length == 0) {
      assertThat(xo.getAssignedRepositoryNames(), is(Collections.emptyList()));
    }
    else {
      assertThat(xo.getAssignedRepositoryNames(), is(Arrays.asList(repositoryNames)));
    }
  }
}
