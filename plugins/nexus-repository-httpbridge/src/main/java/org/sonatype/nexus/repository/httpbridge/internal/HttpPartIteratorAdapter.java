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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.repository.view.Payload;

import com.google.common.base.Throwables;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Servlet multipart-payload adapter.
 *
 * @since 3.0
 */
class HttpPartIteratorAdapter
    implements Iterable<Payload>
{
  private final HttpServletRequest httpRequest;

  public HttpPartIteratorAdapter(final HttpServletRequest httpRequest) {
    this.httpRequest = checkNotNull(httpRequest);
  }

  @Override
  public Iterator<Payload> iterator() {
    try {
      final FileItemIterator itemIterator = new ServletFileUpload().getItemIterator(httpRequest);
      return new PayloadIterator(itemIterator);
    }
    catch (FileUploadException | IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * {@link FileItemStream} payload.
   */
  private static class FileItemStreamPayload
      implements Payload
  {
    private final FileItemStream next;

    public FileItemStreamPayload(final FileItemStream next) {
      this.next = next;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return next.openStream();
    }

    @Override
    public long getSize() {
      return -1;
    }

    @Nullable
    @Override
    public String getContentType() {
      return next.getContentType();
    }
  }

  /**
   * {@link Payload} iterator.
   */
  private static class PayloadIterator
      implements Iterator<Payload>
  {
    private final FileItemIterator itemIterator;

    public PayloadIterator(final FileItemIterator itemIterator) {
      this.itemIterator = itemIterator;
    }

    @Override
    public boolean hasNext() {
      try {
        return itemIterator.hasNext();
      }
      catch (FileUploadException | IOException e) {
        throw Throwables.propagate(e);
      }
    }

    @Override
    public Payload next() {
      try {
        return new FileItemStreamPayload(itemIterator.next());
      }
      catch (FileUploadException | IOException e) {
        throw Throwables.propagate(e);
      }
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Determine if given request is multipart.
   */
  public static boolean isMultipart(final HttpServletRequest httpRequest) {
    // We're circumventing ServletFileUpload.isMultipartContent as some clients (nuget) use PUT for multipart uploads
    return FileUploadBase.isMultipartContent(new ServletRequestContext(httpRequest));
  }
}
