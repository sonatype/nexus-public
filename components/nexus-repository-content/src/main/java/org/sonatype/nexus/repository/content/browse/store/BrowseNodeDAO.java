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
import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.content.store.AssetDAO;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;

import org.apache.ibatis.annotations.Param;

/**
 * Browse node {@link ContentDataAccess}.
 *
 * @since 3.26
 */
@Expects({ContentRepositoryDAO.class, ComponentDAO.class, AssetDAO.class})
@SchemaTemplate("format")
public interface BrowseNodeDAO
    extends ContentDataAccess
{
  String FILTER_PARAMS = "filterParams";

  /**
   * Retrieves the browse nodes directly under the given hierarchical display path.
   *
   * @param repositoryId the repository containing the browse nodes
   * @param displayPath  the hierarchical path leading up to the browse nodes
   * @param limit        when positive limits the number of browse nodes returned
   * @param filter       optional filter to apply
   * @param filterParams parameter map for the optional filter
   * @return browse nodes found directly under the display path
   */
  List<BrowseNode> getByDisplayPath(
      @Param("repositoryId") int repositoryId,
      @Param("displayPath") List<String> displayPath,
      @Param("limit") int limit,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) Map<String, Object> filterParams);

  /**
   * Does a browse node already exist for this component?
   */
  boolean hasComponentNode(@Param("componentId") int componentId);

  /**
   * Does a browse node already exist for this asset?
   */
  boolean hasAssetNode(@Param("assetId") int assetId);

  /**
   * Merges the given browse node with the tree of nodes in the content data store.
   *
   * @param browseNode the node to merge
   */
  void mergeBrowseNode(BrowseNodeData browseNode);

  /**
   * Trims leaf browse nodes in the given repository that no longer have a component or asset.
   *
   * @param repositoryId the repository containing the browse nodes
   * @return {@code true} if any nodes were trimmed from the tree
   */
  boolean trimBrowseNodes(@Param("repositoryId") int repositoryId);

  /**
   * Deletes all browse nodes in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the browse nodes
   * @param limit        when positive limits the number of browse nodes deleted per-call
   * @return {@code true} if any browse nodes were deleted
   */
  boolean deleteBrowseNodes(@Param("repositoryId") int repositoryId, @Param("limit") int limit);

  /**
   * Deletes a browse node by its asset internal id and path.
   *
   * @param internalAssetId the asset internal id
   * @param path            the path
   * @return the parent node id of the deleted node
   */
  Long deleteByAssetIdAndPath(@Param("internalAssetId") Integer internalAssetId, @Param("path") String path);

  /**
   * Retrieves a list of parent browse nodes for the given node id.
   *
   * @param internalNodeId the node id
   * @return the list of parent browse nodes
   */
  List<BrowseNode> getNodeParents(@Param("internalNodeId") Long internalNodeId);

  /**
   * Deletes a browse node by its node id.
   *
   * @param internalNodeId the node id
   */
  void delete(@Param("internalNodeId") Long internalNodeId);

  /**
   * Retrieves the browse node by its request path.
   *
   * @param repositoryId the repository containing the browse nodes
   * @param requestPath  the request path
   * @return the browse node if found
   */
  List<BrowseNode> getByRequestPath(@Param("repositoryId") int repositoryId, @Param("requestPath") String requestPath);
}
