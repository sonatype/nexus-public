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
 * there are currently only two supported files inside the /api/v1 directory: gems and api_key.
 * the constructor allows all file-names
 *
 * @author christian
 */
public class ApiV1File
    extends RubygemsFile
{
  ApiV1File(RubygemsFileFactory factory, String storage, String remote, String name) {
    super(factory, FileType.API_V1, storage, remote, name);
    set(null);// no payload
  }

  /**
   * convenient method to convert a gem-filename into <code>GemFile</code>
   *
   * @param filename of the gem
   * @return GemFile
   */
  public GemFile gem(String filename) {
    return factory.gemFile(filename.replaceFirst(".gem$", ""));
  }
}