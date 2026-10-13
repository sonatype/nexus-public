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
package org.sonatype.nexus.blobstore.api;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * A handle for binary data stored within a {@link BlobStore}.
 *
 * @since 3.0
 */
public interface Blob
{
  BlobId getId();

  /**
   * An immutable map of the headers that were provided when the blob was created.
   *
   * @throws BlobStoreException may be thrown if the blob is {@link BlobStore#delete deleted} or
   *           {@link BlobStore#delete hard deleted}.
   */
  Map<String, String> getHeaders();

  /**
   * Opens an input stream to the blob's content. The returned stream may be closed asynchronously if the blob is
   * {@link BlobStore#deleteHard(BlobId) hard deleted}. The input stream must support {@link InputStream#mark(int)}
   * because the ability to inspect the stream must be supported
   *
   * @throws BlobStoreException may be thrown if the blob is {@link BlobStore#delete deleted} or
   *           {@link BlobStore#delete hard deleted}.
   */
  InputStream getInputStream();

  /**
   * Provides metrics about this Blob.
   *
   * @throws BlobStoreException may be thrown if the blob is {@link BlobStore#delete deleted} or
   *           {@link BlobStore#delete hard deleted}.
   */
  BlobMetrics getMetrics();

  /**
   * Provides a URL for direct access (i.e. not through Nexus Repository) to the blob's content.
   * <p>
   * Implementations that want to honour {@code globalDistributionEnabled} MUST override this
   * four-argument method; overriding only the three-argument variant will silently ignore the flag
   * because the default here bridges by discarding it.
   *
   * @param globalDistributionEnabled {@code true} if the caller allows this blob store to return a
   *          globally-distributed URL (e.g. CloudFront). Blob stores that do not implement a
   *          global distribution generator ignore this flag.
   */
  default Optional<URL> getRedirectUrl(
      final String action,
      final String name,
      @Nullable final String contentType,
      final boolean globalDistributionEnabled)
  {
    return getRedirectUrl(action, name, contentType);
  }

  /**
   * @deprecated Kept only for binary compatibility with implementations compiled before the
   *             {@code globalDistributionEnabled} flag was introduced. All in-tree callers use the
   *             4-arg form {@link #getRedirectUrl(String, String, String, boolean)}. This overload
   *             will be removed once no external implementation still ships an override of it.
   */
  @Deprecated
  default Optional<URL> getRedirectUrl(final String action, final String name, @Nullable final String contentType) {
    return Optional.empty();
  }

  /**
   * @return if this blob would return a URL when calling getRedirectUrl
   */
  default boolean hasRedirect() {
    return false;
  }
}
