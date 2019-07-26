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
package org.sonatype.nexus.repository.browse.internal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Component-led layout based on group, name, and version; places assets one level below their components.
 *
 * @since 3.6
 */
@Singleton
@Named
public class DefaultBrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  /**
   * @return componentPath/lastSegment(assetPath) if the component was not null, otherwise assetPath
   */
  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);

    if (component != null) {
      List<BrowsePaths> paths = computeComponentPaths(asset, component);
      String lastSegment = lastSegment(asset.name());
      BrowsePaths.appendPath(paths, lastSegment);
      return paths;
    }
    else {
      return super.computeAssetPaths(asset, component);
    }
  }

  /**
   * @return [componentGroup]/componentName/[componentVersion]
   */
  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, final Component component) {
    List<String> paths = new ArrayList<>();

    if (!Strings2.isBlank(component.group())) {
      paths.add(component.group());
    }
    paths.add(component.name());
    if (!Strings2.isBlank(component.version())) {
      paths.add(component.version());
    }
    return BrowsePaths.fromPaths(paths, true);
  }
}
