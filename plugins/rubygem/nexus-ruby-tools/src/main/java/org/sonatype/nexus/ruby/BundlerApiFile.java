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
 * belongs to the path /api/v1/dependencies?gems=name1,name2
 *
 * @author christian
 */
public class BundlerApiFile
    extends RubygemsFile
{

  private final String[] names;

  private static String storageName(String remote) {
    Sha1Digest digest = new Sha1Digest();
    digest.update(remote.getBytes());
    // assume no digest collision happens
    return remote.replaceFirst("\\?gems=.*$", "/" + digest.hexDigest() + ".gems");
  }

  BundlerApiFile(RubygemsFileFactory factory, String remote, String... names) {
    super(factory, FileType.BUNDLER_API, storageName(remote), remote, null);
    this.names = names;
  }

  /**
   * names of gems from the query parameter 'gems'
   */
  public String[] gemnames() {
    return names;
  }
}