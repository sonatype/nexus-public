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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven layout is based on group, name, and version; places assets one level below their components.
 * This differs from the default generator in that any dots in the group are converted to slashes.
 *
 * Note: snapshot components keep their unique version in the component path:
 *
 * /org/sonatype/nexus/nexus-common/3.7.0-20171212.235354-266/
 *
 * which means snapshot assets are listed under their unique version:
 *
 * /org/sonatype/nexus/nexus-common/3.7.0-20171107.223311-149/nexus-common-3.7.0-20171107.223311-149.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-20171113.234015-168/nexus-common-3.7.0-20171113.234015-168.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-20171212.235354-266/nexus-common-3.7.0-20171212.235354-266.pom
 *
 * instead being listed under the same base '-SNAPSHOT' directory:
 *
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/nexus-common-3.7.0-20171107.223311-149.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/nexus-common-3.7.0-20171113.234015-168.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/nexus-common-3.7.0-20171212.235354-266.pom
 *
 * This avoids having multiple snapshot components at the same '3.7.0-SNAPSHOT' node in the browse tree.
 *
 * @since 3.6
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2BrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  /**
   * @return componentPath/lastSegment(assetPath) if the component was not null, otherwise assetPath
   */
  @Override
  public List<String> computeAssetPath(final Asset asset, @Nullable final Component component) {
    checkNotNull(asset);

    if (component != null) {
      List<String> path = computeComponentPath(asset, component);

      // place asset just below component
      path.add(lastSegment(asset.name()));

      return path;
    }
    else {
      return super.computeAssetPath(asset, null);
    }
  }

  /**
   * @return [componentGroupDotsToDashes]/componentName/[componentVersion]
   */
  @Override
  public List<String> computeComponentPath(final Asset asset, final Component component) {
    List<String> path = new ArrayList<>();

    if (!Strings2.isBlank(component.group())) {
      path.addAll(Splitter.on('.').omitEmptyStrings().splitToList(component.group()));
    }

    path.add(component.name());

    if (!Strings2.isBlank(component.version())) {
      String baseVersion = component.attributes().child("maven2").get("baseVersion", String.class);
      if (!component.version().equals(baseVersion)) {
        // Put the SNAPSHOT version (baseVersion) before the component version in the tree.
        path.add(baseVersion);
      }
      path.add(component.version());
    }

    return path;
  }
}
