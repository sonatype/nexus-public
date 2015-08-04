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
package org.sonatype.nexus.yum.client.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * @since yum 3.0
 */
public class CompressionAdapter
{

  private final CompressionType compression;

  public CompressionAdapter(CompressionType compression) {
    this.compression = compression;
  }

  public InputStream adapt(InputStream inputStream)
      throws IOException
  {
    switch (compression) {
      case NONE:
        return inputStream;
      case GZIP:
        return new GZIPInputStream(inputStream);
      case BZIP2:
        return new BZip2CompressorInputStream(inputStream);
      default:
        throw new IllegalArgumentException("Could not adapt unknown compression " + compression);
    }
  }

}
