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
package org.sonatype.nexus.content.maven.internal.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.BrowsePathBuilder;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.ComponentPathBrowseNodeGenerator;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.browse.node.BrowsePathBuilder.appendPath;

/**
 * Browse node for maven.
 *
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
 * @since 3.next
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2BrowseNodeGenerator
    extends ComponentPathBrowseNodeGenerator
{
  protected static final String BASE_VERSION = "baseVersion";

  private static final String SLASH = "/";

  @Override
  public List<BrowsePath> computeAssetPaths(final Asset asset) {
    checkNotNull(asset);

    Optional<Component> component = asset.component();
    if (component.isPresent()) {
      // place asset one level beneath component, using the asset's name as the display name
      List<BrowsePath> paths = computeComponentPaths(asset);

      String assetRequestPath = paths.get(paths.size() - 1).getRequestPath();
      if (isSnapshotVersion(component.get())) {
        assetRequestPath = getNonTimestampedRequestPath(paths);
      }
      appendAssetBrowsePath(asset, paths, assetRequestPath);
      return paths;
    }
    else {
      return super.computeAssetPaths(asset);
    }
  }

  /**
   * @return [componentGroupDotsToDashes]/componentName/[componentVersion]
   */
  @Override
  public List<BrowsePath> computeComponentPaths(final Asset asset) {
    checkNotNull(asset);
    checkArgument(asset.component().isPresent());
    Component component = asset.component().get();

    List<String> pathParts = new ArrayList<>();

    if (!Strings2.isBlank(component.namespace())) {
      pathParts.addAll(Splitter.on('.').omitEmptyStrings().splitToList(component.namespace()));
    }
    pathParts.add(component.name());

    List<BrowsePath> paths = BrowsePathBuilder.fromPaths(pathParts, true);

    String version = component.version();
    if (!Strings2.isBlank(version)) {
      String baseVersion = component.attributes().child(Maven2Format.NAME).get(BASE_VERSION, String.class);
      String requestPath = paths.get(paths.size() - 1).getRequestPath() + baseVersion + SLASH;
      if (!version.equals(baseVersion)) {
        appendPath(paths, baseVersion, requestPath);
        appendPath(paths, version, requestPath + version + SLASH);
      }
      else {
        appendPath(paths, version, requestPath);
      }
    }
    return paths;
  }

  private boolean isSnapshotVersion(final Component component) {
    String baseVersion = component.attributes(Maven2Format.NAME).get(BASE_VERSION, String.class);
    return !StringUtils.equals(component.version(), baseVersion);
  }

  private String getNonTimestampedRequestPath(final List<BrowsePath> paths) {
    checkArgument(paths.size() >= 2);
    return paths.get(paths.size() - 2).getRequestPath();
  }

  private void appendAssetBrowsePath(final Asset asset, final List<BrowsePath> paths, final String assetRequestPath) {
    String lastSegment = lastSegment(asset.path());
    appendPath(paths, lastSegment, assetRequestPath + lastSegment);
  }
}
