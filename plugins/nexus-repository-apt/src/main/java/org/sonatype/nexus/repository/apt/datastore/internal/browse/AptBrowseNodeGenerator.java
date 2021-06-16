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
package org.sonatype.nexus.repository.apt.datastore.internal.browse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.BrowsePathBuilder;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.DefaultBrowseNodeGenerator;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH;

/**
 * @since 3.31
 */
@Named(AptFormat.NAME)
@Singleton
public class AptBrowseNodeGenerator
    extends DefaultBrowseNodeGenerator
{
  private static final String PACKAGES_PATH = "packages";

  private static final String METADATA_PATH = "metadata";

  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    Component component = asset.component().orElse(null);
    if (component != null) {
      List<BrowsePath> path = computeComponentPaths(asset);
      BrowsePathBuilder.appendPath(path, asset.path().substring(1));
      return path;
    }

    List<String> pathParts = new ArrayList<>();
    String path = asset.path();
    if (path.endsWith(".deb") || path.endsWith(".udeb")) {
      pathParts.add(PACKAGES_PATH);
    }
    else if (!path.startsWith("/snapshots")) {
      pathParts.add(METADATA_PATH);
    }

    pathParts.addAll(Splitter.on(SLASH).omitEmptyStrings().splitToList(path));

    return BrowsePathBuilder.fromPaths(pathParts, true);
  }

  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    checkNotNull(asset);

    checkArgument(asset.component().isPresent());
    Component component = asset.component().get();

    List<String> pathParts = new ArrayList<>();
    pathParts.add(PACKAGES_PATH);
    pathParts.add(component.name().substring(0, 1).toLowerCase());
    pathParts.add(component.name());
    pathParts.add(component.version());
    pathParts.add(component.namespace());
    pathParts.add(component.name());
    return BrowsePathBuilder.fromPaths(pathParts, true);
  }
}
