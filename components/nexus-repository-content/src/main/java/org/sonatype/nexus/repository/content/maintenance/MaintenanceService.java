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
package org.sonatype.nexus.repository.content.maintenance;

import java.util.List;
import java.util.Set;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

/**
 * A service for executing maintenance operations (such as a 'delete') on assets in a repository.
 *
 * @since 3.26
 */
public interface MaintenanceService
{
  /**
   * Delete an asset in the specified repository.
   *
   * @return Set of asset names that were removed
   */
  Set<String> deleteAsset(Repository repository, Asset asset);

  /**
   * Delete a component in the specified repository.
   *
   * @return Set of asset names that were removed
   */
  Set<String> deleteComponent(Repository repository, Component component);

  /**
   * Delete a path in the specified repository.
   */
  void deleteFolder(Repository repository, String path);

  /**
   * Check if a component can be deleted.
   *
   * @return true if the component can be deleted, false otherwise
   */
  boolean canDeleteComponent(Repository repository, Component component);

  /**
   * Check if an asset can be deleted.
   *
   * @return true if the asset can be deleted, false otherwise
   */
  boolean canDeleteAsset(Repository repository, Asset asset);

  /**
   * Check if user can potentially delete any asset in a given path.
   *
   * @return true if the path can be deleted, false otherwise
   */
  boolean canDeleteFolder(Repository repository, String path);

  /**
   * Deletes a list of assets from the corresponding repository.
   *
   * @param repository Repository to delete assets from
   * @param assetIds List of internal asset IDs to delete
   * @return Set of assets that were removed
   */
  Set<String> deleteAssets(Repository repository, List<Integer> assetIds);
}
