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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

/**
 * Implementation of a {@link BrowseNodeGenerator} which creates a folder structure from the path of the asset by
 * splitting on /.
 *
 * @since 3.6
 */
public abstract class AbstractPathBrowseNodeGenerator
  implements BrowseNodeGenerator
{
  /**
   * Construct the asset path by splitting the asset name on the `/` character.
   *
   * @return the path to the asset
   */
  @Override
  public List<String> computeAssetPath(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);

    String assetName = asset.name();
    if (assetName.startsWith("/")) {
      assetName = assetName.substring(1);
    }
    return asList(assetName.split("/"));
  }

  /**
   * Same path as the asset
   *
   * @return the path to the asset
   */
  @Override
  public List<String> computeComponentPath(final Asset asset, final Component component) {
    return (component != null) ? computeAssetPath(asset, component) : Collections.emptyList();
  }
}
