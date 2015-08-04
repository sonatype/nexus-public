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
package org.sonatype.nexus.obr.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Abstract {@link ObrSite} that automatically detects compressed OBRs and inflates them.
 */
public abstract class AbstractObrSite
    implements ObrSite
{
  public final InputStream openStream()
      throws IOException
  {
    if ("application/zip".equalsIgnoreCase(getContentType())) {
      final ZipInputStream zis = new ZipInputStream(openRawStream());
      for (ZipEntry e = zis.getNextEntry(); e != null; e = zis.getNextEntry()) {
        // scan for the specific OBR entry
        final String name = e.getName().toLowerCase();
        if (name.endsWith("repository.xml") || name.endsWith("obr.xml")) {
          return zis;
        }
      }
      throw new IOException("No repository.xml or obr.xml in zip: " + getMetadataUrl());
    }

    return openRawStream();
  }

  /**
   * Opens a new raw stream to the OBR metadata, caller must close the stream.
   *
   * @return a new input stream
   */
  protected abstract InputStream openRawStream()
      throws IOException;

  /**
   * Retrieves the Content-Type of the OBR metadata.
   *
   * @return OBR resource Content-Type
   */
  protected abstract String getContentType();
}
