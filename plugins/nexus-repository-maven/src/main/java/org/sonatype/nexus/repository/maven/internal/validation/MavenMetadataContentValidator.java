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
package org.sonatype.nexus.repository.maven.internal.validation;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.MavenModels;

import org.apache.maven.artifact.repository.metadata.Metadata;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;

/**
 * Validates that maven-metadata.xml is parsable and has the correct path.
 * 
 * @since 3.16
 */
@Singleton
public class MavenMetadataContentValidator
    extends ComponentSupport
{
  public void validate(final String path, final InputStream mavenMetadata) {
    try {
      Metadata metadata = MavenModels.readMetadata(mavenMetadata);

      if (metadata == null) {
        throw new InvalidContentException("Metadata at path " + path + " is not a valid maven-metadata.xml");
      }

      // maven-metadata.xml files for plugins do not contain groupId and therefore cannot be validated
      if (isNotEmpty(metadata.getGroupId())) {
        validatePath(path, metadata);
      }
      else {
        log.debug("No groupId found in maven-metadata.xml therefore skipping validation");
      }
    }
    catch (IOException e) {
      log.warn("Unable to read maven-metadata.xml at path {} caused by {}", path, e );
      
      throw new InvalidContentException("Unable to read maven-metadata.xml reason: " + e.getMessage());
    }
  }

  private void validatePath(final String path, final Metadata metadata) {
    String version = getMetadataVersion(metadata);

    MavenPath expectedPath = metadataPath(metadata.getGroupId(), metadata.getArtifactId(), version);

    if (!path.equals(expectedPath.getPath())) {
      String pattern = "Invalid maven-metadata.xml GAV %s, %s, %s does not match request path %s";

      String message = String
          .format(pattern, metadata.getGroupId(), metadata.getArtifactId(), metadata.getVersion(), path);

      log.warn("maven-metadata.xml path {} does not match the expected path {}", path, expectedPath.getPath());

      throw new InvalidContentException(message);
    }
  }

  private String getMetadataVersion(final Metadata metadata) {
    if (metadata.getVersion() != null && metadata.getVersion().contains("-SNAPSHOT")) {
      log.debug("maven-metadata.xml contains a SNAPSHOT version ({}) therefore the version is expected to be part of " +
              "the path", metadata.getVersion());

      return metadata.getVersion();
    }
    else {
      log.debug("maven-metadata.xml version ({}) is either null or not a SNAPSHOT therefore not expected in the path",
          metadata.getVersion());
      
      return null;
    }
  }
}
