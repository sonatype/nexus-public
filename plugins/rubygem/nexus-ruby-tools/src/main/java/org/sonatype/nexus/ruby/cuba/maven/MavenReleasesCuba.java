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

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.Cuba;
import org.sonatype.nexus.ruby.cuba.State;

/**
 * cuba for /maven/releases/
 *
 * @author christian
 */
public class MavenReleasesCuba
    implements Cuba
{
  public static final String RUBYGEMS = "rubygems";

  private final Cuba mavenReleasesRubygems;

  public MavenReleasesCuba(Cuba mavenReleasesRubygems) {
    this.mavenReleasesRubygems = mavenReleasesRubygems;
  }

  /**
   * directory [rubygems]
   */
  @Override
  public RubygemsFile on(State state) {
    switch (state.name) {
      case MavenReleasesCuba.RUBYGEMS:
        return state.nested(mavenReleasesRubygems);
      case "":
        return state.context.factory.directory(state.context.original, MavenReleasesCuba.RUBYGEMS);
      default:
        return state.context.factory.notFound(state.context.original);
    }
  }
}