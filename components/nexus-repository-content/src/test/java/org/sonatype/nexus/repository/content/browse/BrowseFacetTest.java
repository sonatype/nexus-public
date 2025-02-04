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
package org.sonatype.nexus.repository.content.browse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeManager;
import org.sonatype.nexus.repository.ossindex.PackageUrlService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrowseFacetTest
    extends TestSupport
{
  @Mock
  private BrowseNodeManager browseNodeManager;

  @Mock
  private Repository repository;

  @Mock
  private PackageUrlService packageUrlService;

  private BrowseFacet underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new BrowseFacetImpl(
        Collections.emptyMap(),
        Collections.emptyMap(),
        packageUrlService,
        1000);

    when(repository.getFormat()).thenReturn(new Format("raw")
    {
    });
    when(repository.getName()).thenReturn("My-Raw-Repository");

    underTest.attach(repository);

    Field browseNodeManagerField = BrowseFacetImpl.class.getDeclaredField("browseNodeManager");
    browseNodeManagerField.setAccessible(true);
    browseNodeManagerField.set(underTest, browseNodeManager);
  }

  @Test
  public void testDeleteByAssetIdAndPath() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(Collections.emptyList());

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager, never()).delete(anyLong());
  }

  @Test
  public void testDeleteByAssetIdAndPathWithParentNode() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;
    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setAssetCount(0L);
    parentNode.setNodeId(parentNodeId);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(Arrays.asList(parentNode));

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager).delete(parentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndPathWithParentNodeWithAssets() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;
    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setAssetCount(10L);
    parentNode.setNodeId(parentNodeId);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(Arrays.asList(parentNode));

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager, never()).delete(parentNodeId);
  }

  @Test
  public void testDeleteByAssetIdAndParentWithChildIsNotDeleted() {
    Integer internalAssetId = 1;
    String path = "test/path";
    Long parentNodeId = 2L;

    BrowseNodeData childNode = new BrowseNodeData();
    childNode.setAssetCount(0L);
    childNode.setNodeId(100L);

    when(browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path)).thenReturn(parentNodeId);
    when(browseNodeManager.getNodeParents(parentNodeId)).thenReturn(generateParentNodes());

    when(browseNodeManager.hasAnyAssetOrComponentChildren(3L)).thenReturn(true);

    underTest.deleteByAssetIdAndPath(internalAssetId, path);

    verify(browseNodeManager, times(1)).deleteByAssetIdAndPath(internalAssetId, path);
    verify(browseNodeManager, times(1)).delete(parentNodeId);
  }

  private List<BrowseNode> generateParentNodes() {
    BrowseNodeData parentNodeTwo = new BrowseNodeData();
    parentNodeTwo.setAssetCount(0L);
    parentNodeTwo.setNodeId(2L);

    BrowseNodeData parentNode = new BrowseNodeData();
    parentNode.setAssetCount(0L);
    parentNode.setNodeId(3L);

    return List.of(parentNodeTwo, parentNode);
  }
}
