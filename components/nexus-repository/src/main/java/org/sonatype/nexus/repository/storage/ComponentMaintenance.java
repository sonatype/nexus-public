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

import java.util.Set;
import java.util.function.BooleanSupplier;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl.DeletionProgress;

/**
 * Exposes manual component maintenance operations.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface ComponentMaintenance
    extends Facet
{
  /**
   * Deletes a component from storage.
   *
   * @return name(s) of removed asset(s)
   */
  Set<String> deleteComponent(EntityId componentId);

  /**
   * Deletes a component and maybe the associated blobs.
   *
   * @param componentId entity id of the component to delete
   * @param deleteBlobs should blob deletion be requested
   * @return name(s) of removed asset(s)
   *
   * @since 3.9
   */
  Set<String> deleteComponent(EntityId componentId, boolean deleteBlobs);

  /**
   * Deletes an asset from storage.
   * @return name of removed asset(s)
   */
  Set<String> deleteAsset(EntityId assetId);

  /**
   * Deletes an asset and maybe the associated blob.
   *
   * @param assetId entity id of the asset to delete
   * @param deleteBlob should blob deletion be requested
   * @return name of removed asset(s)
   * @since 3.9
   */
  Set<String> deleteAsset(EntityId assetId, boolean deleteBlob);

  /**
   * Deletes a list of components
   * 
   * @param components list of components to delete
   * @param cancelledCheck check for cancellation
   * @param batchSize number of components to commit at a time
   * @return {@link DeletionProgress} for the current deletion attempt
   *
   * @since 3.next
   */
  DeletionProgress deleteComponents(Iterable<EntityId> components, BooleanSupplier cancelledCheck, int batchSize);

  /**
   * Runs at the end of deleteComponents.
   *
   * @since 3.14
   */
  void after();

}
