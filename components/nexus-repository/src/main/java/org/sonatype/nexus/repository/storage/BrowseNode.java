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

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a path segment in a tree hierarchy.
 *
 * @since 3.6
 */
public class BrowseNode
    extends Entity
{
  private EntityId assetId;

  private EntityId componentId;

  private EntityId parentId;

  private String path;

  private String repositoryName;

  private String assetNameLowercase;

  private boolean leaf;

  @Nullable
  public EntityId getAssetId() {
    return assetId;
  }

  @Nullable
  public EntityId getComponentId() {
    return componentId;
  }

  @Nullable
  public EntityId getParentId() {
    return parentId;
  }

  public String getPath() {
    return require(path, BrowseNodeSqlBuilder.P_PATH);
  }

  public String getRepositoryName() {
    return require(repositoryName, BrowseNodeSqlBuilder.P_REPOSITORY_NAME);
  }

  /**
   * @since 3.7
   */
  @Nullable
  public String getAssetNameLowercase() {
    return assetNameLowercase;
  }

  /**
   * @since 3.7
   */
  public boolean isLeaf() {
    return leaf;
  }

  public void setAssetId(@Nullable final EntityId assetId) {
    this.assetId = assetId;
  }

  public void setComponentId(@Nullable final EntityId componentId) {
    this.componentId = componentId;
  }

  public void setParentId(@Nullable final EntityId parentId) {
    this.parentId = parentId;
  }

  public void setPath(final String path) {
    checkNotNull(path);
    this.path = path;
  }

  public void setRepositoryName(final String repositoryName) {
    checkNotNull(repositoryName);
    this.repositoryName = repositoryName;
  }

  /**
   * @since 3.7
   */
  public void setAssetNameLowercase(@Nullable final String assetNameLowercase) {
    this.assetNameLowercase = assetNameLowercase;
  }

  /**
   * @since 3.7
   */
  public void setLeaf(final boolean leaf) {
    this.leaf = leaf;
  }

  public BrowseNode withAssetId(@Nullable final EntityId assetId) {
    setAssetId(assetId);
    return this;
  }

  public BrowseNode withComponentId(@Nullable final EntityId componentId) {
    setComponentId(componentId);
    return this;
  }

  public BrowseNode withParentId(@Nullable final EntityId parentId) {
    setParentId(parentId);
    return this;
  }

  public BrowseNode withPath(final String path) {
    setPath(path);
    return this;
  }

  public BrowseNode withRepositoryName(final String repositoryName) {
    setRepositoryName(repositoryName);
    return this;
  }

  /**
   * @since 3.7
   */
  public BrowseNode withAssetNameLowercase(@Nullable final String assetNameLowercase) {
    setAssetNameLowercase(assetNameLowercase);
    return this;
  }

  private <V> V require(final V value, final String name) {
    checkState(value != null, "Missing property: %s", name);
    return value;
  }

  @Override
  public String toString() {
    return "BrowseNode{" +
        "assetId=" + assetId +
        ", componentId=" + componentId +
        ", parentId=" + parentId +
        ", path='" + path + '\'' +
        ", repositoryName='" + repositoryName + '\'' +
        ", assetNameLowercase='" + assetNameLowercase + '\'' +
        '}';
  }
}
