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
   * Handle both {@code store-name@blob-id} and {@code store-name:blob-id@node-id} blob ref formats
   */
  private static final Pattern BLOB_REF_PATTERN = Pattern.compile("([^@]+)@(.+)");

  private static final Pattern SPLIT_PATTERN = Pattern.compile("([^@:]+):([^@:].+)");

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

    String firstPart = matcher.group(1);
    if (firstPart.contains(":")) {
      Matcher splitMatcher = SPLIT_PATTERN.matcher(firstPart);
      checkArgument(splitMatcher.matches(), "Not a valid blob reference");

      String store = splitMatcher.group(1);
      String blobId = splitMatcher.group(2);
      return new BlobRef(store, blobId);
    } else {
      String store = firstPart;
      String blobId = matcher.group(2);
      if (store.isEmpty() || blobId.isEmpty()) {
        throw new IllegalArgumentException("Not a valid blob reference");
      } else {
        return new BlobRef(store, blobId);
      }
    }
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
