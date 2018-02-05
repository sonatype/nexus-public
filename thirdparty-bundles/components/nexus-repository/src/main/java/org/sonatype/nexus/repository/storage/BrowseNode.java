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

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.EntityId;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a path segment in a tree hierarchy.
 *
 * @since 3.6
 */
public class BrowseNode
    extends AbstractEntity
{
  private String repositoryName;

  private String parentPath;

  private String name;

  private boolean leaf;

  @Nullable
  private EntityId componentId;

  @Nullable
  private EntityId assetId;

  @Nullable
  private String assetNameLowercase;

  public String getRepositoryName() {
    return require(repositoryName, BrowseNodeEntityAdapter.P_REPOSITORY_NAME);
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = checkNotNull(repositoryName);
  }

  /**
   * @since 3.7
   */
  public String getParentPath() {
    return require(parentPath, BrowseNodeEntityAdapter.P_PARENT_PATH);
  }

  /**
   * @since 3.7
   */
  public void setParentPath(final String parentPath) {
    this.parentPath = checkNotNull(parentPath);
  }

  /**
   * @since 3.7
   */
  public String getName() {
    return require(name, BrowseNodeEntityAdapter.P_NAME);
  }

  /**
   * @since 3.7
   */
  public void setName(final String name) {
    this.name = checkNotNull(name);
  }

  /**
   * @since 3.6.1
   */
  public boolean isLeaf() {
    return leaf;
  }

  /**
   * @since 3.6.1
   */
  public void setLeaf(final boolean leaf) {
    this.leaf = leaf;
  }

  @Nullable
  public EntityId getComponentId() {
    return componentId;
  }

  public void setComponentId(final EntityId componentId) {
    this.componentId = checkNotNull(componentId);
  }

  @Nullable
  public EntityId getAssetId() {
    return assetId;
  }

  public void setAssetId(final EntityId assetId) {
    this.assetId = checkNotNull(assetId);
  }

  /**
   * @since 3.6.1
   */
  @Nullable
  public String getAssetNameLowercase() {
    return assetNameLowercase;
  }

  /**
   * @since 3.6.1
   */
  public void setAssetNameLowercase(final String assetNameLowercase) {
    this.assetNameLowercase = checkNotNull(assetNameLowercase);
  }

  private <V> V require(final V value, final String name) {
    checkState(value != null, "Missing property: %s", name);
    return value;
  }

  @Override
  public String toString() {
    return "BrowseNode{" +
        "repositoryName=" + repositoryName +
        ", parentPath=" + parentPath +
        ", name=" + name +
        ", leaf=" + leaf +
        ", componentId='" + componentId + '\'' +
        ", assetId='" + assetId + '\'' +
        ", assetNameLowercase='" + assetNameLowercase + '\'' +
        '}';
  }
}
