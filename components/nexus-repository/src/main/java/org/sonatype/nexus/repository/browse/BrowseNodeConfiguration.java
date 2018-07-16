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

import org.sonatype.goodies.common.Time;

import com.google.common.annotations.VisibleForTesting;

import static org.sonatype.goodies.common.Time.seconds;

/**
 * Configuration options for browse tree
 *
 * @since 3.6
 */
@Named
public class BrowseNodeConfiguration
{
  private final boolean automaticRebuild;

  private final int rebuildPageSize;

  private final int deletePageSize;

  private final int maxNodes;

  private final int maxHtmlNodes;

  private final Time queryTimeout;

  @Inject
  public BrowseNodeConfiguration(@Named("${nexus.browse.component.tree.automaticRebuild:-true}") final boolean automaticRebuild,
                                 @Named("${nexus.browse.component.tree.rebuildPageSize:-1000}") final int rebuildPageSize,
                                 @Named("${nexus.browse.component.tree.deletePageSize:-1000}") final int deletePageSize,
                                 @Named("${nexus.browse.component.tree.maxNodes:-10000}") final int maxNodes,
                                 @Named("${nexus.browse.component.tree.maxHtmlNodes:-10000}") final int maxHtmlNodes,
                                 @Named("${nexus.browse.component.tree.queryTimeout:-59s}") final Time queryTimeout)
  {
    this.automaticRebuild = automaticRebuild;
    this.rebuildPageSize = rebuildPageSize;
    this.deletePageSize = deletePageSize;
    this.maxNodes = maxNodes;
    this.maxHtmlNodes = maxHtmlNodes;
    this.queryTimeout = queryTimeout;
  }

  @VisibleForTesting
  public BrowseNodeConfiguration() {
    this(true, 1000, 1000, 10_000, 10_000, seconds(0));
  }

  /**
   * The number of assets to retrieve at a time while rebuilding the browse tree
   */
  public int getRebuildPageSize() {
    return rebuildPageSize;
  }

  /**
   * The number of nodes to delete at a time while truncating the browse tree
   *
   * @since 3.7
   */
  public int getDeletePageSize() {
    return deletePageSize;
  }

  /**
   * The maximum number of nodes to display on a given level of a tree
   */
  public int getMaxNodes() {
    return maxNodes;
  }

  /**
   * The maximum number of nodes to display in the browse html view (for a given level of the tree)
   *
   * @since 3.6.1
   */
  public int getMaxHtmlNodes() {
    return maxHtmlNodes;
  }

  /**
   * How long to wait for filtered subtree queries to complete before returning a potentially truncated set of results
   *
   * @since 3.7
   */
  public Time getQueryTimeout() {
    return queryTimeout;
  }

  /**
   * Whether the tree should be automatically rebuilt if the number of assets is different from the number of leaves in
   * the browse tree on startup
   */
  public boolean isAutomaticRebuildEnabled() {
    return automaticRebuild;
  }
}
