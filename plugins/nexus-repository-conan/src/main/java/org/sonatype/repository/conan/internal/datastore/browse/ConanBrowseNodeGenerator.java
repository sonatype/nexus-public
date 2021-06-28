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
package org.sonatype.repository.conan.internal.datastore.browse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.repository.conan.internal.ConanFormat;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.node.BrowsePathBuilder.fromPaths;
import static org.sonatype.repository.conan.internal.common.ConanBrowseNodeGeneratorHelper.assetSegment;

/**
 * @since 3.32
 */
@Singleton
@Named(ConanFormat.NAME)
public class ConanBrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    final List<String> componentList = new ArrayList<>();
    asset.component().ifPresent(component -> {
      componentList.add(component.namespace());
      componentList.add(component.name());
      componentList.add(component.version());
    });

    return fromPaths(componentList, true);
  }

  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    checkNotNull(asset);
    if (asset.component().isPresent()) {
      List<BrowsePath> browsePaths = computeComponentPaths(asset);
      browsePaths.addAll(fromPaths(assetSegment(asset.path()), false));
      return browsePaths;
    }
    else {
      return super.computeAssetPaths(asset);
    }
  }
}
