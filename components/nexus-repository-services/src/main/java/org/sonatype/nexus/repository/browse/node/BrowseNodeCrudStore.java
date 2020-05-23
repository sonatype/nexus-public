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

import java.util.List;

/**
 * Store providing access to the browse tree for assets & components.
 *
 * @since 3.7
 */
public interface BrowseNodeCrudStore<ASSET, COMPONENT>
{
  /**
   * Creates a {@link BrowseNode} for the given asset.
   */
  void createAssetNode(String repositoryName, String format, List<BrowsePath> paths, ASSET asset);

  /**
   * Creates a {@link BrowseNode} for the given component.
   */
  void createComponentNode(String repositoryName, String format, List<BrowsePath> paths, COMPONENT component);

  boolean assetNodeExists(ASSET asset);

  /**
   * Deletes the asset's {@link BrowseNode}.
   */
  void deleteAssetNode(ASSET asset);

  /**
   * Deletes the component's {@link BrowseNode}.
   */
  void deleteComponentNode(COMPONENT component);

  /**
   * Deletes all {@link BrowseNode}s belonging to the given repository.
   */
  void deleteByRepository(String repositoryName);
}
