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

import java.util.List;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;

/**
 * Component-led layout that places components one level above their assets.
 *
 * @since 3.24
 */
public abstract class ComponentPathBrowseNodeGenerator
    extends AssetPathBrowseNodeGenerator
{
  @Override
  public boolean hasMultipleAssetsPerComponent() {
    return true;
  }

  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    List<BrowsePath> assetPaths = computeAssetPaths(asset);
    return assetPaths.subList(0, assetPaths.size() - 1);
  }
}
