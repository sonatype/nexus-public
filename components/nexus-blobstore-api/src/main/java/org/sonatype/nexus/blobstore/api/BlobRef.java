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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a pointer to a blob in a given store.
 *
 * @since 3.0
 */
public class BlobRef
{
  /**
   * The pattern to match node-id and blob-id segments with 30 or more characters.
   */
  private static final String ID_MATCHER = "[\\w-]{30,}";

  /**
   * String of the pattern used on 3.47.1 and newer for blobref. Matches {@code store@blob-id}
   */
  private static final String CANONICAL_PATTERN =
      String.format("(?<store>.+)@(?<blobid>%s$)", ID_MATCHER);

  /**
   * String of the pattern used on OrientDB installs prior to 3.47.0. Matches {@code store@node-id:blob-id}
   */
  private static final String ORIENT_PATTERN =
      String.format("(?<ostore>.+)@(%s):(?<oblobid>%s$)", ID_MATCHER, ID_MATCHER);

  /**
   * String of the pattern used on SQL installs prior to 3.47.0. Matches {@code store:blob-id@node-id}
   */
  private static final String SQL_PATTERN =
      String.format("(?<sstore>.+):(?<sblobid>%s)@(%s$)", ID_MATCHER, ID_MATCHER);

  /**
   * Matcher which matches all 3 formats
   */
  private static final Pattern BLOB_REF_PATTERN =
      Pattern.compile(String.format("%s|%s|%s", ORIENT_PATTERN, SQL_PATTERN, CANONICAL_PATTERN));

  private static final String BLOB_REF_SIMPLE_FORMAT = "%s@%s";

  private final String node;

  private final String store;

  private final String blob;

  public BlobRef(final String store, final String blob) {
    this(null, store, blob);
  }

  public BlobRef(@Nullable final String node, final String store, final String blob) {
    this.node = node;
    this.store = checkNotNull(store);
    this.blob = checkNotNull(blob);
  }

  public static BlobRef parse(final String spec) {
    Matcher matcher = BLOB_REF_PATTERN.matcher(spec);
    checkArgument(matcher.matches(), "Not a valid blob reference");

    String store = matcher.group("store");
    if (store != null) {
      // canonical pattern
      return new BlobRef(store, matcher.group("blobid"));
    }

    store = matcher.group("ostore");
    if (store != null) {
      // orient pattern
      return new BlobRef(store, matcher.group("oblobid"));
    }

    store = matcher.group("sstore");
    if (store != null) {
      // sql pattern
      return new BlobRef(store, matcher.group("sblobid"));
    }

    throw new IllegalArgumentException("Not a valid blob reference");
  }

  @Nullable
  public String getNode() {
    return node;
  }

  public String getStore() {
    return store;
  }

  public String getBlob() {
    return blob;
  }

  public BlobId getBlobId() {
    return new BlobId(getBlob());
  }

  public BlobId getBlobId(final OffsetDateTime blobCreated) {
    return new BlobId(getBlob(), blobCreated);
  }

  /**
   * @return the blob ref encoded as a string, using the syntax {@code store@blob-id}
   */
  @Override
  public String toString() {
    return String.format(BLOB_REF_SIMPLE_FORMAT, getStore(), getBlob());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BlobRef blobRef = (BlobRef) o;
    return Objects.equals(blob, blobRef.blob) && Objects.equals(store, blobRef.store);
  }

  @Override
  public int hashCode() {
    return Objects.hash(store, blob);
  }
}
