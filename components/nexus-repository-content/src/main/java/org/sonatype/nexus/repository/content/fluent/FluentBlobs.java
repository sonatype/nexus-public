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
package org.sonatype.nexus.repository.content.fluent;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;

/**
 * Fluent API for ingesting blobs.
 *
 * @since 3.21
 */
public interface FluentBlobs
{
  /**
   * Ingests the given stream as a temporary blob with the requested hashing.
   */
  TempBlob ingest(InputStream in, @Nullable String contentType, Iterable<HashAlgorithm> hashing);

  /**
   * Ingests the given stream as a temporary blob with custom headers and the requested hashing.
   */
  TempBlob ingest(InputStream in, @Nullable String contentType, Map<String, String> headers, Iterable<HashAlgorithm> hashing);

  /**
   * Ingests the given payload as a temporary blob with the requested hashing.
   */
  TempBlob ingest(Payload payload, Iterable<HashAlgorithm> hashing);

  TempBlob ingest(final Blob srcBlob, final BlobStore srcStore, final Map<HashAlgorithm, HashCode> hashes);


  /**
   * Ingests the given payload as a temporary blob with the requested hashing.
   *
   * @param path        the path to the file on the local file system
   * @param contentType the content type of the provided file if known
   * @param hashing     the hashing algorithms to use for the blob
   * @param requireHardLink  when true ingest will fail if the attempt to hard link fails, otherwise an attempt will be
   *                         made to copy the file content.
   *
   * @since 3.41
   */
  TempBlob ingest(Path path, @Nullable String contentType, Iterable<HashAlgorithm> hashing, boolean requireHardLink);

  /**
   * Ingests a blob from a {@code sourceFile} via hard-linking.
   *
   * @since 3.29
   */
  Blob ingest(Path sourceFile, Map<String, String> headers, HashCode sha1, long size);

  /**
   * Fetches the Blob associated with the specified BlobRef or Optional.empty() if
   * no such Blob exists.
   *
   * @since 3.29
   */
  Optional<Blob> blob(BlobRef blobRef);

  /**
   * @since 3.29
   */
  BlobStoreMetrics getMetrics();
}
