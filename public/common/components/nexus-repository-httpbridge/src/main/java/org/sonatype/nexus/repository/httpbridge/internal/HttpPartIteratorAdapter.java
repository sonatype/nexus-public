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
import java.nio.file.InvalidPathException;
import java.util.Iterator;

import javax.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;

// NEXUS-46395: commons-fileupload 1.x → 2.x renames:
//   FileItemIterator -> FileItemInputIterator
//   FileItemStream   -> FileItemInput
//   FileUploadBase   -> AbstractFileUpload (with isMultipartContent moved to JakartaServletFileUpload)
//   jakarta.servlet.* integration -> jakarta.servlet5 subpackage
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletRequestContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Servlet multipart-payload adapter.
 *
 * @since 3.0
 */
class HttpPartIteratorAdapter
    implements Iterable<PartPayload>
{
  private final HttpServletRequest httpRequest;

  public HttpPartIteratorAdapter(final HttpServletRequest httpRequest) {
    this.httpRequest = checkNotNull(httpRequest);
  }

  @Override
  public Iterator<PartPayload> iterator() {
    try {
      // NEXUS-46395: commons-fileupload 2.x: getItemIterator returns FileItemInputIterator,
      // and JakartaServletFileUpload is generic over its FileItem type (use the parameterized
      // form to satisfy javac).
      final FileItemInputIterator itemIterator =
          new JakartaServletFileUpload<>().getItemIterator(httpRequest);
      return new PayloadIterator(itemIterator);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@link FileItemInput} payload.
   */
  private static class FileItemStreamPayload
      implements PartPayload
  {
    private final FileItemInput next;

    public FileItemStreamPayload(final FileItemInput next) {
      this.next = next;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      // NEXUS-46395: commons-fileupload 2.x renamed openStream() -> getInputStream()
      return next.getInputStream();
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

    @Nullable
    @Override
    public String getName() {
      try {
        return next.getName();
      }
      // NEXUS-46395: commons-fileupload 2.x throws InvalidPathException for filenames
      // containing NUL bytes or characters illegal on the host filesystem (replacing 1.x's
      // InvalidFileNameException, which was also a RuntimeException). Match the @Nullable
      // contract by treating an unrepresentable name as absent rather than letting the
      // exception propagate as a 500 through repository upload handlers (Maven, raw, apt).
      catch (InvalidPathException e) {
        return null;
      }
    }

    @Override
    public String getFieldName() {
      return next.getFieldName();
    }

    @Override
    public boolean isFormField() {
      return next.isFormField();
    }

    @Nullable
    @Override
    public String getHeader(final String name) {
      FileItemHeaders headers = next.getHeaders();
      return headers != null ? headers.getHeader(name) : null;
    }
  }

  /**
   * {@link Payload} iterator.
   */
  private static class PayloadIterator
      implements Iterator<PartPayload>
  {
    private final FileItemInputIterator itemIterator;

    public PayloadIterator(final FileItemInputIterator itemIterator) {
      this.itemIterator = itemIterator;
    }

    @Override
    public boolean hasNext() {
      try {
        return itemIterator.hasNext();
      }
      // NEXUS-46395: in commons-fileupload 2.x, FileUploadException now extends IOException,
      // so a multi-catch alternative would be redundant; one IOException catch covers both.
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public PartPayload next() {
      try {
        return new FileItemStreamPayload(itemIterator.next());
      }
      catch (IOException e) {
        throw new RuntimeException(e);
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
    // We're circumventing JakartaServletFileUpload.isMultipartContent as some clients (nuget) use PUT for multipart
    // uploads
    return JakartaServletFileUpload.isMultipartContent(new JakartaServletRequestContext(httpRequest));
  }
}
