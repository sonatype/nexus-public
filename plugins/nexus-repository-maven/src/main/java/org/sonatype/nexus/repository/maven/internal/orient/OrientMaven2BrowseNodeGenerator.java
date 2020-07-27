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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.node.BrowsePath.SLASH;

/**
 * Maven layout is based on group, name, and version; places assets one level below their components.
 * This differs from the default generator in that any dots in the group are converted to slashes.
 *
 * Note: snapshot components are combined under their base version but keep their timestamped version:
 *
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/3.7.0-20171212.235354-266/
 *
 * This avoids having multiple snapshot components at the same '3.7.0-SNAPSHOT' node in the browse UI.
 *
 * A side-effect of this is snapshot assets are also displayed under the additional timestamped folder:
 *
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/3.7.0-20171107.223311-149/nexus-common-3.7.0-20171107.223311-149.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/3.7.0-20171113.234015-168/nexus-common-3.7.0-20171113.234015-168.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/3.7.0-20171212.235354-266/nexus-common-3.7.0-20171212.235354-266.pom
 *
 * instead being displayed under the same base '-SNAPSHOT' folder as in the standard Maven layout:
 *
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/nexus-common-3.7.0-20171107.223311-149.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/nexus-common-3.7.0-20171113.234015-168.pom
 * /org/sonatype/nexus/nexus-common/3.7.0-SNAPSHOT/nexus-common-3.7.0-20171212.235354-266.pom
 *
 * But note they still retain their real asset path as the 'request' path for permissions checks.
 *
 * @since 3.6
 */
@Singleton
@Named(Maven2Format.NAME)
@Priority(Integer.MAX_VALUE)
public class OrientMaven2BrowseNodeGenerator
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
      return super.computeAssetPaths(asset, null);
    }
  }

  /**
   * @return [componentGroupDotsToDashes]/componentName/[componentVersion]
   */
  @Override
  public List<BrowsePaths> computeComponentPaths(final Asset asset, final Component component) {
    List<String> pathParts = new ArrayList<>();

    if (!Strings2.isBlank(component.group())) {
      pathParts.addAll(Splitter.on('.').omitEmptyStrings().splitToList(component.group()));
    }

    pathParts.add(component.name());

    List<BrowsePaths> paths = BrowsePaths.fromPaths(pathParts, true);

    if (!Strings2.isBlank(component.version())) {
      String baseVersion = component.attributes().child("maven2").get("baseVersion", String.class);
      //the request path should be as expected by maven, so that security is applied properly, hence we use the
      //same path for both nodes added below
      String requestPath = paths.get(paths.size() - 1).getRequestPath() + baseVersion + SLASH;
      if (!component.version().equals(baseVersion)) {
        // Put the SNAPSHOT version (baseVersion) before the component version in the tree.
        BrowsePaths.appendPath(paths, baseVersion, requestPath);
      }
      BrowsePaths.appendPath(paths, component.version(), requestPath);
    }

    return paths;
  }
}
