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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;

/**
 * Manages browse nodes for a specific repository.
 *
 * @since 3.26
 */
public class BrowseNodeManager
    extends ComponentSupport
{
  private final BrowseNodeStore<BrowseNodeDAO> browseNodeStore;

  private final int repositoryId;

  public BrowseNodeManager(final BrowseNodeStore<BrowseNodeDAO> browseNodeStore, final int repositoryId) {
    this.browseNodeStore = checkNotNull(browseNodeStore);
    this.repositoryId = repositoryId;
  }

  /**
   * Retrieves the browse nodes directly under the given hierarchical display path.
   *
   * @param displayPath the hierarchical path leading up to the browse nodes
   * @param limit when positive limits the number of browse nodes returned
   * @param filter optional filter to apply to the browse nodes
   * @param filterParams parameter map for the optional filter
   * @return browse nodes found directly under the display path
   */
  public List<BrowseNode> getByDisplayPath(
      final List<String> displayPath,
      final int limit,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return browseNodeStore.getByDisplayPath(repositoryId, displayPath, limit, filter, filterParams);
  }

  /**
   * Does a browse node already exist for this component?
   */
  public boolean hasComponentNode(final Component component) {
    return browseNodeStore.hasComponentNode(internalComponentId(component));
  }

  /**
   * Does a browse node already exist for this asset?
   */
  public boolean hasAssetNode(final Asset asset) {
    return browseNodeStore.hasAssetNode(internalAssetId(asset));
  }

  /**
   * Creates browse nodes for the path, applying a final step to the last node.
   */
  public void createBrowseNodes(final List<BrowsePath> paths, final Consumer<BrowseNodeData> finalStep) {
    Integer parentId = null;
    for (int i = 0; i < paths.size(); i++) {
      BrowseNodeData node = new BrowseNodeData();
      node.setRepositoryId(repositoryId);
      node.setRequestPath(paths.get(i).getRequestPath());
      node.setDisplayName(paths.get(i).getDisplayName());
      if (parentId != null) {
        node.setParentId(parentId);
      }
      if (i == paths.size() - 1) {
        finalStep.accept(node);
      }
      browseNodeStore.mergeBrowseNode(node);
      parentId = node.nodeId;
    }
  }

  /**
   * Deletes all browse nodes associated with the repository.
   */
  public void deleteBrowseNodes() {
    browseNodeStore.deleteBrowseNodes(repositoryId);
  }
}
