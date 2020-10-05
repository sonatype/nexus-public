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
package org.sonatype.nexus.repository.p2.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Splitter;

/**
 * @since 3.28
 */
@Singleton
@Named(P2Format.NAME)
public class P2BrowseNodeGenerator
    implements BrowseNodeGenerator
{
  private final Set<String> knownSubDirectories;

  @Inject
  public P2BrowseNodeGenerator() {
    Set<String> knownFirstSegments = new HashSet<>();
    knownFirstSegments.add("binary");
    knownFirstSegments.add("features");
    knownFirstSegments.add("plugins");

    knownSubDirectories = Collections.unmodifiableSet(knownFirstSegments);
  }

  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component) {
    List<String> assetPaths = splitPath(asset.name());

    List<BrowsePaths> browsePaths = computeComponentPath(assetPaths, component);

    browsePaths.addAll(BrowsePaths.fromPaths(Collections.singletonList(assetPaths.get(assetPaths.size() - 1)), false));
    return browsePaths;
  }

  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, @Nullable final Component component) {
    return computeComponentPath(splitPath(asset.name()), component);
  }

  private List<BrowsePaths> computeComponentPath(final List<String> assetPath, @Nullable final Component component) {
    List<String> pathParts = new ArrayList<>();
    if (assetPath.size() > 1) {
      pathParts.add(assetPath.get(0));
    }

    if (component != null) {
      if (!knownSubDirectories.contains(assetPath.get(0))) {
        pathParts.add(assetPath.get(1));
      }

      pathParts.addAll(Splitter.on('.').omitEmptyStrings().splitToList(component.name()));
      pathParts.add(component.version());
    }
    return BrowsePaths.fromPaths(pathParts, true);
  }

  private List<String> splitPath(final String path) {
    return Splitter.on('/').omitEmptyStrings().splitToList(path);
  }
}
