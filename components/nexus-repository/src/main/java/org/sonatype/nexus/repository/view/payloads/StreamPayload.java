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
package org.sonatype.nexus.repository.view.payloads;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stream payload.
 *
 * @since 3.0
 */
public class StreamPayload
    implements Payload
{
  private final InputStreamSupplier stream;

  private final long size;

  private final String contentType;

  public StreamPayload(final InputStreamSupplier stream, final long size, @Nullable final String contentType) {
    this.stream = checkNotNull(stream);
    this.size = size;
    this.contentType = contentType;
  }

  /**
   * Returnes opened stream from configured supplier.
   */
  @Override
  public InputStream openInputStream() throws IOException {
    return stream.get();
  }

  @Override
  public long getSize() {
    return size;
  }

  @Nullable
  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "stream=" + stream +
        ", size=" + size +
        ", contentType='" + contentType + '\'' +
        '}';
  }

  //
  // Supplier
  //

  // NOTE: Not using Guava Supplier, as we need to express throws IOException

  /**
   * Supplies an {@link InputStream}.
   */
  public interface InputStreamSupplier
  {
    @Nonnull
    InputStream get() throws IOException;
  }

  /**
   * Returns an {@link InputStream} and takes in an argument
   */
  public interface InputStreamFunction<T>
  {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    @Nonnull
    InputStream apply(T t) throws IOException;
  }
}
