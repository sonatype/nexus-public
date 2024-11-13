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

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

/**
 * {@link BrowseNode} data backed by the content data store.
 *
 * Note: the component and asset internal id fields have a prefix of "db" to avoid clashing with
 * the external id getters {@link #getComponentId} and {@link #getAssetId} from {@link BrowseNode}
 * which return a different incompatible type.
 *
 * @since 3.26
 */
public class BrowseNodeData
    implements BrowseNode
{
  Long nodeId; // NOSONAR: internal id

  int repositoryId; // NOSONAR: internal repository id

  private String requestPath;

  private String displayName;

  long parentId; // NOSONAR: internal id

  private boolean leaf;

  @Nullable
  Integer dbComponentId; // NOSONAR: internal id

  @Nullable
  Integer dbAssetId; // NOSONAR: internal id

  @Nullable
  private String packageUrl;

  @Nullable
  private Long assetCount;

  @Nullable
  private OffsetDateTime lastUpdated;

  // BrowseNode API

  @Override
  public String getPath() {
    return requestPath;
  }

  @Override
  public String getName() {
    return displayName;
  }

  @Override
  public boolean isLeaf() {
    return leaf;
  }

  @Override
  public EntityId getComponentId() {
    return dbComponentId != null ? toExternalId(dbComponentId) : null;
  }

  @Override
  public EntityId getAssetId() {
    return dbAssetId != null ? toExternalId(dbAssetId) : null;
  }

  @Nullable
  @Override
  public String getPackageUrl() {
    return packageUrl;
  }

  @Override
  public Long getAssetCount() { return assetCount != null ? assetCount : 0L; }

  public Long getNodeId() {
    return nodeId;
  }

  @Nullable
  @Override
  public OffsetDateTime getLastUpdated() {
    return lastUpdated;
  }

  // MyBatis setters + validation

  /**
   * Sets the internal node id.
   */
  public void setNodeId(final long nodeId) {
    this.nodeId = nodeId;
  }

  /**
   * Sets the internal repository id.
   */
  public void setRepositoryId(final int repositoryId) {
    this.repositoryId = repositoryId;
  }

  /**
   * Sets the request path.
   */
  public void setRequestPath(final String requestPath) {
    this.requestPath = checkNotNull(requestPath);
  }

  /**
   * Sets the display name.
   */
  public void setDisplayName(final String displayName) {
    this.displayName = checkNotNull(displayName);
  }

  /**
   * Sets the internal parent node id.
   */
  public void setParentId(final long parentId) {
    this.parentId = parentId;
  }

  /**
   * Sets the (optional) component at this node.
   */
  public void setComponent(@Nullable final Component component) {
    if (component != null) {
      this.dbComponentId = internalComponentId(component);
      this.lastUpdated = component.lastUpdated();
    }
    else {
      this.dbComponentId = null;
    }
  }

  /**
   * Sets the (optional) asset at this node.
   */
  public void setAsset(@Nullable final Asset asset) {
    if (asset != null) {
      this.dbAssetId = internalAssetId(asset);
      this.lastUpdated = asset.lastUpdated();
    }
    else {
      this.dbAssetId = null;
    }
  }

  /**
   * Sets the (optional) package url at this node.
   */
  public void setPackageUrl(@Nullable final String packageUrl) {
    this.packageUrl = packageUrl;
  }

  /**
   * Sets the (optional) asset count to node.
   */
  public void setAssetCount(@Nullable final Long assetCount) { this.assetCount = assetCount; }

  @Override
  public String toString() {
    return "BrowseNode{" + "nodeId='" + nodeId + "', repositoryId='" + repositoryId + "', displayName='" + displayName +
        "', requestPath='" + requestPath + "', leaf='" + leaf + "', parentId='" + parentId + "', packageUrl='" +
        packageUrl + "', dbComponentId='" + dbComponentId + "', dbAssetId='" + dbAssetId + "', assetCount='" +
        assetCount + "', lastUpdated='" + lastUpdated + "'}";
  }
}
