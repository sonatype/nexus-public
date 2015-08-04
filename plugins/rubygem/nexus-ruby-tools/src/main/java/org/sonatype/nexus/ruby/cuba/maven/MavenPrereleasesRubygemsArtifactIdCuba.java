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
package org.sonatype.nexus.ruby.cuba.maven;

import org.sonatype.nexus.ruby.MavenMetadataFile;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.Cuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /maven/prereleases/rubygems/{artifactId}
 *
 * @author christian
 */
public class MavenPrereleasesRubygemsArtifactIdCuba
    implements Cuba
{

  public static final String SNAPSHOT = "-SNAPSHOT";

  private final String name;

  public MavenPrereleasesRubygemsArtifactIdCuba(String name) {
    this.name = name;
  }

  /**
   * directories one for each version of the gem with given name/artifactId
   *
   * files [maven-metadata.xml,maven-metadata.xml.sha1]
   */
  @Override
  public RubygemsFile on(State state) {
    switch (state.name) {
      case MavenReleasesRubygemsArtifactIdCuba.MAVEN_METADATA_XML:
        return state.context.factory.mavenMetadata(name, true);
      case MavenReleasesRubygemsArtifactIdCuba.MAVEN_METADATA_XML + ".sha1":
        MavenMetadataFile file = state.context.factory.mavenMetadata(name, true);
        return state.context.factory.sha1(file);
      case "":
        return state.context.factory.gemArtifactIdDirectory(state.context.original, name, true);
      default:
        return state.nested(new MavenPrereleasesRubygemsArtifactIdVersionCuba(name,
            state.name.replace(SNAPSHOT, "")));
    }
  }
}