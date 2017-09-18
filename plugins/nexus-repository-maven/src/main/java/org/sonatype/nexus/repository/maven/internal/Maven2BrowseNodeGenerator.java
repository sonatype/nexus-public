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
package org.sonatype.nexus.repository.maven.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Handles events that require management of folder data for maven repositories.
 *
 * @since 3.6
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2BrowseNodeGenerator
    implements BrowseNodeGenerator
{
  /**
   * Construct the asset path by splitting the asset name on the `/` character.
   *
   * @return the path to the asset
   */
  @Override
  public List<String> computeAssetPath(final Asset asset, final Component component) {
    checkNotNull(asset);

    return asList(asset.name().split("/"));
  }

  /**
   * @return the path to the folder containing the asset
   */
  @Override
  public List<String> computeComponentPath(final Asset asset, final Component component) {
    if (component != null) {
      List<String> assetPath = computeAssetPath(asset, component);
      return assetPath.subList(0, assetPath.size() - 1);
    }
    else {
      return emptyList();
    }
  }
}
