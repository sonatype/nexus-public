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

public class GemArtifactFile
    extends RubygemsFile
{
  private final String version;

  private final boolean snapshot;

  private GemFile gem;

  GemArtifactFile(RubygemsFileFactory factory,
                  String path,
                  String name,
                  String version,
                  boolean snapshot)
  {
    super(factory, FileType.GEM_ARTIFACT, path, path, name);
    this.version = version;
    this.snapshot = snapshot;
  }

  /**
   * the version of the gem
   */
  public String version() {
    return version;
  }

  /**
   * whether it is a snapshot or not
   */
  public boolean isSnapshot() {
    return snapshot;
  }

  /**
   * is lazy state of the associated GemFile. the GemFile needs to
   * have the right platform for which the DependencyData is needed
   * to retrieve this platform. a second call can be done without DependencyData !
   *
   * @param dependencies can be null
   * @return the associated GemFile - can be null if DependencyData was never passed in
   */
  public GemFile gem(DependencyData dependencies) {
    if (this.gem == null && dependencies != null) {
      String platform = dependencies.platform(version());
      if (platform != null) {
        this.gem = factory.gemFile(name(), version(), platform);
      }
    }
    return this.gem;
  }

  /**
   * the associated DependencyFile object for the gem-artifact
   */
  public DependencyFile dependency() {
    return factory.dependencyFile(name());
  }
}