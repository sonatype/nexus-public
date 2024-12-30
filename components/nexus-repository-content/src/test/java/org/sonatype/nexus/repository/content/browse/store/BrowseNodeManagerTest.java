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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.content.store.AssetData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.browse.store.BrowseNodeManager.MAX_CHILDREN;

@RunWith(MockitoJUnitRunner.class)
public class BrowseNodeManagerTest
    extends TestSupport
{
  @Mock
  BrowseNodeStore<BrowseNodeDAO> browseNodeStore;

  BrowseNodeManager underTest;

  @Before
  public void setup() {
    underTest = new BrowseNodeManager(browseNodeStore, 1);
  }

  @Test
  public void testHasChildrenWithNoChildren() {
    Long parentNodeId = 1L;
    when(browseNodeStore.getChildByParentNodeId(parentNodeId, MAX_CHILDREN, 0)).thenReturn(Collections.emptyList());
    boolean hasChildren = underTest.hasAnyAssetOrComponentChildren(1L);
    assertThat(hasChildren, is(false));
  }

  @Test
  public void testHasChildrenWithSubFolderButNoChildren() {
    Long parentNodeId = 1L;
    when(browseNodeStore.getChildByParentNodeId(parentNodeId, MAX_CHILDREN, 0)).thenReturn(
        generateChildrenNodes(List.of(3L, 2L), null));
    when(browseNodeStore.getChildByParentNodeId(2L, MAX_CHILDREN, 0)).thenReturn(Collections.emptyList());

    boolean hasChildren = underTest.hasAnyAssetOrComponentChildren(1L);
    assertThat(hasChildren, is(false));
  }

  @Test
  public void testHasChildrenWithSubFolderAndChildren() {
    AssetData asset = new AssetData();
    asset.setAssetId(1000);

    Long parentNodeId = 1L;
    when(browseNodeStore.getChildByParentNodeId(parentNodeId, MAX_CHILDREN, 0)).thenReturn(
        generateChildrenNodes(List.of(2L, 3L), null));
    when(browseNodeStore.getChildByParentNodeId(2L, MAX_CHILDREN, 0)).thenReturn(
        generateChildrenNodes(List.of(4L, 5L), asset));

    boolean hasChildren = underTest.hasAnyAssetOrComponentChildren(1L);
    assertThat(hasChildren, is(true));
  }

  private List<BrowseNode> generateChildrenNodes(List<Long> nodeIds, @Nullable AssetData asset) {
    return nodeIds.stream().map(nodeId -> {
      BrowseNodeData childNode = new BrowseNodeData();
      childNode.setAssetCount(0L);
      childNode.setNodeId(nodeId);

      if (nodeId == 5L) {
        childNode.setAsset(asset);
      }
      return childNode;
    }).collect(Collectors.toList());
  }
}
