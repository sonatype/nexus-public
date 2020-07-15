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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.BrowsePathBuilder;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;

import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Component-led layout based on group, name, and version; places assets one level below their components.
 *
 * @since 3.24
 */
@Singleton
@Named(DefaultBrowseNodeGenerator.NAME)
public class DefaultBrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  public static final String NAME = "default";

  /**
   * @return componentPath/assetName if asset has an component, otherwise assetPath
   */
  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    checkNotNull(asset);

    if (asset.component().isPresent()) {
      // place asset one level beneath component, using the asset's name as the display name
      List<BrowsePath> paths = computeComponentPaths(asset);
      String lastSegment = lastSegment(asset.path());
      BrowsePathBuilder.appendPath(paths, lastSegment);
      return paths;
    }
    else {
      return super.computeAssetPaths(asset);
    }
  }

  /**
   * @return [componentGroup/]componentName/[componentVersion/]
   */
  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    checkNotNull(asset);

    List<String> paths = new ArrayList<>();

    checkArgument(asset.component().isPresent());
    Component component = asset.component().get();
    if (!Strings2.isBlank(component.namespace())) {
      paths.add(component.namespace());
    }
    paths.add(component.name());
    if (!Strings2.isBlank(component.version())) {
      paths.add(component.version());
    }
    return BrowsePathBuilder.fromPaths(paths, true);
  }
}
