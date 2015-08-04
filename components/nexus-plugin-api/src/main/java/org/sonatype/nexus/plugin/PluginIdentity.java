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
package org.sonatype.nexus.plugin;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.inject.Inject;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Properties2;

import org.jetbrains.annotations.NonNls;

/**
 * Helper for plugin identity components.
 *
 * These components should be marked as @EagerSingleton so that the validation of the GAV always is processed,
 * even if the class is not directly injected.
 *
 * @since 2.7
 */
public class PluginIdentity
    extends ComponentSupport
{
  public static final String UNKNOWN = "unknown";

  private final GAVCoordinate coordinates;

  @Inject
  public PluginIdentity(final String groupId, final String artifactId) throws Exception {
    this.coordinates = loadCoordinates(groupId, artifactId);
  }

  protected GAVCoordinate loadCoordinates(final String groupId, final String artifactId) throws IOException {
    String path = String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId); //NON-NLS
    URL url = getClass().getResource(path);

    // If we can not find the metadata, then bitch and return whatever we can
    if (url == null) {
      log.warn("Missing plugin project metadata: {}", path);
      return new GAVCoordinate(groupId, artifactId, UNKNOWN);
    }

    Properties props = Properties2.load(url);
    @NonNls
    GAVCoordinate gav = new GAVCoordinate(
        props.getProperty("groupId", UNKNOWN),
        props.getProperty("artifactId", UNKNOWN),
        props.getProperty("version", UNKNOWN)
    );

    // Complain if there is a mismatch between what we expect the gav to be and what it really is
    if (!groupId.equals(gav.getGroupId())) {
      log.warn("Plugin groupId mismatch; expected: {}, found: {}", groupId, gav.getGroupId()); //NON-NLS
    }
    if (!artifactId.equals(gav.getArtifactId())) {
      log.warn("Plugin artifactId mismatch; expected: {}, found: {}", artifactId, gav.getArtifactId()); //NON-NLS
    }

    return gav;
  }

  public GAVCoordinate getCoordinates() {
    return coordinates;
  }

  public String getId() {
    return getCoordinates().getArtifactId();
  }

  public String getVersion() {
    return getCoordinates().getVersion();
  }
}