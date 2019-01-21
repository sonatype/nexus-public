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
package org.sonatype.nexus.blobstore.file;

import java.nio.file.Path;

import org.sonatype.nexus.blobstore.AttributesLocation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Location for FileBlobStore attributes files
 *
 * @since 3.15
 */
public class FileAttributesLocation
    implements AttributesLocation
{
  private final Path path;

  public FileAttributesLocation(final Path path) {
    this.path = checkNotNull(path);
  }

  @Override
  public String getFileName() {
    return path.toFile().getName();
  }

  @Override
  public String getFullPath() {
    return path.toString();
  }

  public Path getPath() {
    return path;
  }
}
