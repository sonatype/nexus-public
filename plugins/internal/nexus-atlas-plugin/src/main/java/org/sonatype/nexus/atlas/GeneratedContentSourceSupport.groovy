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
package org.sonatype.nexus.atlas

import java.nio.file.Files

/**
 * Support for generated {@link SupportBundle.ContentSource} implementations.
 *
 * These sources will buffer output to a file on prepare.
 *
 * @since 2.7
 */
abstract class GeneratedContentSourceSupport
    extends ContentSourceSupport
{
  private File file

  GeneratedContentSourceSupport(final SupportBundle.ContentSource.Type type, final String path) {
    super(type, path)
  }

  @Override
  void prepare() {
    assert file == null
    file = File.createTempFile(path.replaceAll('/', '-') + '-', '.tmp').canonicalFile
    log.trace 'Preparing: {}', file
    generate(file)
  }

  protected abstract void generate(File file) throws Exception

  @Override
  long getSize() {
    assert file.exists()
    return file.length()
  }

  @Override
  InputStream getContent() {
    assert file.exists()
    return file.newInputStream()
  }

  @Override
  void cleanup() {
    if (file != null) {
      log.trace 'Cleaning: {}', file
      Files.delete(file.toPath())
      file = null
    }
  }
}