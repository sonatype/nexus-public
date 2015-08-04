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

import java.util.Arrays;

/**
 * represent /maven/releases/rubygems/{artifactId} or /maven/prereleases/rubygems/{artifactId}
 *
 * @author christian
 */
public class GemArtifactIdDirectory
    extends Directory
{

  private final boolean prereleased;

  GemArtifactIdDirectory(RubygemsFileFactory factory, String path, String name, boolean prereleased) {
    super(factory, path, name);
    items.add("maven-metadata.xml");
    items.add("maven-metadata.xml.sha1");
    this.prereleased = prereleased;
  }

  /**
   * whether to show prereleased or released gems inside the directory
   */
  public boolean isPrerelease() {
    return prereleased;
  }

  /**
   * the <code>DependencyFile</code> of the given gem
   */
  public DependencyFile dependency() {
    return this.factory.dependencyFile(name());
  }

  /**
   * setup the directory items. for each version one item, either
   * released or prereleased version.
   */
  public void setItems(DependencyData data) {
    if (!prereleased) {
      // we list ALL versions when not on prereleased directory
      this.items.addAll(0, Arrays.asList(data.versions(false)));
    }
    this.items.addAll(0, Arrays.asList(data.versions(true)));
  }
}