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
package org.sonatype.nexus.coreui;

import java.util.List;
import java.util.Set;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;

/**
 * Helper for {@link ComponentComponent}.
 *
 * @since 3.26
 */
public interface ComponentHelper
{
  /**
   * Fetch the assets under the given component.
   */
  List<AssetXO> readComponentAssets(Repository repository, ComponentXO componentXO);

  /**
   * Preview the affect of the given JEXL on visible assets.
   */
  PageResult<AssetXO> previewAssets(
      RepositorySelector repositorySelector,
      List<Repository> selectedRepositories,
      String jexlExpression,
      QueryOptions queryOptions);

  /**
   * Fetch the component model with this external id.
   */
  ComponentXO readComponent(Repository repository, EntityId componentId);

  /**
   * Do we have enough permissions to delete this component?
   */
  boolean canDeleteComponent(Repository repository, ComponentXO componentXO);

  /**
   * Delete this component and any related assets and return the deleted paths.
   */
  Set<String> deleteComponent(Repository repository, ComponentXO componentXO);

  /**
   * Fetch the asset model with this external id.
   */
  AssetXO readAsset(Repository repository, EntityId assetId);

  /**
   * Do we have enough permissions to delete this asset?
   */
  boolean canDeleteAsset(Repository repository, EntityId assetId);

  /**
   * Delete this asset and any related assets and return the deleted paths.
   */
  Set<String> deleteAsset(Repository repository, EntityId assetId);

  /**
   * Do we have enough permissions to delete this folder?
   */
  boolean canDeleteFolder(Repository repository, String path);

  /**
   * Delete all components and assets under this folder.
   */
  void deleteFolder(Repository repository, String path);
}
