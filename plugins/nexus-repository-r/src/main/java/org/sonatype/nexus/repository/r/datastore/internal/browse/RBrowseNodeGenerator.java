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
package org.sonatype.nexus.repository.r.datastore.internal.browse;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.BrowsePathBuilder;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.DefaultBrowseNodeGenerator;
import org.sonatype.nexus.repository.r.RFormat;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH_CHAR;

/**
 * R places components at the same level as their assets.
 *
 * @since 3.32
 */
@Singleton
@Named(RFormat.NAME)
public class RBrowseNodeGenerator
    extends DefaultBrowseNodeGenerator
{
  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    checkNotNull(asset);

    if (asset.component().isPresent()) {
      List<BrowsePath> paths = computeComponentPaths(asset);
      String lastSegment = lastSegment(asset.path());
      BrowsePathBuilder.appendPath(paths, lastSegment);
      return paths;
    }
    else {
      return super.computeAssetPaths(asset);
    }
  }

  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    checkNotNull(asset);

    Component component = asset.component().orElseThrow(() ->
        new RuntimeException("Component is not presented for asset: {}" + asset.path()));
    List<String> assetPaths = Splitter.on(SLASH_CHAR).omitEmptyStrings().splitToList(asset.path());
    List<BrowsePath> browsePaths = BrowsePathBuilder.fromPaths(assetPaths.subList(0, assetPaths.size() - 1), true);
    if (!browsePaths.get(browsePaths.size() - 1).getDisplayName().equals(component.name())) {
      BrowsePathBuilder.appendPath(browsePaths, component.name(), computeRequestPath(browsePaths, component.name()));
    }
    String requestPath = computeRequestPath(browsePaths, component.version());
    BrowsePathBuilder.appendPath(browsePaths, component.version(), requestPath);
    return browsePaths;
  }

  private String computeRequestPath(List<BrowsePath> browsePaths, String path) {
    return browsePaths.get(browsePaths.size() - 1).getRequestPath() + path + SLASH;
  }
}
