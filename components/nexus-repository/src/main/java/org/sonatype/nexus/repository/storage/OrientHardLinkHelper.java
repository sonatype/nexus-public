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
package org.sonatype.nexus.repository.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.mime.MimeSupport;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @since 3.35
 * @deprecated please use the appropriate ingest methods on {@code StorageFacet} and {@code FluentBlobs}
 */
@Deprecated
@Named
@Singleton
public class OrientHardLinkHelper
{
  private final MimeSupport mimeSupport;

  @Inject
  public OrientHardLinkHelper(final MimeSupport mimeSupport) {
    this.mimeSupport = checkNotNull(mimeSupport);
  }

  /**
   * Ingests a blob from a {@code content} via hard-linking.
   */
  public AssetBlob ingestHardLink(
      final StorageTx tx,
      final File content,
      final Map<HashAlgorithm, HashCode> hashes)
      throws IOException
  {
    checkState(hashes.containsKey(HashAlgorithm.SHA1));

    String path = content.getPath();
    Path contentPath = content.toPath();
    String contentType = detectMimeType(content);

    long size = Files.size(contentPath);
    return tx.createBlob(path, contentPath, hashes, null, contentType, size);
  }

  /**
   * Ingests a blob from a {@code content} via hard-linking.
   */
  public AssetBlob ingestHardLink(
      final StorageTx tx,
      final File content,
      final Map<HashAlgorithm, HashCode> hashes,
      final String contentType)
      throws IOException
  {
    checkState(hashes.containsKey(HashAlgorithm.SHA1));

    String path = content.getPath();
    Path contentPath = content.toPath();

    long size = Files.size(contentPath);
    return tx.createBlob(path, contentPath, hashes, null, contentType, size);
  }

  private String detectMimeType(final File content) throws IOException {
    String path = content.getPath();
    Path contentPath = content.toPath();
    try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(contentPath))) {
      return mimeSupport.detectMimeType(inputStream, path);
    }
  }
}
