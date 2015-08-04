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
 * represents /quick/Marshal.4.8/{name}-{version}.gemspec.rz or /quick/Marshal.4.8/{name}-{platform}-{version}.gemspec.rz
 * or /quick/Marshal.4.8/{filename}.gemspec.rz
 *
 * @author christian
 */
public class GemspecFile
    extends BaseGemFile
{
  /**
   * setup with full filename
   */
  GemspecFile(RubygemsFileFactory factory, String storage, String remote, String name) {
    super(factory, FileType.GEMSPEC, storage, remote, name);
  }

  /**
   * setup with name, version and platform
   */
  GemspecFile(RubygemsFileFactory factory, String storage, String remote, String name, String version, String platform) {
    super(factory, FileType.GEMSPEC, storage, remote, name, version, platform);
  }

  /**
   * retrieve the associated gem-file
   */
  public GemFile gem() {
    if (version() != null) {
      return factory.gemFile(name(), version(), platform());
    }
    else {
      return factory.gemFile(filename());
    }
  }
}