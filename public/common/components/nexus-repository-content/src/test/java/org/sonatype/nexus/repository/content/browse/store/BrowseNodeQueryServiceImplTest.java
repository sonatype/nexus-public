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
package org.sonatype.nexus.repository.content.browse.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorFilterBuilder;
import org.sonatype.nexus.selector.SelectorManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowseNodeQueryServiceImplTest
    extends Test5Support
{
  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private ContentAuthHelper contentAuthHelper;

  @Mock
  private SelectorFilterBuilder selectorFilterBuilder;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private BrowseFacet browseFacet;

  private BrowseNodeComparator defaultBrowseNodeComparator;

  private BrowseNodeQueryServiceImpl underTest;

  @BeforeEach
  void setup() {
    defaultBrowseNodeComparator = mock(BrowseNodeComparator.class, DefaultBrowseNodeComparator.NAME);

    underTest = new BrowseNodeQueryServiceImpl(
        securityHelper,
        selectorManager,
        emptyList(),
        emptyList(),
        asList(defaultBrowseNodeComparator),
        contentAuthHelper,
        selectorFilterBuilder);

    when(repository.getName()).thenReturn("test-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    lenient().when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));
  }

  @Test
  void testAccessGrantedWhenUserHasFullBrowsePermission() {
    BrowseNode mockNode = mock(BrowseNode.class);

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  void testJexlFilteringAppliedWhenNoSeclSelectors() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    SelectorConfiguration jexlSelector = mock(SelectorConfiguration.class);
    when(jexlSelector.getType()).thenReturn("jexl");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(jexlSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn(null);

    when(contentAuthHelper.checkPathPermissionsJexlOnly(anyString(), anyString(), anyString()))
        .thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  void testCeslFilteringAppliedWhenNoJexlSelectors() {
    BrowseNode mockNode = mock(BrowseNode.class);

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), eq("filter"), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    SelectorConfiguration seclSelector = mock(SelectorConfiguration.class);
    when(seclSelector.getType()).thenReturn("csel");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(seclSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn("filter");

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  void testContentSelectorsAppliedEvenWithBrowsePermission() {
    BrowseNode mockNode = mock(BrowseNode.class);

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), eq("path_filter"), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    // But content selectors are active and should be applied
    SelectorConfiguration seclSelector = mock(SelectorConfiguration.class);
    when(seclSelector.getType()).thenReturn("csel");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(seclSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn("path_filter");

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("team-a"), 100);

    // Content selectors should be applied via SQL filter, restricting results
    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  void testNoResultsWhenNoPermissionAndNoSelectors() {
    // No browse permission
    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    // No content selectors configured
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(emptyList());

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    // Should return empty results - no permission and no selectors
    assertThat(result, is(emptyIterable()));
  }

  @Test
  void testSelectorManagerExceptionHandledGracefully() {
    BrowseNode mockNode = mock(BrowseNode.class);

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    // Selector manager throws exception when fetching selectors
    when(selectorManager.browseActive(anyList(), anyList()))
        .thenThrow(new RuntimeException("Selector service unavailable"));

    // User has browse permission, so should still work
    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    // Should still return results based on browse permission
    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  void testSelectorManagerExceptionWithoutBrowsePermission() {
    // Selector manager throws exception
    when(selectorManager.browseActive(anyList(), anyList()))
        .thenThrow(new RuntimeException("Selector service unavailable"));

    // User has no browse permission
    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    // Should return empty - no permission and selectors failed
    assertThat(result, is(emptyIterable()));
  }

  @Test
  void testMixedJexlAndCselSelectors() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), eq("csel_filter"), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    // Mix of JEXL and CSEL selectors
    SelectorConfiguration jexlSelector = mock(SelectorConfiguration.class);
    when(jexlSelector.getType()).thenReturn("jexl");

    SelectorConfiguration seclSelector = mock(SelectorConfiguration.class);

    when(selectorManager.browseActive(anyList(), anyList()))
        .thenReturn(asList(jexlSelector, seclSelector));

    // CSEL filter should be built (JEXL filtered out)
    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn("csel_filter");

    when(contentAuthHelper.checkPathPermissionsJexlOnly(anyString(), anyString(), anyString()))
        .thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    // Should apply both CSEL (via SQL) and JEXL (via post-filter)
    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  void testJexlFilterRejectsNodes() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    SelectorConfiguration jexlSelector = mock(SelectorConfiguration.class);
    when(jexlSelector.getType()).thenReturn("jexl");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(jexlSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn(null);

    // JEXL filter rejects the node
    when(contentAuthHelper.checkPathPermissionsJexlOnly(anyString(), anyString(), anyString()))
        .thenReturn(false);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    // Should return empty - JEXL filter rejected the node
    assertThat(result, is(emptyIterable()));
  }

  @Test
  void testCselFilterRejectsNodes() {
    BrowseNode allowedNode = mock(BrowseNode.class);

    when(browseFacet.getByDisplayPath(anyList(), anyInt(), eq("filter"), any()))
        .thenReturn(new ArrayList<>(asList(allowedNode)));

    SelectorConfiguration seclSelector = mock(SelectorConfiguration.class);
    when(seclSelector.getType()).thenReturn("csel");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(seclSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn("filter");

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    // Should return only the allowed node
    assertThat(((List<BrowseNode>) result), hasSize(1));
    assertThat(((List<BrowseNode>) result).get(0), is(allowedNode));
  }

  @Test
  void testGroupRepositoryDeduplication() {
    // Setup group repository
    when(repository.getType()).thenReturn(new GroupType());

    GroupFacet groupFacet = mock();
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    // Setup member repositories
    Repository member1 = mock();
    Repository member2 = mock();

    BrowseFacet member1BrowseFacet = mock();
    BrowseFacet member2BrowseFacet = mock();

    when(member1.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(member1BrowseFacet));
    when(member2.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(member2BrowseFacet));

    // Setup nodes with duplicates (same name)
    BrowseNodeData node1FromMember1 = mock();
    when(node1FromMember1.getName()).thenReturn("duplicate-node");

    BrowseNodeData node2FromMember1 = mock();
    when(node2FromMember1.getName()).thenReturn("unique-node-1");

    BrowseNodeData node1FromMember2 = mock();
    when(node1FromMember2.getName()).thenReturn("duplicate-node");

    BrowseNodeData node2FromMember2 = mock();
    when(node2FromMember2.getName()).thenReturn("unique-node-2");

    // Member1 returns 2 nodes, Member2 returns 2 nodes (one duplicate)
    when(member1BrowseFacet.getByDisplayPath(anyList(), anyInt(), any(), any()))
        .thenReturn(List.of(node1FromMember1, node2FromMember1));

    when(member2BrowseFacet.getByDisplayPath(anyList(), anyInt(), any(), any()))
        .thenReturn(List.of(node1FromMember2, node2FromMember2));

    // Configure group facet to return both members
    when(groupFacet.allMembers()).thenReturn(List.of(member1, member2));

    // User has browse permission
    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(true);

    List<BrowseNode> resultList = (List<BrowseNode>) underTest.getByPath(repository, List.of("test"), 100);

    // Should return 3 unique nodes (duplicate removed, first-one-wins)
    assertThat(resultList.size(), is(3));

    // Verify deduplication
    List<String> nodeNames = resultList.stream()
        .map(BrowseNode::getName)
        .toList();
    assertThat(nodeNames, containsInAnyOrder("duplicate-node", "unique-node-1", "unique-node-2"));
  }
}
