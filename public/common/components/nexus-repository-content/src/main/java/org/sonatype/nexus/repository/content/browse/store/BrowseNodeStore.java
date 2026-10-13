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
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.content.store.ContentStoreSupport;
import org.sonatype.nexus.repository.content.upgrades.SoftDeleteAnchorMigrationStep_2_169;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Browse node store.
 *
 * @since 3.26
 */
public class BrowseNodeStore<T extends BrowseNodeDAO>
    extends ContentStoreSupport<T>
{
  /**
   * Optional dependency serving two independent concerns:
   * <ul>
   * <li>{@link #isPostgresql()} — dialect detection for the {@code getByDisplayPath} SQL,
   * pre-existing.</li>
   * <li>{@link #softDeleteAvailable()} — gates the {@code deleted_on IS NULL} predicate
   * behind {@link SoftDeleteAnchorMigrationStep_2_169#VERSION}.</li>
   * </ul>
   * Both fall back to {@code false} when this is null (which only happens in unit tests
   * that construct the store directly without Spring): {@code isPostgresql()} exercises the
   * H2 code path, and {@code softDeleteAvailable()} returns false — both of which are the
   * correct defaults for unit-test fixtures.
   */
  private DatabaseCheck databaseCheck;

  /** See {@code ComponentStore#softDeleteAvailable}. */
  private volatile boolean softDeleteAvailable;

  @Autowired
  public BrowseNodeStore(
      final DataSessionSupplier sessionSupplier,
      final String contentStoreName,
      final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
  }

  /** See the {@link #databaseCheck} field for what consumers this serves. */
  @Autowired(required = false)
  public void setDatabaseCheck(final DatabaseCheck databaseCheck) {
    this.databaseCheck = databaseCheck;
  }

  private boolean isPostgresql() {
    return databaseCheck != null && databaseCheck.isPostgresql();
  }

  /**
   * See {@code ComponentStore#softDeleteAvailable()}. Disabled for NEXUS-54969 (returns {@code false}
   * unconditionally); when re-enabled it would again gate on {@link SoftDeleteAnchorMigrationStep_2_169#VERSION}.
   */
  protected boolean softDeleteAvailable() {
    // NEXUS-54969: soft-delete disabled at the DB layer. The _2_169 anchor still advances the schema version, so
    // the previous isAtLeast() gate would flip true even though the (now no-op) column migration never added
    // deleted_on to upgraded format tables -- which would cause "column deleted_on does not exist" in the
    // (conan-only) browse-node join filter. Hardcoded false keeps deleted_on out of every read. One-line revert.
    return false;
  }

  /**
   * Retrieves the browse nodes directly under the given hierarchical display path.
   *
   * @param repositoryId the repository containing the browse nodes
   * @param displayPath the hierarchical path leading up to the browse nodes
   * @param limit when positive limits the number of browse nodes returned
   * @param filter optional filter to apply to the browse nodes
   * @param filterParams parameter map for the optional filter
   * @return browse nodes found directly under the display path
   */
  @Transactional
  public List<BrowseNode> getByDisplayPath(
      final int repositoryId,
      final List<String> displayPath,
      final int limit,
      @Nullable final String filter,
      @Nullable final Map<String, Object> filterParams)
  {
    return dao().getByDisplayPath(repositoryId, displayPath, limit, filter, filterParams, softDeleteAvailable());
  }

  /**
   * Does a browse node already exist for this component?
   */
  @Transactional
  public boolean hasComponentNode(final int componentId) {
    return dao().hasComponentNode(componentId);
  }

  /**
   * Does a browse node already exist for this asset?
   */
  @Transactional
  public boolean hasAssetNode(final int assetId) {
    return dao().hasAssetNode(assetId);
  }

  /**
   * Merges the given browse node with the tree of nodes in the content data store.
   *
   * @param browseNode the node to merge
   */
  @Transactional
  public void mergeBrowseNode(final BrowseNodeData browseNode) {
    dao().mergeBrowseNode(browseNode);
  }

  /**
   * Trims any dangling browse nodes from the given repository.
   *
   * @param repositoryId the repository containing the browse nodes
   * @return {@code true} if any nodes were trimmed from the tree
   */
  @Transactional
  public boolean trimBrowseNodes(final int repositoryId) {
    log.debug("Removing unused browse nodes in repository {}", repositoryId);
    boolean trimmed = false;
    while (dao().trimBrowseNodes(repositoryId)) {
      commitChangesSoFar();
      trimmed = true;
    }
    log.debug("Removed unused browse nodes in repository {}", repositoryId);
    return trimmed;
  }

  /**
   * Deletes all browse nodes in the given repository from the content data store.
   *
   * @param repositoryId the repository containing the browse nodes
   * @return {@code true} if any browse nodes were deleted
   */
  @Transactional
  public boolean deleteBrowseNodes(final int repositoryId) {
    log.debug("Deleting all browse nodes in repository {}", repositoryId);

    Long maxNodeId = dao().getMaxNodeId(repositoryId);
    if (maxNodeId == null) {
      log.debug("No browse nodes found in repository {}", repositoryId);
      return false;
    }

    boolean deleted = false;
    int batchSize = deleteBatchSize();
    long currentMinId = 1;

    while (currentMinId <= maxNodeId) {
      long currentMaxId = Math.min(currentMinId + batchSize - 1, maxNodeId);
      int deletedCount = dao().deleteBrowseNodesByIdRange(repositoryId, currentMinId, currentMaxId);

      if (deletedCount > 0) {
        deleted = true;
        log.debug("Deleted {} browse nodes in range {}-{} for repository {}",
            deletedCount, currentMinId, currentMaxId, repositoryId);
      }

      commitChangesSoFar();
      currentMinId = currentMaxId + 1;
    }

    log.debug("Deleted all browse nodes in repository {}", repositoryId);
    return deleted;
  }

  /**
   * Deletes a browse node based on its internal asset Id and node path.
   *
   * @param internalAssetId the internal Id of the asset
   * @param path the path of the node
   * @return the parent node id of the deleted node
   */
  @Transactional
  public Long deleteByAssetIdAndPath(final Integer internalAssetId, final String path) {
    if (isPostgresql()) {
      return dao().deleteByAssetIdAndPath(internalAssetId, path);
    }
    Long parentId = dao().findParentByAssetIdAndPath(internalAssetId, path);
    if (parentId == null) {
      return null;
    }
    dao().deleteNodeByAssetIdAndPath(internalAssetId, path);
    return parentId;
  }

  /**
   * Deletes a browse node based on its internal asset Id.
   *
   * @param internalAssetId the internal Id of the asset
   * @return the parent node id of the deleted node
   */
  @Transactional
  public Long deleteByAssetId(final Integer internalAssetId) {
    if (isPostgresql()) {
      return dao().deleteByAssetId(internalAssetId);
    }
    Long parentId = dao().findParentByAssetId(internalAssetId);
    if (parentId == null) {
      return null;
    }
    dao().deleteNodeByAssetId(internalAssetId);
    return parentId;
  }

  /**
   * Retrieves a list of parent nodes for the given node.
   *
   * @param internalNodeId the internal Id of current node
   * @return list of parent nodes
   */
  @Transactional
  public List<BrowseNode> getNodeParents(final Long internalNodeId) {
    if (isPostgresql()) {
      return dao().getNodeParents(internalNodeId);
    }
    // H2: walk parents iteratively (no recursive CTE on H2). Bounded by tree depth.
    List<BrowseNode> parents = new ArrayList<>();
    BrowseNodeData current = dao().getByNodeId(internalNodeId);
    if (current == null) {
      return parents;
    }
    long parentId = current.getParentId();
    while (parentId != 0L) {
      BrowseNodeData parent = dao().getByNodeId(parentId);
      if (parent == null) {
        break;
      }
      parents.add(parent);
      parentId = parent.getParentId();
    }
    return parents;
  }

  /**
   * Deletes a browse node based on its internal node Id.
   *
   * @param internalNodeId the internal Id of the node
   */
  @Transactional
  public void delete(final Long internalNodeId) {
    dao().delete(internalNodeId);
  }

  /**
   * Retrieves a browse node by its request path.
   *
   * @param repositoryId the repository containing the browse node
   * @param requestPath the request path of the node
   * @return the browse node or {@code null} if not found
   */
  @Transactional
  public BrowseNode getByRequestPath(final int repositoryId, final String requestPath) {
    return dao().getByRequestPath(repositoryId, requestPath)
        .stream()
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves a list of child nodes for the given parent node.
   *
   * @param parentNodeId the internal Id of parent node
   * @param limit when positive limits the number of browse nodes returned
   * @return list of child nodes
   */
  @Transactional
  public List<BrowseNode> getChildByParentNodeId(final Long parentNodeId, final int limit, final int offset) {
    return dao().getChildByParentNodeId(parentNodeId, limit, offset);
  }

  /**
   * Returns the node id of the browse node for the given asset, or {@code null} if none exists.
   *
   * @param assetId the internal asset id
   * @return the node id, or {@code null}
   */
  @Transactional
  @Nullable
  public Long getNodeIdForAsset(final int assetId) {
    return dao().getNodeIdForAsset(assetId);
  }

  /**
   * Deletes the browse node with the given node id and returns its parent id (PostgreSQL only).
   *
   * @param nodeId the node id to delete
   * @return the parent node id, or {@code null}
   */
  @Transactional
  @Nullable
  public Long deleteByNodeIdReturningParentId(final long nodeId) {
    return dao().deleteByNodeIdReturningParentId(nodeId);
  }
}
