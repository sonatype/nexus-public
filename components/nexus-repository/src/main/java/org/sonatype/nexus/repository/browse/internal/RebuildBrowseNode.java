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
package org.sonatype.nexus.repository.browse.internal;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.BrowseNode;

/*
 * Internal class used while rebuilding the browse node tree.
 *
 * @since 3.6
 */
class RebuildBrowseNode
{
  private RebuildBrowseNode parentNode;

  private Map<String, RebuildBrowseNode> children = new HashMap<>();

  private String name;

  private EntityId assetId;

  private EntityId componentId;

  private BrowseNode browseNode;

  private String assetNameLowercase;

  public RebuildBrowseNode getParentNode() {
    return parentNode;
  }

  public RebuildBrowseNode withParentNode(final RebuildBrowseNode parentNode) {
    this.parentNode = parentNode;
    return this;
  }

  public Map<String, RebuildBrowseNode> getChildren() {
    return children;
  }

  public String getName() {
    return name;
  }

  public RebuildBrowseNode withName(final String name) {
    this.name = name;
    return this;
  }

  public EntityId getAssetId() {
    return assetId;
  }

  public RebuildBrowseNode withAssetId(final EntityId assetId) {
    this.assetId = assetId;
    return this;
  }

  public EntityId getComponentId() {
    return componentId;
  }

  public RebuildBrowseNode withComponentId(final EntityId componentId) {
    this.componentId = componentId;
    return this;
  }

  public BrowseNode getBrowseNode() {
    return browseNode;
  }

  public RebuildBrowseNode withBrowseNode(final BrowseNode browseNode) {
    this.browseNode = browseNode;
    return this;
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
  public RebuildBrowseNode withAssetNameLowercase(@Nullable final String assetNameLowercase) {
    this.assetNameLowercase = assetNameLowercase;
    return this;
  }
}
