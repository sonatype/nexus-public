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
package org.sonatype.nexus.proxy.maven.metadata;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * @author juven
 */
public abstract class AbstractMetadataProcessor
{
  protected static final String METADATA_SUFFIX = "/maven-metadata.xml";

  protected AbstractMetadataHelper metadataHelper;

  public AbstractMetadataProcessor(AbstractMetadataHelper metadataHelper) {
    this.metadataHelper = metadataHelper;
  }

  /**
   * @return true if got processed, else false
   */
  public boolean process(String path)
      throws IOException
  {
    if (!shouldProcessMetadata(path)) {
      return false;
    }

    Metadata oldMetadata = null;

    if (isMetadataExisted(path)) {
      oldMetadata = readMetadata(path);

      if (oldMetadata != null && isMetadataCorrect(oldMetadata, path)) {
        postProcessMetadata(path);

        return true;
      }
      else {
        removedMetadata(path);
      }
    }

    processMetadata(path, oldMetadata);

    postProcessMetadata(path);

    buildMetadataChecksum(path);

    return true;

  }

  protected boolean isMetadataExisted(String path)
      throws IOException
  {
    return metadataHelper.exists(path + METADATA_SUFFIX);
  }

  protected Metadata readMetadata(String path)
      throws IOException
  {
    try (final InputStream mdStream = metadataHelper.retrieveContent(path + METADATA_SUFFIX)) {
      Metadata md = MetadataBuilder.read(mdStream);
      return md;
    }
    catch (IOException e) {
      if (metadataHelper.logger.isDebugEnabled()) {
        metadataHelper.logger.info("Failed to parse metadata from '" + path + "'", e);
      }
      else {
        metadataHelper.logger.info("Failed to parse metadata from '" + path + "'");
      }
      return null;
    }
  }

  protected void removedMetadata(String path)
      throws IOException
  {
    metadataHelper.remove(path + METADATA_SUFFIX);
  }

  protected void buildMetadataChecksum(String path)
      throws IOException
  {
    metadataHelper.rebuildChecksum(path + METADATA_SUFFIX);
  }

  protected abstract boolean isMetadataCorrect(Metadata metadata, String path)
      throws IOException;

  protected abstract boolean shouldProcessMetadata(String path);

  protected void processMetadata(final String path)
      throws IOException
  {
    // do nothing
  }

  /**
   * @since 2.6
   */
  protected void processMetadata(final String path, final Metadata oldMetadata)
      throws IOException
  {
    processMetadata(path);
  }

  protected abstract void postProcessMetadata(String path);
}
