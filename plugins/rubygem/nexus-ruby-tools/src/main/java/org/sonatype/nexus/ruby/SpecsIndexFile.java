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
 * represents /specs.4.8 or /prereleased_specs.4.8 or /latest_specs.4.8
 *
 * @author christian
 */
public class SpecsIndexFile
    extends RubygemsFile
{
  private final SpecsIndexType specsType;

  SpecsIndexFile(RubygemsFileFactory factory, String path, String name) {
    super(factory, FileType.SPECS_INDEX, path, path, name);
    specsType = SpecsIndexType.fromFilename(path);
  }

  /**
   * retrieve the SpecsIndexType
   */
  public SpecsIndexType specsType() {
    return specsType;
  }

  /**
   * get the gzipped version of this file
   */
  public SpecsIndexZippedFile zippedSpecsIndexFile() {
    return factory.specsIndexZippedFile(name());
  }
}