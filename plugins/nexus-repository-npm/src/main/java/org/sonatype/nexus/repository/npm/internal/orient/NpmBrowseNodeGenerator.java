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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.AssetPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * NPM places components at the same level as their assets and removes `-` segments from the path.
 *
 * @since 3.6
 */
@Singleton
@Named(NpmFormat.NAME)
public class NpmBrowseNodeGenerator
    extends AssetPathBrowseNodeGenerator
{
  /**
   * In NPM we have two types of assets:
   * <ul>
   * <li>metadata assets - for example the "jquery" metadata asset will have a path of ["jquery"]</li>
   * <li>component assets - for example the "jquery/1.9.1/-/jquery-1.9.1.tar.gz" asset will have a path of ["jquery",
   * "1.9.1", "jquery-1.9.1.tar.gz"]</li>
   * </ul>
   */
  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);

    return super.computeAssetPaths(asset, component).stream().filter(paths -> !"-".equals(paths.getBrowsePath()))
        .collect(toList());
  }
}
