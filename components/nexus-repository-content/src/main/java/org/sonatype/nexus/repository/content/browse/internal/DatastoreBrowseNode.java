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

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @since 3.22
 */
public class DatastoreBrowseNode
    implements BrowseNode<Integer>
{
  @Nullable
  private Integer assetId;

  @Nullable
  private Integer componentId;

  @Nullable
  private Integer id;

  private String format;

  private boolean leaf;

  private String name;

  @Nullable
  private Integer parentId;

  private String path;

  private int repositoryId;

  public DatastoreBrowseNode() {
    // persistence
  }

  public DatastoreBrowseNode(
      final String format,
      final Integer parentId,
      final String path,
      final String name)
  {
    this.format = format;
    this.parentId = parentId;
    this.path = path;
    this.name = name;
  }

  @Override
  @Nullable
  public Integer getAssetId() {
    return assetId;
  }

  @Override
  @Nullable
  public Integer getComponentId() {
    return componentId;
  }

  public String getFormat() {
    return format;
  }

  public Integer getId() {
    return id;
  }

  @Override
  public String getName() {
    return require(name, "name");
  }

  @Nullable
  public Integer getParentId() {
    return parentId;
  }

  @Override
  public String getPath() {
    return require(path, "path");
  }

  public int getRepositoryId() {
    return require(repositoryId, "repositoryId");
  }

  @Override
  public boolean isLeaf() {
    return leaf;
  }

  private <V> V require(final V value, final String name) {
    checkState(value != null, "Missing property: %s", name);
    return value;
  }

  public void setAssetId(final Integer assetId) {
    this.assetId = assetId;
  }

  public void setComponentId(final Integer componentId) {
    this.componentId = componentId;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public void setId(final Integer id) {
    this.id = id;
  }

  public void setLeaf(final boolean leaf) {
    this.leaf = leaf;
  }

  public void setName(final String name) {
    this.name = checkNotNull(name);
  }

  public void setParentId(final Integer parentId) {
    this.parentId = parentId;
  }

  public void setPath(final String path) {
    this.path = checkNotNull(path);
  }

  public void setPaths(final List<BrowsePath> paths) {
    setName(paths.get(paths.size() - 1).getBrowsePath());
    setPath(paths.get(paths.size() - 1).getRequestPath());
  }

  public void setRepositoryName(final int repositoryId) {
    this.repositoryId = checkNotNull(repositoryId);
  }

  @Override
  public String toString() {
    return "BrowseNode{" + "repositoryId=" + repositoryId + ", parentId=" + parentId
        + ", name=" + name + ", path=" + path + ", leaf=" + leaf + ", componentId='" + componentId + '\''
        + ", assetId='" + assetId + '\'' + '}';
  }
}
