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
package org.sonatype.nexus.supportzip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for including existing files as {@link SupportBundle.ContentSource}.
 *
 * @since 2.7
 */
public class FileContentSourceSupport
    extends ContentSourceSupport
{
  private final File file;

  /**
   * @since 3.0
   */
  public FileContentSourceSupport(final Type type, final String path, final File file, final Priority priority) {
    super(type, path, priority);
    this.file = checkNotNull(file);
  }

  public FileContentSourceSupport(final Type type, final String path, final File file) {
    this(type, path, file, Priority.DEFAULT);
  }

  @Override
  public void prepare() throws Exception {
    checkState(file.exists());
  }

  @Override
  public long getSize() {
    checkState(file.exists());
    return file.length();
  }

  @Override
  public InputStream getContent() throws Exception {
    checkState(file.exists());
    log.debug("Reading: {}", file);
    return new BufferedInputStream(new FileInputStream(file));
  }

  @Override
  public void cleanup() throws Exception {
    // nothing
  }
}