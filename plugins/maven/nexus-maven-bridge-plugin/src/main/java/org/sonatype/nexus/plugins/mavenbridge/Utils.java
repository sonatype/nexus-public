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
package org.sonatype.nexus.plugins.mavenbridge;

import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.nexus.proxy.maven.gav.Gav;

/**
 * Collection of static utility methods to bridge the "gap" between Aether and Nexus.
 *
 * @author cstamas
 */
public class Utils
{
  private Utils() {
  }

  /**
   * A shorthand method to create a Dependency from GAV and scope.
   *
   * @param gav   GAV to make Dependency, may not be {@code null}.
   * @param scope the needed scope, or {@code null}
   */
  public static Dependency createDependencyFromGav(final Gav gav, final String scope) {
    Dependency dependency =
        new Dependency(new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), gav.getExtension(),
            gav.getVersion()), scope);

    return dependency;
  }
}
