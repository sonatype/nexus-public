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
package org.sonatype.nexus.repository.browse.node;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.VersionComparator;

/**
 * Sort using VersionComparator when dealing with two components, fall back to node name when dealing
 * with any other comparison of the same type (component/asset/folder), finally fall back to node type
 *
 * @since 3.13
 */
@Named(value = DefaultBrowseNodeComparator.NAME)
public class DefaultBrowseNodeComparator
    implements BrowseNodeComparator
{
  public static final String NAME = "default";

  private final VersionComparator versionComparator;

  public static final int NODE_PRIORITY_COMPONENT = 1;
  public static final int NODE_PRIORITY_FOLDER = 2;
  public static final int NODE_PRIORITY_ASSET = 3;

  @Inject
  public DefaultBrowseNodeComparator(final VersionComparator versionComparator) {
    this.versionComparator = versionComparator;
  }

  @Override
  public int compare(final BrowseNode o1, final BrowseNode o2) {
    int o1Priority = getNodePriorityLevel(o1);
    int o2Priority = getNodePriorityLevel(o2);

    if (o1Priority == NODE_PRIORITY_COMPONENT && o2Priority == NODE_PRIORITY_COMPONENT) {
      try {
        return versionComparator.compare(o1.getName(), o2.getName());
      }
      catch (IllegalArgumentException e) { //NOSONAR
        return 0;
      }
    }

    if (o1Priority == o2Priority) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }

    return Integer.compare(o1Priority, o2Priority);
  }

  /**
   * Returns an integer representing the priority level of the BrowseNode for comparison.
   * Priority is determined by the presence of component or asset identifiers, with folder nodes
   * having the highest priority.
   *
   * @param browseNode browseNode the node to evaluate for priority
   * @return an integer indicating the BrowseNode's priority level. A component node has the lowest priority,
   * followed by asset nodes, then folder nodes.
   */
  public static int getNodePriorityLevel(final BrowseNode browseNode) {
    if (browseNode.getComponentId() != null) {
      return NODE_PRIORITY_COMPONENT;
    }

    if (browseNode.getAssetId() != null) {
      return NODE_PRIORITY_ASSET;
    }

    return NODE_PRIORITY_FOLDER;
  }
}
