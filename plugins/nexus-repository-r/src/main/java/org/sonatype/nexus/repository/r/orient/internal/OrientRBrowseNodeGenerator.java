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
package org.sonatype.nexus.repository.r.orient.internal;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.r.RFormat;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * R places components at the same level as their assets.
 *
 * @since 3.28
 */
@Singleton
@Named(RFormat.NAME)
public class OrientRBrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component)
  {
    checkNotNull(asset);
    if (component != null) {
      List<BrowsePaths> paths = computeComponentPaths(asset, component);
      String lastSegment = lastSegment(asset.name());
      BrowsePaths.appendPath(paths, lastSegment);
      return paths;
    }
    else {
      return super.computeAssetPaths(asset, null);
    }
  }

  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, final Component component)
  {
    checkNotNull(asset);
    checkNotNull(component);
    List<BrowsePaths> browsePaths = super.computeComponentPaths(asset, null);
    if (!browsePaths.get(browsePaths.size() - 1).getDisplayName().equals(component.name())) {
      BrowsePaths.appendPath(browsePaths, component.name(), computeRequestPath(browsePaths, component.name()));
    }
    BrowsePaths.appendPath(browsePaths, component.version(), computeRequestPath(browsePaths, component.version()));
    return browsePaths;
  }

  private String computeRequestPath(List<BrowsePaths> browsePaths, String path) {
    return browsePaths.get(browsePaths.size() - 1).getRequestPath() + path + "/";
  }
}
