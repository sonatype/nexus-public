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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;
import org.sonatype.nexus.security.ClientInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_IP_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.TEMPORARY_BLOB_HEADER;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_OCTET_STREAM;

/**
 * {@link FluentBlobs} implementation.
 *
 * @since 3.24
 */
public class FluentBlobsImpl
    implements FluentBlobs
{
  private final ContentFacetSupport facet;

  public FluentBlobsImpl(final ContentFacetSupport facet) {
    this.facet = checkNotNull(facet);
  }

  @Override
  public TempBlob ingest(final InputStream in,
                         @Nullable final String contentType,
                         final Iterable<HashAlgorithm> hashing)
  {
    Optional<ClientInfo> clientInfo = facet.clientInfo();

    Builder<String, String> tempHeaders = ImmutableMap.builder();
    tempHeaders.put(TEMPORARY_BLOB_HEADER, "");
    tempHeaders.put(REPO_NAME_HEADER, facet.repository().getName());
    tempHeaders.put(BLOB_NAME_HEADER, "temp");
    tempHeaders.put(CREATED_BY_HEADER, clientInfo.map(ClientInfo::getUserid).orElse("system"));
    tempHeaders.put(CREATED_BY_IP_HEADER, clientInfo.map(ClientInfo::getRemoteIP).orElse("system"));
    tempHeaders.put(CONTENT_TYPE_HEADER, ofNullable(contentType).orElse(APPLICATION_OCTET_STREAM));

    MultiHashingInputStream hashingStream = new MultiHashingInputStream(hashing, in);
    Blob blob = facet.stores().blobStore.create(hashingStream, tempHeaders.build());

    return new TempBlob(blob, hashingStream.hashes(), true, facet.stores().blobStore);
  }

  @Override
  public TempBlob ingest(final Payload payload, final Iterable<HashAlgorithm> hashing) {
    if (payload instanceof TempBlobPartPayload) {
      return ((TempBlobPartPayload) payload).getTempBlob();
    }
    try (InputStream in = payload.openInputStream()) {
      return ingest(in, payload.getContentType(), hashing);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
