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
package org.sonatype.nexus.repository.storage;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;

/**
 * Store providing access to the browse tree for assets & components.
 *
 * @since 3.6
 */
public interface BrowseNodeStore
{
  /**
   * Adds nodes to the tree from pathSegments
   *
   * @return the leaf path
   */
  BrowseNode createNodes(String repositoryName, Iterable<String> pathSegments);

  /**
   * Adds nodes to the tree from pathSegments
   *
   * @param repositoryName the name of the repository to create the tree for
   * @param pathSegments the path to create browse nodes for
   * @param createChildLinks whether to skip creating child links or not (used when bulk inserting nodes)
   * @return the leaf path
   */
  BrowseNode createNodes(String repositoryName, Iterable<String> pathSegments, boolean createChildLinks);

  /**
   * @param id
   * @return the BrowseNode identified by id
   */
  BrowseNode getById(EntityId id);

  /**
   * @param assetId
   * @return the BrowseNode with the associated assetId or null if no browse node was found
   */
  BrowseNode getByAssetId(EntityId assetId);

  /**
   * @param repositoryName
   * @param pathSegments
   * @return all nodes matching the path segments for the repository or an empty list
   */
  Iterable<BrowseNode> getByPath(final String repositoryName, final Iterable<String> pathSegments);

  /**
   * Get the list of nodes found under the specified tree path. (Filtered by permissions)
   *
   * @return the children or null if the path does not exist
   */
  @Nullable
  Iterable<BrowseNode> getChildrenByPath(Repository repository, Iterable<String> pathSegments, @Nullable String filter);

  /**
   * Save a node to the store
   */
  BrowseNode save(BrowseNode node);

  /**
   * Save a node to the store
   */
  BrowseNode save(BrowseNode node, boolean updateChildLinks);

  /**
   * Remove a node from the store, including any parent nodes that would now have no children
   */
  void deleteNode(BrowseNode node);

  /**
   * Remove a node from the store that matches the specified Asset,
   * including any parent nodes that would now have no children
   */
  void deleteNodeByAssetId(EntityId assetId);

  void truncateRepository(String repositoryName);

  /**
   * Update all browse node children_ids sets in a repository
   *
   * @param repositoryName of the repository to update
   */
  void updateChildNodes(String repositoryName);
}
