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
package org.sonatype.nexus.repository.content.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;

/**
 * @since 3.35
 * @deprecated please use the appropriate ingest methods on {@code StorageFacet} and {@code FluentBlobs}
 */
@Deprecated
@Named
@Singleton
public class HardLinkHelper
{
  private final MimeSupport mimeSupport;

  @Inject
  public HardLinkHelper(final MimeSupport mimeSupport) {
    this.mimeSupport = checkNotNull(mimeSupport);
  }

  /**
   * Ingests a blob from a {@code content} via hard-linking.
   */
  public Blob ingestHardLink(final Repository repository, final File content, final HashCode hashCodeSha1)
      throws IOException
  {
    checkNotNull(repository);
    checkNotNull(content);
    checkNotNull(hashCodeSha1);

    Map<String, String> headers = new HashMap<>();
    String path = content.getPath();
    headers.put(BLOB_NAME_HEADER, path);
    String contentType = detectMimeType(content);
    headers.put(CONTENT_TYPE_HEADER, contentType);

    Path contentPath = content.toPath();

    FluentBlobs fluentBlobs = repository.facet(ContentFacet.class).blobs();

    return fluentBlobs.ingest(contentPath, headers, hashCodeSha1, Files.size(contentPath));
  }

  /**
   * Ingests a blob from a {@code content} via hard-linking.
   */
  public Blob ingestHardLink(
      final Repository repository,
      final File content,
      final HashCode hashCodeSha1,
      final String contentType)
      throws IOException
  {
    checkNotNull(repository);
    checkNotNull(content);
    checkNotNull(hashCodeSha1);
    checkNotNull(contentType);

    Map<String, String> headers = new HashMap<>();
    String path = content.getPath();
    headers.put(BLOB_NAME_HEADER, path);
    headers.put(CONTENT_TYPE_HEADER, contentType);

    Path contentPath = content.toPath();

    FluentBlobs fluentBlobs = repository.facet(ContentFacet.class).blobs();

    return fluentBlobs.ingest(contentPath, headers, hashCodeSha1, Files.size(contentPath));
  }

  private String detectMimeType(final File content) throws IOException {
    String path = content.getPath();
    Path contentPath = content.toPath();
    try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(contentPath))) {
      return mimeSupport.detectMimeType(inputStream, path);
    }
  }
}
