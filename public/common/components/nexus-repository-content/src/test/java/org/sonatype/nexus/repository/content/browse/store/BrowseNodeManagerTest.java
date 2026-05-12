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

import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.browse.store.BrowseNodeManager.MAX_CHILDREN;

@ExtendWith({LoggingExtension.class, MockitoExtension.class})
public class BrowseNodeManagerTest
{
  @CaptureLogsFor(value = BrowseNodeManager.class, level = org.slf4j.event.Level.DEBUG)
  TestLogAccessor logs;

  @Mock
  BrowseNodeStore<BrowseNodeDAO> browseNodeStore;

  BrowseNodeManager underTest;

  private static final int REPOSITORY_ID = 1;

  @BeforeEach
  public void setup() {
    underTest = new BrowseNodeManager(browseNodeStore, REPOSITORY_ID);
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
    when(browseNodeStore.getChildByParentNodeId(3L, MAX_CHILDREN, 0)).thenReturn(Collections.emptyList());

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

  private List<BrowseNode> generateChildrenNodes(final List<Long> nodeIds, @Nullable final AssetData asset) {
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

  @Test
  public void testDoCreateBrowseNodesHandlesDuplicateKeyAtDebugLevel() {
    List<BrowsePath> paths = createTestPaths();
    RuntimeException cause = new RuntimeException("Unique constraint violation");
    DuplicateKeyException duplicateException = new DuplicateKeyException(cause);

    doThrow(duplicateException).when(browseNodeStore).mergeBrowseNode(any(BrowseNodeData.class));

    underTest.doCreateBrowseNodes(paths, node -> {
    });

    verify(browseNodeStore, times(1)).mergeBrowseNode(any(BrowseNodeData.class));

    List<ILoggingEvent> logEvents = logs.logs();
    assertFalse(logEvents.isEmpty(), "Expected at least one log event");

    boolean foundDebugLog = logEvents.stream()
        .anyMatch(event -> event.getLevel() == Level.DEBUG
            && event.getFormattedMessage().contains("Duplicate key for browse node found"));

    assertTrue(foundDebugLog, "Expected DEBUG level log for duplicate key, but found: " +
        logEvents.stream()
            .map(e -> e.getLevel() + ": " + e.getFormattedMessage())
            .collect(Collectors.joining(", ")));

    boolean foundWarnLog = logEvents.stream()
        .anyMatch(event -> event.getLevel() == Level.WARN
            && event.getFormattedMessage().contains("Duplicate key for browse node found"));

    assertFalse(foundWarnLog, "Should NOT log at WARN level for duplicate key");
  }

  @Test
  public void testDoCreateBrowseNodesSuccess() {
    List<BrowsePath> paths = createTestPaths();

    underTest.doCreateBrowseNodes(paths, node -> {
    });

    verify(browseNodeStore, times(3)).mergeBrowseNode(any(BrowseNodeData.class));
  }

  private List<BrowsePath> createTestPaths() {
    return List.of(
        new BrowsePath("test", "Test"),
        new BrowsePath("test/package", "Package"),
        new BrowsePath("test/package/file", "File"));
  }
}
