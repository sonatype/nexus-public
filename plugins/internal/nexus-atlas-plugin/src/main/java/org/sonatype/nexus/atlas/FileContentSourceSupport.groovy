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

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Support for including existing files as {@link SupportBundle.ContentSource}.
 *
 * @since 2.7
 */
class FileContentSourceSupport
    extends ContentSourceSupport
{
  private final File file

  FileContentSourceSupport(final SupportBundle.ContentSource.Type type, final String path, final File file) {
    super(type, path)
    this.file = checkNotNull(file)
  }

  @Override
  void prepare() {
    assert file.exists()
    // TODO: Should we copy the file to temp location to ensure it exists?
    // TODO: May have to if we have to obfuscate the file with a generic mechanism?
  }

  @Override
  long getSize() {
    assert file.exists()
    return file.length()
  }

  @Override
  InputStream getContent() {
    assert file.exists()
    log.debug 'Reading: {}', file
    return file.newInputStream()
  }

  @Override
  void cleanup() {
    // nothing
  }
}