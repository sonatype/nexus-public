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
 * cuba for /maven
 *
 * @author christian
 */
public class MavenCuba
    implements Cuba
{
  public static final String RELEASES = "releases";

  public static final String PRERELEASES = "prereleases";

  private final Cuba releases;

  private final Cuba prereleases;

  public MavenCuba(Cuba releases, Cuba prereleases) {
    this.releases = releases;
    this.prereleases = prereleases;
  }

  /**
   * directories [releases,prereleases]
   */
  @Override
  public RubygemsFile on(State state) {
    switch (state.name) {
      case RELEASES:
        return state.nested(releases);
      case PRERELEASES:
        return state.nested(prereleases);
      case "":
        return state.context.factory.directory(state.context.original,
            PRERELEASES, RELEASES);
      default:
        return state.context.factory.notFound(state.context.original);
    }
  }
}