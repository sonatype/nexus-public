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
package org.sonatype.nexus.repository.content.facet;

import java.util.Set;
import java.util.function.BooleanSupplier;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.task.DeletionProgress;

/**
 * @since 3.24
 */
@Facet.Exposed
public interface ContentMaintenanceFacet
    extends Facet
{
  /**
   * Deletes a component from storage.
   *
   * @return name(s) of removed asset(s)
   */
  Set<String> deleteComponent(Component component);

  /**
   * Deletes a component and maybe the associated blobs.
   *
   * @param component the component to delete
   * @param deleteBlobs should blob deletion be requested
   * @return name(s) of removed asset(s)
   */
  Set<String> deleteComponent(Component component, boolean deleteBlobs);

  /**
   * Deletes an asset from storage.
   *
   * @return name of removed asset(s)
   */
  Set<String> deleteAsset(Asset asset);

  /**
   * Deletes an asset and maybe the associated blob.
   *
   * @param asset the asset to delete
   * @param deleteBlob should blob deletion be requested
   * @return name of removed asset(s)
   */
  Set<String> deleteAsset(Asset asset, boolean deleteBlob);

  /**
   * Deletes a list of components
   *
   * @param components list of components to delete
   * @param cancelledCheck check for cancellation
   * @param batchSize number of components to commit at a time
   * @return {@link DeletionProgress} for the current deletion attempt
   */
  DeletionProgress deleteComponents(Iterable<Integer> components, BooleanSupplier cancelledCheck, int batchSize);

  /**
   * Runs at the end of deleteComponents.
   */
  void after();
}
