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
package org.sonatype.nexus.rest.indextreeview;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * REST Response object received after request an item from the index browser tree, contains the typical 'data'
 * parameter, which is the tree item.
 *
 * @author cstamas
 * @since 2.7.0
 */
@XStreamAlias("indexBrowserTreeViewResponse")
public class IndexBrowserTreeViewResponseDTO
{
  /**
   * The tree node and its details.
   */
  private IndexBrowserTreeNodeDTO data;

  /**
   * Empty constructor, used along with XStreamRepresentation where you pass in a "fresh" and empty instance to
   * deserialize input over it.
   */
  public IndexBrowserTreeViewResponseDTO() {
    this.data = null;
  }

  /**
   * Constructor with TreeNode
   */
  public IndexBrowserTreeViewResponseDTO(final IndexBrowserTreeNodeDTO node) {
    if (node == null) {
      throw new NullPointerException("IndexBrowserTreeNodeDTO is null!");
    }
    this.data = node;
  }

  /**
   * Get the tree node and its details.
   *
   * @return IndexBrowserTreeNode
   */
  public IndexBrowserTreeNodeDTO getData() {
    return data;
  }
}
