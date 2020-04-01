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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.SelectProvider;

import static org.sonatype.nexus.repository.content.browse.internal.BrowseNodeDAOQueryBuilder.WHERE_PARAMS;

/**
 * DAO to support tree browsing of assets & components
 *
 * @since 3.22
 */
@SchemaTemplate("format")
public interface BrowseNodeDAO
    extends ContentDataAccess
{
  /**
   * Checks whether a browse node exists for the given asset.
   */
  boolean assetNodeExists(@Param("asset") Asset asset);

  /**
   * Creates a node updating the passed node with the generated ID. (Upsert may be used when supported.)
   *
   * NOTE: Asset and Component ID are not set.
   */
  void createNode(@Param("repository") ContentRepository repository, @Param("node") DatastoreBrowseNode node);

  /**
   * Attempt to remove a browse node by its identifier.
   */
  boolean deleteBrowseNode(@Param("browseNodeId") int browseNodeId);

  /**
   * Attempt to safely remove nodes from a repository up to the maximum specified.
   */
  int deleteRepository(@Param("repository") ContentRepository repository, @Param("maxNodes") int maxNodes);

  /**
   * Find the direct children of a browse node.
   */
  @SelectProvider(type = BrowseNodeDAOQueryBuilder.class, method = "findChildrenQuery")
  @ResultMap("datastoreBrowseNode")
  Iterable<DatastoreBrowseNode> findChildren(
      @Param("repository") ContentRepository repository,
      @Param("path") String path,
      @Param("maxNodes") int maxNodes,
      @Param("contentSelectors") List<String> contentSelectors,
      @Param(WHERE_PARAMS) Map<String, Object> whereParams);

  /**
   * Find the deepest node in the given paths.
   */
  Optional<DatastoreBrowseNode> findDeepestNode(
      @Param("repository") ContentRepository repository,
      @Param("paths") String... paths);

  /**
   * Find the browse node representing the specified path.
   */
  Optional<DatastoreBrowseNode> findPath(
      @Param("repository") ContentRepository repository,
      @Param("path") String path);

  /**
   * Find the browse node associated with the specified asset.
   */
  Optional<DatastoreBrowseNode> findBrowseNodeByAssetId(@Param("asset") Asset asset);

  /**
   * Find the browse node associated with the specified component.
   */
  Optional<DatastoreBrowseNode> findBrowseNodeByComponentId(@Param("component") Component component);

  /**
   * Get the parentId of a browse node if it exists.
   */
  Optional<Integer> getParentBrowseNodeId(@Param("browseNodeId") int browseNodeId);

  /**
   * Link an asset to a specified browse node.
   */
  void linkAsset(@Param("nodeId") int nodeId, @Param("asset") Asset asset);

  /**
   * Link a component to a specified browse node.
   */
  void linkComponent(@Param("nodeId") int nodeId, @Param("component") Component component);

  /**
   * Delete an asset node if it has no children and is not linked to a component.
   */
  boolean maybeDeleteAssetNode(@Param("asset") Asset asset);

  /**
   * Delete a component node if it has no children and is not linked to an asset.
   */
  boolean maybeDeleteComponentNode(@Param("component") Component component);

  /**
   * Delete a node if it has no children.
   */
  boolean maybeDeleteNode(
      @Param("repository") ContentRepository repository,
      @Param("path") String path);

  /**
   * Remove an asset link from a node, if it is also currently linked to a component.
   */
  boolean unlinkAsset(@Param("asset") Asset asset);

  /**
   * Remove a component link from a node, if it is also currently linked to an asset.
   */
  boolean unlinkComponent(@Param("component") Component component);
}
