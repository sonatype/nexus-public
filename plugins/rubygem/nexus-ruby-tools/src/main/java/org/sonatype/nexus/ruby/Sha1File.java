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
 * a SHA1 digest of give <code>RubygemsFile</code>
 *
 * @author christian
 */
public class Sha1File
    extends RubygemsFile
{
  private final RubygemsFile source;

  Sha1File(RubygemsFileFactory factory, String storage, String remote, RubygemsFile source) {
    super(factory, FileType.SHA1, storage, remote, source.name());
    this.source = source;
    if (source.notExists()) {
      markAsNotExists();
    }
  }

  /**
   * the source for which the SHA1 digest
   *
   * @return RubygemsFile
   */
  public RubygemsFile getSource() {
    return source;
  }
}