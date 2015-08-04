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


public class BaseGemFile
    extends RubygemsFile
{
  /**
   * helper method to concatenate <code>name</code>, <code>version</code>
   * and <code>platform</code> in the same manner as rubygems create filenames
   * of gems.
   */
  public static String toFilename(String name, String version, String platform) {
    StringBuilder filename = new StringBuilder(name);
    if (version != null) {
      filename.append("-").append(version);
      if (platform != null && !"ruby".equals(platform)) {
        filename.append("-").append(platform);
      }
    }
    return filename.toString();
  }

  private final String filename;

  private final String version;

  private final String platform;

  /**
   * contructor using the full filename of a gem. there is no version nor platform info available
   */
  BaseGemFile(RubygemsFileFactory factory, FileType type, String storage, String remote, String filename) {
    this(factory, type, storage, remote, filename, null, null);
  }

  /**
   * constructor using name, version and platform to build the filename of a gem
   */
  BaseGemFile(RubygemsFileFactory factory, FileType type, String storage, String remote,
              String name, String version, String platform)
  {
    super(factory, type, storage, remote, name);
    this.filename = toFilename(name, version, platform);
    this.version = version;
    this.platform = platform;
  }

  /**
   * the full filename of the gem
   */
  public String filename() {
    return filename;
  }

  /**
   * the version of the gem
   *
   * @return can be <code>null</code>
   */
  public String version() {
    return version;
  }

  /**
   * the platform of the gem
   *
   * @return can be <code>null</code>
   */
  public String platform() {
    return platform;
  }
}