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
package org.sonatype.nexus.repository.content.browse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.browse.node.BrowseNode;

/**
 * Browse {@link Facet} that maintains the browse tree.
 *
 * @since 3.26
 */
@Facet.Exposed
public interface BrowseFacet
    extends Facet
{
  /**
   * Retrieves the browse nodes directly under the given hierarchical display path.
   *
   * @param displayPath the hierarchical path leading up to the browse nodes
   * @param limit when positive limits the number of browse nodes returned
   * @param filter optional filter to apply to the browse nodes
   * @param filterParams parameter map for the optional filter
   * @return browse nodes found directly under the display path
   */
  List<BrowseNode> getByDisplayPath(
      List<String> displayPath,
      int limit,
      @Nullable String filter,
      @Nullable Map<String, Object> filterParams);

  /**
   * Adds the necessary browse nodes leading up to these assets and their components.
   *
   * @param assetIds the assets to add
   */
  void addPathsToAssets(Collection<EntityId> assetIds);

  /**
   * Trims any dangling browse nodes from this repository.
   */
  void trimBrowseNodes();

  /**
   * Rebuilds the browse node tree for this repository.
   */
  void rebuildBrowseNodes(Consumer<String> progressUpdater);

  /**
   * Deletes a browse node by its asset internal id and path.
   *
   * @param internalAssetId the asset internal id
   * @param path            the path
   */
  void deleteByAssetIdAndPath(Integer internalAssetId, String path);

  /**
   * Deletes a browse node by its node id.
   *
   * @param nodeId the node id
   */
  void deleteByNodeId(Long nodeId);

  /**
   * Retrieves the browse node by its request path.
   *
   * @param requestPath the request path
   * @return the browse node if found
   */
  Optional<BrowseNode> getByRequestPath(String requestPath);
}
