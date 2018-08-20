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
package org.sonatype.nexus.repository.assetdownloadcount;

import org.joda.time.DateTime;

/**
 * Store maintaining counts of downloads of assets, per repository
 *
 * @since 3.3
 */
public interface AssetDownloadCountStore
{

  String CACHE_NAME = "assetDownloadCountStoreCache";

  /**
   * Get the count of downloads of an asset for the specified day
   */
  long getDailyCount(final String repositoryName, final String assetName, final DateTime date);

  /**
   * Get the total number of downloads in the last thirty days
   */
  long getLastThirtyDays(final String repositoryName, final String assetName);

  /**
   * Get the count of downloads of an asset for the specified month
   */
  long getMonthlyCount(final String repositoryName, final String assetName, final DateTime date);

  /**
   * Get the counts for the last X days
   */
  long[] getDailyCounts(final String repositoryName, final String assetName);

  /**
   * Get the counts for the last X months
   */
  long[] getMonthlyCounts(final String repositoryName, final String assetName);

  /**
   * Increment the daily/monthly count of an asset
   */
  void incrementCount(final String repositoryName, final String assetName);

  /**
   * Set vulnerable count data for a specified date
   */
  void setMonthlyVulnerableCount(final String repositoryName,
                                 final DateTime date,
                                 final long count);

  /**
   * Set count data for a specified date
   */
  void setMonthlyCount(final String repositoryName,
                       final DateTime date,
                       final long count);

  /**
   * Get vulnerable download count data for each of the last 12 months
   */
  long[] getMonthlyVulnerableCounts(final String repositoryName);

  /**
   * Get download count data for each of the last 12 months
   */
  long[] getMonthlyCounts(final String repositoryName);

  /**
   * Check if the feature is enabled
   */
  boolean isEnabled();
}
