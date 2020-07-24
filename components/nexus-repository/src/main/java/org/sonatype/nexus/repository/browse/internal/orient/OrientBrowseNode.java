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
package org.sonatype.nexus.repository.browse.internal.orient;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An orientdb backed implementation of {@link BrowseNode}
 *
 * @since 3.6
 */
public class OrientBrowseNode
    extends AbstractEntity
    implements BrowseNode<EntityId>
{
  private String repositoryName;

  private String format;

  private String path;

  private String parentPath;

  private String name;

  private boolean leaf;

  @Nullable
  private String packageUrl;

  @Nullable
  private EntityId componentId;

  @Nullable
  private EntityId assetId;

  public String getRepositoryName() {
    return require(repositoryName, BrowseNodeEntityAdapter.P_REPOSITORY_NAME);
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = checkNotNull(repositoryName);
  }

  public String getFormat() {
    return require(format, BrowseNodeEntityAdapter.P_FORMAT);
  }

  public void setFormat(final String format) {
    this.format = checkNotNull(format);
  }

  /**
   * @since 3.18
   */
  @Override
  public String getPath() {
    return require(path, BrowseNodeEntityAdapter.P_PATH);
  }

  public void setPath(final String path) {
    this.path = checkNotNull(path);
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
  @Override
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
  @Override
  public boolean isLeaf() {
    return leaf;
  }

  /**
   * @since 3.6.1
   */
  public void setLeaf(final boolean leaf) {
    this.leaf = leaf;
  }

  @Override
  @Nullable
  public EntityId getComponentId() {
    return componentId;
  }

  public void setComponentId(final EntityId componentId) {
    this.componentId = checkNotNull(componentId);
  }

  @Override
  @Nullable
  public EntityId getAssetId() {
    return assetId;
  }

  public void setAssetId(final EntityId assetId) {
    this.assetId = checkNotNull(assetId);
  }

  public void setPaths(final List<? extends BrowsePath> paths) {
    setParentPath(joinPath(
        paths.subList(0, paths.size() - 1).stream().map(BrowsePath::getBrowsePath).collect(Collectors.toList())));
    setName(paths.get(paths.size() - 1).getBrowsePath());
    setPath(paths.get(paths.size() - 1).getRequestPath());
  }

  private static String joinPath(final List<String> path) {
    StringBuilder buf = new StringBuilder("/");
    path.forEach(s -> buf.append(s).append("/"));
    return buf.toString();
  }

  private <V> V require(final V value, final String name) {
    checkState(value != null, "Missing property: %s", name);
    return value;
  }

  @Override
  public String toString() {
    return "BrowseNode{" + "repositoryName=" + repositoryName + ", format=" + format + ", parentPath=" + parentPath
        + ", name=" + name + ", path=" + path + ", leaf=" + leaf + ", componentId='" + componentId + '\''
        + ", assetId='" + assetId + '\'' + '}';
  }

  @Nullable
  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
  }
}
