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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a pointer to a blob on a given node, in a given store.
 *
 * @since 3.0
 */
public class BlobRef
{
  private final String node;

  private final String store;

  private final String blob;

  public static final Pattern BLOB_REF_PATTERN = Pattern.compile("([^@]+)@([^:]+):(.*)");

  public BlobRef(final String node, final String store, final String blob) {
    this.node = checkNotNull(node);
    this.store = checkNotNull(store);
    this.blob = checkNotNull(blob);
  }

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

  public static BlobRef parse(final String spec) {
    final Matcher matcher = BLOB_REF_PATTERN.matcher(spec);
    checkArgument(matcher.matches(), "Not a valid blob reference");

    return new BlobRef(matcher.group(2), matcher.group(1), matcher.group(3));
  }

  /**
   * @return the blob ref encoded as a string, using the syntax {@code store@node:blob-id}
   */
  public String toString() {
    return String.format("%s@%s:%s", getStore(), getNode(), getBlob());
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

    if (!blob.equals(blobRef.blob)) {
      return false;
    }
    if (!node.equals(blobRef.node)) {
      return false;
    }
    if (!store.equals(blobRef.store)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = node.hashCode();
    result = 31 * result + store.hashCode();
    result = 31 * result + blob.hashCode();
    return result;
  }
}
