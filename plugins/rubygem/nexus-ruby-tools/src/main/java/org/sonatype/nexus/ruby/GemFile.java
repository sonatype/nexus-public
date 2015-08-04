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
 * represents /gems/{name}-{version}.gem or /gems/{name}-{platform}-{version}.gem or /gems/{filename}.gem
 *
 * @author christian
 */
public class GemFile
    extends BaseGemFile
{

  /**
   * setup with full filename
   */
  GemFile(RubygemsFileFactory factory, String storage, String remote, String filename) {
    super(factory, FileType.GEM, storage, remote, filename);
  }

  /**
   * setup with name, version and platform
   */
  GemFile(RubygemsFileFactory factory,
          String storage,
          String remote,
          String name,
          String version,
          String platform)
  {
    super(factory, FileType.GEM, storage, remote, name, version, platform);
  }

  /**
   * retrieve the associated gemspec
   */
  public GemspecFile gemspec() {
    if (version() != null) {
      return factory.gemspecFile(name(), version(), platform());
    }
    else {
      return factory.gemspecFile(filename());
    }
  }
}