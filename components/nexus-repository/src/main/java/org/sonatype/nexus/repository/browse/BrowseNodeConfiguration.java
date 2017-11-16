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
package org.sonatype.nexus.repository.browse;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.annotations.VisibleForTesting;

/**
 * Configuration options for browse tree
 *
 * @since 3.6
 */
@Named
public class BrowseNodeConfiguration
{
  /**
   * Configuration property for enabling browse trees
   */
  public static final String ENABLED = "nexus.browse.component.tree.enabled";

  private final boolean enabled;

  private final boolean automaticRebuild;

  private final int rebuildPageSize;

  private final int maxNodes;

  private final int maxHtmlNodes;

  private final int maxTruncateCount;

  private final int maxUpdateChildCount;

  @Inject
  public BrowseNodeConfiguration(@Named("${nexus.browse.component.tree.enabled:-false}") final boolean enabled,
                                 @Named("${nexus.browse.component.tree.automaticRebuild:-true}") final boolean automaticRebuild,
                                 @Named("${nexus.browse.component.tree.rebuildPageSize:-1000}") final int rebuildPageSize,
                                 @Named("${nexus.browse.component.tree.maxNodes:-10000}") final int maxNodes,
                                 @Named("${nexus.browse.component.tree.maxHtmlNodes:-10000}") final int maxHtmlNodes,
                                 @Named("${nexus.browse.component.tree.maxTruncateNodes:-1000}") final int maxTruncateCount,
                                 @Named("${nexus.browse.component.tree.maxUpdateChildNodes:-1000}") final int maxUpdateChildCount)
  {
    this.enabled = enabled;
    this.automaticRebuild = automaticRebuild;
    this.rebuildPageSize = rebuildPageSize;
    this.maxNodes = maxNodes;
    this.maxHtmlNodes = maxHtmlNodes;
    this.maxTruncateCount = maxTruncateCount;
    this.maxUpdateChildCount = maxUpdateChildCount;
  }

  @VisibleForTesting
  public BrowseNodeConfiguration() {
    this(true, true, 1000, 10000, 10000, 1000, 1000);
  }

  /**
   * The number of assets to retrieve at a time while rebuilding the browse tree
   */
  public int getRebuildPageSize() {
    return rebuildPageSize;
  }

  /**
   * The maximum number of nodes to display on a given level of a tree
   */
  public int getMaxNodes() {
    return maxNodes;
  }

  /**
   * @since 3.6.1
   * @return the maximum number of nodes to display in the browse html view (for a given level of the tree)
   */
  public int getMaxHtmlNodes() {
    return maxHtmlNodes;
  }

  /**
   * The maximum number of nodes to truncate in a transaction
   */
  public int getMaxTruncateCount() {
    return maxTruncateCount;
  }

  /**
   * The maximum number of nodes to update with children in a transaction
   */
  public int getMaxUpdateChildCount() {
    return maxUpdateChildCount;
  }

  /**
   * Whether the tree should be automatically rebuilt if the number of assets is different from the number of leaves in
   * the browse tree on startup
   */
  public boolean isAutomaticRebuildEnabled() {
    return enabled && automaticRebuild;
  }

  /**
   * Whether the browse tree is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
}
