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
package org.sonatype.nexus.repository.p2.orient.internal;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNode;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.browse.BrowsePaths.fromPaths;
import static org.sonatype.nexus.repository.p2.internal.browse.P2BrowseNodeGeneratorHelper.computeComponentPath;
import static org.sonatype.nexus.repository.p2.internal.browse.P2BrowseNodeGeneratorHelper.splitPath;

/**
 * @since 3.28
 */
@Singleton
@Named(P2Format.NAME)
@Priority(Integer.MAX_VALUE)
public class OrientP2BrowseNodeGenerator
    implements BrowseNodeGenerator
{
  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component) {
    List<BrowsePaths> browsePaths = computeComponentPaths(asset, component);

    List<String> assetPaths = splitPath(asset.name());
    browsePaths.addAll(fromPaths(singletonList(assetPaths.get(assetPaths.size() - 1)), false));
    return browsePaths;
  }

  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, @Nullable final Component component) {
    List<String> componentPaths = computeComponentPath(
        splitPath(asset.name()),
        ofNullable(component).map(MetadataNode::name),
        ofNullable(component).map(Component::version)
    );
    return fromPaths(componentPaths,true);
  }

}
