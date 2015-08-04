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
package org.sonatype.nexus.ruby;

/**
 * represent /maven/releases/rubygems/{artifactId}/{version} or /maven/prereleases/rubygems/{artifactId}/{version}
 *
 * @author christian
 */
public class GemArtifactIdVersionDirectory
    extends Directory
{

  /**
   * setup the directory items
   */
  GemArtifactIdVersionDirectory(RubygemsFileFactory factory,
                                String path,
                                String name,
                                String version,
                                boolean prerelease)
  {
    super(factory, path, name);
    String base = name + "-" + version + ".";
    this.items.add(base + "pom");
    this.items.add(base + "pom.sha1");
    this.items.add(base + "gem");
    this.items.add(base + "gem.sha1");
    if (prerelease) {
      this.items.add("maven-metadata.xml");
      this.items.add("maven-metadata.xml.sha1");
    }
  }
}