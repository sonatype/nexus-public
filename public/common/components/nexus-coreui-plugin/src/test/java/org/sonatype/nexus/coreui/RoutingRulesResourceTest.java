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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.RoutingRulePreviewXO;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleData;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class RoutingRulesResourceTest
{
  @InjectMocks
  private RoutingRulesResource underTest;

  @Mock
  RoutingRuleStore routingRuleStore;

  @Mock
  RoutingRuleHelper routingRuleHelper;

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  RepositoryManager repositoryManager;

  private RoutingRule rule1 = routingRule("rule1");

  private RoutingRule rule2 = routingRule("rule2");

  private RoutingRule rule3 = routingRule("rule3");

  private Repository repository1 = repository("repository1");

  private Repository repository2 = repository("repository2");

  private Repository repository3 = repository("repository3");

  @Test
  void testGetRoutingRules_AssignedRepositories() {
    when(routingRuleStore.list()).thenReturn(List.of(rule1, rule2, rule3));

    Map<EntityId, List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), List.of(repository1));
    ruleRepoMap.put(new DetachedEntityId("rule2"), List.of(repository2));
    ruleRepoMap.put(new DetachedEntityId("rule3"), List.of(repository3));

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(List.of(repository1), "read"))
        .thenReturn(List.of(repository1));

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(List.of(repository2), "read"))
        .thenReturn(List.of(repository2));

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(List.of(repository3), "read"))
        .thenReturn(List.of(repository3));

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 1, "repository1");
    assertXO(xos.get(1), "rule2", 1, "repository2");
    assertXO(xos.get(2), "rule3", 1, "repository3");
  }

  @Test
  void testGetRoutingRules_NoAssignedRepositories() {
    when(routingRuleStore.list()).thenReturn(List.of(rule1, rule2, rule3));

    Map<EntityId, List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), List.of());
    ruleRepoMap.put(new DetachedEntityId("rule2"), List.of());
    ruleRepoMap.put(new DetachedEntityId("rule3"), List.of());

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 0);
    assertXO(xos.get(1), "rule2", 0);
    assertXO(xos.get(2), "rule3", 0);
  }

  @Test
  void testGetRoutingRules_AssignedRepositoriesHiddenByPerms() {
    when(routingRuleStore.list()).thenReturn(List.of(rule1, rule2, rule3));

    Map<EntityId, List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), List.of(repository1));
    ruleRepoMap.put(new DetachedEntityId("rule2"), List.of(repository2));
    ruleRepoMap.put(new DetachedEntityId("rule3"), List.of(repository3));

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(List.of(repository1), "read"))
        .thenReturn(List.of(repository1));

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(List.of(repository2), "read"))
        .thenReturn(List.of());

    when(repositoryPermissionChecker.userHasRepositoryAdminPermission(List.of(repository3), "read"))
        .thenReturn(List.of(repository3));

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 1, "repository1");
    assertXO(xos.get(1), "rule2", 1);
    assertXO(xos.get(2), "rule3", 1, "repository3");
  }

  @Test
  void testGetRoutingRules_AssignedRepositoriesMultipleHiddenByPerms() {
    when(routingRuleStore.list()).thenReturn(List.of(rule1, rule2, rule3));

    Map<EntityId, List<Repository>> ruleRepoMap = new HashMap<>();
    ruleRepoMap.put(new DetachedEntityId("rule1"), List.of(repository1, repository2, repository3));
    ruleRepoMap.put(new DetachedEntityId("rule2"), List.of());
    ruleRepoMap.put(new DetachedEntityId("rule3"), List.of());

    when(routingRuleHelper.calculateAssignedRepositories()).thenReturn(ruleRepoMap);

    when(repositoryPermissionChecker
        .userHasRepositoryAdminPermission(List.of(repository1, repository2, repository3), "read"))
            .thenReturn(List.of(repository1, repository3));

    List<RoutingRuleXO> xos = underTest.getRoutingRules(true);

    assertThat(xos.size(), is(3));
    assertXO(xos.get(0), "rule1", 3, "repository1", "repository3");
    assertXO(xos.get(1), "rule2", 0);
    assertXO(xos.get(2), "rule3", 0);
  }

  @Test
  void testGetRoutingRulesPreview_AllHandlesMissingGroupRepositories() {
    Repository proxyRepository = repository("repository1", new ProxyType());

    when(repositoryManager.browse()).thenReturn(List.of(proxyRepository));
    when(routingRuleStore.list()).thenReturn(List.of());

    RoutingRulePreviewXO preview = underTest.getRoutingRulesPreview("*", "all");

    assertThat(preview.getChildren().size(), is(1));
    assertThat(preview.getChildren().get(0).getRepository(), is("repository1"));
  }

  @Test
  void testGetRoutingRulesPreview_GroupsHandlesMissingGroupRepositories() {
    Repository proxyRepository = repository("repository1", new ProxyType());

    when(repositoryManager.browse()).thenReturn(List.of(proxyRepository));
    when(routingRuleStore.list()).thenReturn(List.of());

    RoutingRulePreviewXO preview = underTest.getRoutingRulesPreview("*", "groups");

    assertThat(preview.getChildren(), empty());
  }

  private static RoutingRule routingRule(final String name) {
    RoutingRuleData routingRule = new RoutingRuleData().name(name).mode(RoutingMode.ALLOW).matchers(List.of(".*"));
    EntityId entityId = new DetachedEntityId(name);
    routingRule.setId(entityId);
    return routingRule;
  }

  private static Repository repository(final String name) {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);
    return repository;
  }

  private static Repository repository(final String name, final org.sonatype.nexus.repository.Type type) {
    Repository repository = mock(Repository.class);
    lenient().when(repository.getName()).thenReturn(name);
    Configuration configuration = mock(Configuration.class);
    lenient().when(repository.getType()).thenReturn(type);
    lenient().when(repository.getFormat()).thenReturn(new Format("maven2")
    {
    });
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(repository.optionalFacet(any())).thenReturn(Optional.empty());
    return repository;
  }

  private static void assertXO(
      final RoutingRuleXO xo,
      final String name,
      final int count,
      final String... repositoryNames)
  {
    assertThat(xo.getName(), is(name));
    assertThat(xo.getId(), is(name));
    assertThat(xo.getAssignedRepositoryCount(), is(count));
    if (repositoryNames == null || repositoryNames.length == 0) {
      assertThat(xo.getAssignedRepositoryNames(), empty());
    }
    else {
      assertThat(xo.getAssignedRepositoryNames(), is(List.of(repositoryNames)));
    }
  }
}
