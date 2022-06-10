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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenModels;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility class containing shared general methods for Maven metadata.
 *
 * @since 3.25
 */
public final class MetadataUtils
{
  private static final Logger log = LoggerFactory.getLogger(MetadataUtils.class);

  private MetadataUtils() {
    //no op
  }

  /**
   * Builds a Maven path for the specified metadata.
   */
  public static MavenPath metadataPath(
      final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(groupId.replace('.', '/'));
    if (artifactId != null) {
      sb.append("/").append(artifactId);
      if (baseVersion != null) {
        sb.append("/").append(baseVersion);
      }
    }
    sb.append("/").append(Constants.METADATA_FILENAME);
    return new MavenPath(sb.toString(), null);
  }

  /**
   * Returns the plugin prefix of a Maven plugin, by opening up the plugin JAR, and reading the Maven Plugin
   * Descriptor. If fails, falls back to mangle artifactId (ie. extract XXX from XXX-maven-plugin or
   * maven-XXX-plugin).
   */
  public static String getPluginPrefix(final MavenPath mavenPath, final InputStreamSupplier inputSupplier) {
    // sanity checks: is artifact and extension is "jar", only possibility for maven plugins currently
    checkArgument(mavenPath.getCoordinates() != null);
    checkArgument(Objects.equals(mavenPath.getCoordinates().getExtension(), "jar"));
    String prefix = null;
    try {
      if (inputSupplier != null) {
        try (ZipInputStream zip = new ZipInputStream(inputSupplier.get())) {
          ZipEntry entry;
          while ((entry = zip.getNextEntry()) != null) {
            if (!entry.isDirectory() && "META-INF/maven/plugin.xml".equals(entry.getName())) {
              final Xpp3Dom dom = MavenModels.parseDom(zip);
              prefix = getChildValue(dom, "goalPrefix", null);
              break;
            }
            zip.closeEntry();
          }
        }
      }
    }
    catch (IOException e) {
      log.warn("Unable to read plugin.xml of {}", mavenPath, e);
    }
    if (prefix != null) {
      return prefix;
    }
    if ("maven-plugin-plugin".equals(mavenPath.getCoordinates().getArtifactId())) {
      return "plugin";
    }
    else {
      return mavenPath.getCoordinates().getArtifactId().replaceAll("-?maven-?", "").replaceAll("-?plugin-?", "");
    }
  }

  /*
   * Helper method to get node's immediate child or default.
   */
  private static String getChildValue(final Xpp3Dom doc, final String childName, final String defaultValue) {
    Xpp3Dom child = doc.getChild(childName);
    if (child == null) {
      return defaultValue;
    }
    return child.getValue();
  }
}
