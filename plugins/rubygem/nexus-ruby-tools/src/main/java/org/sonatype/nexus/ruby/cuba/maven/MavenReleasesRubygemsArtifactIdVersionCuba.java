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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.Cuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /maven/prereleases/rubygems/{artifactId}/{version}/
 *
 * @author christian
 */
public class MavenReleasesRubygemsArtifactIdVersionCuba
    implements Cuba
{

  private static final Pattern FILE = Pattern.compile("^.*\\.(gem|pom|gem.sha1|pom.sha1)$");

  private final String artifactId;

  private final String version;

  public MavenReleasesRubygemsArtifactIdVersionCuba(String artifactId, String version) {
    this.artifactId = artifactId;
    this.version = version;
  }

  /**
   * directories one for each version of the gem with given name/artifactId
   *
   * files [{artifactId}-{version}.gem,{artifactId}-{version}.gem.sha1,
   * {artifactId}-{version}.pom,{artifactId}-{version}.pom.sha1]
   */
  @Override
  public RubygemsFile on(State state) {
    Matcher m = FILE.matcher(state.name);
    if (m.matches()) {
      switch (m.group(1)) {
        case "gem":
          return state.context.factory.gemArtifact(artifactId, version);
        case "pom":
          return state.context.factory.pom(artifactId, version);
        case "gem.sha1":
          RubygemsFile file = state.context.factory.gemArtifact(artifactId, version);
          return state.context.factory.sha1(file);
        case "pom.sha1":
          file = state.context.factory.pom(artifactId, version);
          return state.context.factory.sha1(file);
        default:
      }
    }
    switch (state.name) {
      case "":
        return state.context.factory.gemArtifactIdVersionDirectory(state.context.original, artifactId, version, false);
      default:
        return state.context.factory.notFound(state.context.original);
    }
  }
}