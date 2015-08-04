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
package org.sonatype.nexus.ruby.layout;

import java.io.InputStream;

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsFileFactory;

/**
 * it adds a single extra method to the <code>RubygemsFileFactory</code>
 *
 * @author christian
 */
public interface Layout
    extends RubygemsFileFactory
{
  /**
   * some layout needs to be able to "upload" gem-files
   *
   * @param is   the <code>InputStream</code> which is used to store the given file
   * @param file which can be <code>GemFile</code> or <code>ApiV1File</code> with name "gem"
   */
  void addGem(InputStream is, RubygemsFile file);
}