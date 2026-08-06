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
package org.sonatype.nexus.repository.routing.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.manager.internal.GroupMemberMappingCache;
import org.sonatype.nexus.repository.routing.RoutingRuleInvalidatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RoutingRuleGroupCacheInvalidatorTest
{
  private static final String RULE_ID = UUID.randomUUID().toString();

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private GroupMemberMappingCache groupMemberMappingCache;

  @Mock
  private Repository memberRepository;

  @Mock
  private Repository groupRepository;

  @Mock
  private Configuration memberConfiguration;

  @Mock
  private Configuration oldConfiguration;

  @Mock
  private GroupFacet groupFacet;

  private RoutingRuleGroupCacheInvalidator underTest;

  @Before
  public void setup() {
    underTest = new RoutingRuleGroupCacheInvalidator(repositoryManager, groupMemberMappingCache);
    when(memberRepository.getName()).thenReturn("member-proxy");
    when(memberRepository.getConfiguration()).thenReturn(memberConfiguration);
    when(groupRepository.getName()).thenReturn("member-group");
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(repositoryManager.get("member-group")).thenReturn(groupRepository);
  }

  private EntityId ruleId(final String value) {
    return new EntityUUID(UUID.fromString(value));
  }

  // -- RoutingRuleInvalidatedEvent path -----------------------------------------------------------

  @Test
  public void routingRuleInvalidated_invalidatesEnclosingGroupCache() {
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void routingRuleInvalidated_ignoresRepositoriesWithDifferentRule() {
    String otherRuleId = UUID.randomUUID().toString();
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(otherRuleId));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, never()).invalidateGroupCaches();
    verifyNoInteractions(groupMemberMappingCache);
  }

  @Test
  public void routingRuleInvalidated_ignoresRepositoriesWithNoRuleAssigned() {
    when(memberConfiguration.getRoutingRuleId()).thenReturn(null);
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, never()).invalidateGroupCaches();
  }

  @Test
  public void routingRuleInvalidated_deduplicatesGroupsAcrossMultipleAffectedMembers() {
    Repository secondMember = mock(Repository.class);
    Configuration secondMemberConfig = mock(Configuration.class);
    when(secondMember.getName()).thenReturn("member-proxy-2");
    when(secondMember.getConfiguration()).thenReturn(secondMemberConfig);
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(secondMemberConfig.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(repositoryManager.browse()).thenReturn(Arrays.asList(memberRepository, secondMember));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));
    when(groupMemberMappingCache.getGroups("member-proxy-2")).thenReturn(setOf("member-group"));

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    // both members share the same group; must invalidate once, not twice
    verify(groupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void routingRuleInvalidated_invalidatesAllEnclosingGroupsIncludingNested() {
    Repository parentGroup = mock(Repository.class);
    GroupFacet parentGroupFacet = mock(GroupFacet.class);
    when(parentGroup.getName()).thenReturn("outer-group");
    when(parentGroup.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(parentGroupFacet));
    when(repositoryManager.get("outer-group")).thenReturn(parentGroup);

    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));
    when(groupMemberMappingCache.getGroups("member-proxy"))
        .thenReturn(setOf("member-group", "outer-group"));

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, times(1)).invalidateGroupCaches();
    verify(parentGroupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void routingRuleInvalidated_swallowsGroupFacetException() {
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));
    doThrow(new RuntimeException("boom")).when(groupFacet).invalidateGroupCaches();

    // must not propagate — one bad group shouldn't stop invalidation of others
    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void routingRuleInvalidated_ignoresEventWithNullRuleId() {
    underTest.on(new RoutingRuleInvalidatedEvent());

    verifyNoInteractions(repositoryManager);
    verifyNoInteractions(groupMemberMappingCache);
  }

  // -- RepositoryUpdatedEvent path ----------------------------------------------------------------

  @Test
  public void repositoryUpdated_ruleAssigned_invalidatesEnclosingGroupCache() {
    // before: no rule; after: rule assigned
    when(oldConfiguration.getRoutingRuleId()).thenReturn(null);
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));

    underTest.on(new RepositoryUpdatedEvent(memberRepository, oldConfiguration));

    verify(groupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void repositoryUpdated_ruleUnassigned_invalidatesEnclosingGroupCache() {
    when(oldConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(memberConfiguration.getRoutingRuleId()).thenReturn(null);
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));

    underTest.on(new RepositoryUpdatedEvent(memberRepository, oldConfiguration));

    verify(groupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void repositoryUpdated_ruleChanged_invalidatesEnclosingGroupCache() {
    String otherId = UUID.randomUUID().toString();
    when(oldConfiguration.getRoutingRuleId()).thenReturn(ruleId(otherId));
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));

    underTest.on(new RepositoryUpdatedEvent(memberRepository, oldConfiguration));

    verify(groupFacet, times(1)).invalidateGroupCaches();
  }

  @Test
  public void repositoryUpdated_ruleUnchanged_doesNotInvalidateGroupCache() {
    when(oldConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));

    underTest.on(new RepositoryUpdatedEvent(memberRepository, oldConfiguration));

    verify(groupFacet, never()).invalidateGroupCaches();
    verify(groupMemberMappingCache, never()).getGroups(anyString());
  }

  @Test
  public void repositoryUpdated_bothRuleIdsNull_doesNotInvalidateGroupCache() {
    when(oldConfiguration.getRoutingRuleId()).thenReturn(null);
    when(memberConfiguration.getRoutingRuleId()).thenReturn(null);

    underTest.on(new RepositoryUpdatedEvent(memberRepository, oldConfiguration));

    verify(groupFacet, never()).invalidateGroupCaches();
    verify(groupMemberMappingCache, never()).getGroups(anyString());
  }

  @Test
  public void repositoryUpdated_nullOldConfiguration_isSafe() {
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));

    underTest.on(new RepositoryUpdatedEvent(memberRepository, null));

    verify(groupFacet, never()).invalidateGroupCaches();
    verify(groupMemberMappingCache, never()).getGroups(anyString());
  }

  @Test
  public void repositoryUpdated_nullNewConfiguration_isSafe() {
    when(memberRepository.getConfiguration()).thenReturn(null);

    underTest.on(new RepositoryUpdatedEvent(memberRepository, oldConfiguration));

    verify(groupFacet, never()).invalidateGroupCaches();
    verify(groupMemberMappingCache, never()).getGroups(anyString());
  }

  @Test
  public void routingRuleInvalidated_nullRepositoryConfiguration_isSkipped() {
    when(memberRepository.getConfiguration()).thenReturn(null);
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, never()).invalidateGroupCaches();
    verifyNoInteractions(groupMemberMappingCache);
  }

  @Test
  public void invalidateEnclosingGroups_groupWithNoGroupFacet_isSkipped() {
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, never()).invalidateGroupCaches();
  }

  @Test
  public void invalidateEnclosingGroups_groupNotFoundInManager_isSkipped() {
    when(memberConfiguration.getRoutingRuleId()).thenReturn(ruleId(RULE_ID));
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(memberRepository));
    when(groupMemberMappingCache.getGroups("member-proxy")).thenReturn(setOf("member-group"));
    when(repositoryManager.get("member-group")).thenReturn(null);

    underTest.on(new RoutingRuleInvalidatedEvent(ruleId(RULE_ID)));

    verify(groupFacet, never()).invalidateGroupCaches();
  }

  private static Set<String> setOf(final String... values) {
    return new HashSet<>(Arrays.asList(values));
  }
}
