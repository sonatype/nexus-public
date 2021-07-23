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
package org.sonatype.nexus.repository.p2.datastore.internal.browse;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.p2.internal.P2Format;

import static org.sonatype.nexus.repository.browse.node.BrowsePathBuilder.fromPaths;
import static org.sonatype.nexus.repository.p2.internal.browse.P2BrowseNodeGeneratorHelper.computeComponentPath;
import static org.sonatype.nexus.repository.p2.internal.browse.P2BrowseNodeGeneratorHelper.splitPath;

/**
 * @since 3.next
 */
@Singleton
@Named(P2Format.NAME)
public class P2BrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    List<BrowsePath> browsePaths = computeComponentPaths(asset);

    List<String> assetPaths = splitPath(asset.path());
        browsePaths.addAll(fromPaths(assetPaths.subList(assetPaths.size() - 1, assetPaths.size()), true));
    return browsePaths;
  }

  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    return fromPaths(computeComponentPath(splitPath(asset.path()),
        asset.component().map(Component::name),
        asset.component().map(Component::version)), true);
  }
}
