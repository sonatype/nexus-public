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

public class PomFile
    extends RubygemsFile
{
  private final String version;

  private final boolean snapshot;

  PomFile(RubygemsFileFactory factory, String path,
          String name, String version, boolean snapshot)
  {
    super(factory, FileType.POM, path, path, name);
    this.version = version;
    this.snapshot = snapshot;
  }

  public String version() {
    return version;
  }

  public boolean isSnapshot() {
    return snapshot;
  }

  public GemspecFile gemspec(DependencyData dependencies) {
    String platform = dependencies.platform(version());
    return factory.gemspecFile(name(), version(), platform);
  }

  public GemFile gem(DependencyData dependencies) {
    String platform = dependencies.platform(version());
    return factory.gemFile(name(), version(), platform);
  }

  public DependencyFile dependency() {
    return factory.dependencyFile(name());
  }
}