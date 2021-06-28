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
package org.sonatype.repository.conan.internal.orient.ui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.repository.conan.internal.ConanFormat;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.BrowsePaths.fromPaths;
import static org.sonatype.repository.conan.internal.common.ConanBrowseNodeGeneratorHelper.assetSegment;

/**
 * @since 3.28
 */
@Singleton
@Named(ConanFormat.NAME)
public class OrientConanBrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  public OrientConanBrowseNodeGenerator() {
    super();
  }

  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, final Component component) {
    List<String> componentList = new ArrayList<>();
    componentList.add(component.group());
    componentList.add(component.name());
    componentList.add(component.version());
    return fromPaths(componentList, true);
  }

  @Override
  public List<BrowsePaths> computeAssetPaths(final Asset asset, final Component component) {
    checkNotNull(asset);

    if (component != null) {
      List<BrowsePaths> strings = computeComponentPaths(asset, component);
      strings.addAll(fromPaths(assetSegment(asset.name()), false));
      return strings;
    }
    return super.computeAssetPaths(asset, null);
  }

}
