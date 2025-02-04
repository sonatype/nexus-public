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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.sonatype.nexus.blobstore.BlobAttributesSupport;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.common.property.PropertiesFile;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link BlobAttributes} backed by {@link PropertiesFile}
 *
 * @since 3.0
 */
public class FileBlobAttributes
    extends BlobAttributesSupport<PropertiesFile>
{
  public FileBlobAttributes(final Path path) {
    super(new PropertiesFile(path.toFile()), null, null);
  }

  public FileBlobAttributes(final Path path, final Map<String, String> headers, final BlobMetrics metrics) {
    super(new PropertiesFile(path.toFile()), checkNotNull(headers), checkNotNull(metrics));
  }

  /**
   * @since 3.2
   */
  public Path getPath() {
    return propertiesFile.getFile().toPath();
  }

  /**
   * Returns {@code false} if the attribute file is not found.
   */
  public boolean load() throws IOException {
    if (!Files.exists(getPath())) {
      return false;
    }
    propertiesFile.load();
    readFrom(propertiesFile);
    return true;
  }

  @Override
  public void store() throws IOException {
    writeTo(propertiesFile);
    propertiesFile.store();
  }

  @Override
  public void writeProperties() {
    writeTo(propertiesFile);
  }
}
