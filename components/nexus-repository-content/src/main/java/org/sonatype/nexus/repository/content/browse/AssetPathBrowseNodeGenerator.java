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
import org.sonatype.nexus.repository.browse.node.BrowsePathBuilder;
import org.sonatype.nexus.repository.content.Asset;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH_CHAR;

/**
 * Asset-led layout that assumes that components have the same path as their assets.
 *
 * @since 3.24
 */
public abstract class AssetPathBrowseNodeGenerator
    implements BrowseNodeGenerator
{
  @Override
  public boolean hasMultipleAssetsPerComponent() {
    return false;
  }

  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    checkNotNull(asset);
    List<String> pathSegments = Splitter.on(SLASH_CHAR).omitEmptyStrings().splitToList(asset.path());
    return BrowsePathBuilder.fromPaths(pathSegments, false);
  }

  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    return computeAssetPaths(asset);
  }
}
