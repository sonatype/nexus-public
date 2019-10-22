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
package org.sonatype.nexus.repository.cocoapods.internal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.19
 */
@Singleton
@Named(CocoapodsFormat.NAME)
public class CocoapodsBrowseNodeGenerator
    extends ComponentSupport
    implements BrowseNodeGenerator
{
  private static final String POD_PATH = "pods";

  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);
    if (component != null) {
      List<BrowsePaths> paths = computeComponentPaths(asset, component);
      String viewName = cutResourceParams(lastSegment(asset.name()));
      BrowsePaths.appendPath(paths, viewName);
      return paths;
    }
    else {
      List<String> nameParts = Splitter.on('/').omitEmptyStrings().splitToList(cutResourceParams(asset.name()));
      return BrowsePaths.fromPaths(nameParts, false);
    }
  }

  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, final Component component) {
    List<String> paths = new ArrayList<>();

    // Only pods have components so "pods/" path should be created
    paths.add(POD_PATH);

    if (!Strings2.isBlank(component.group())) {
      paths.add(component.group());
    }
    paths.add(component.name());
    if (!Strings2.isBlank(component.version())) {
      paths.add(component.version());
    }

    return BrowsePaths.fromPaths(paths, true);
  }

  private String cutResourceParams(final String lastSegment) {
    int ind = lastSegment.indexOf('?');
    return ind == -1 ? lastSegment : lastSegment.substring(0, ind);
  }
}
