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

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Asset-led layout that assumes the asset name is its path and components have the same path as their assets.
 *
 * @since 3.7
 */
public abstract class AssetPathBrowseNodeGenerator
    implements BrowseNodeGenerator
{
  /**
   * Construct the asset path by splitting the asset name on the `/` character.
   */
  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);

    List<String> nameParts = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    return BrowsePaths.fromPaths(nameParts, false);
  }

  /**
   * Component path is same as the asset path.
   */
  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, final Component component) {
    return computeAssetPaths(asset, component);
  }
}
