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
package org.sonatype.nexus.repository.filter.export;

import org.sonatype.nexus.repository.storage.Asset;

/**
 * Used to determine {@link Asset} specifics during export.
 */
public interface OrientExportAssetFilter
{
  /**
   * Determines whether the export of the asset should be skipped for the provided {@link Asset}
   *
   * @param asset the {@link Asset} to use in the decision
   * @return true if the asset export should be skipped
   */
  boolean shouldSkipAsset(Asset asset);

  /**
   * Determines whether the export of the attributes should be skipped for the provided {@link Asset}
   *
   * @param asset the {@link Asset} to use in the decision
   * @return true if the attribute export should be skipped
   */
  default boolean shouldSkipAttributes(Asset asset) {
    return false;
  }

  /**
   * Provides a calculated export path for the provided {@link Asset}
   *
   * @param asset the {@link Asset} to calculate the export path for
   * @return the calculated export path
   */
  default String getAssetExportName(Asset asset) {
    return asset.name();
  }
}
