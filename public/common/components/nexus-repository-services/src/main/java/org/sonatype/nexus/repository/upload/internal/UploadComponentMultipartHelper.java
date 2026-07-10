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
package org.sonatype.nexus.repository.upload.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.upload.TempBlobFactory;
import org.sonatype.nexus.repository.upload.internal.BlobStoreMultipartForm.TempBlobFormField;

// NEXUS-46395: commons-fileupload 1.x → 2.x renames:
//   FileItemIterator         → FileItemInputIterator
//   FileItemStream           → FileItemInput
//   FileUploadIOException    → (gone; FileUploadException now extends IOException)
//   IOFileUploadException    → FileUploadException (one wrapper class only)
//   AbstractFileUpload$FileItemIteratorImpl$FileItemStreamImpl → internal name changed;
//                              we walk the public API in 2.x and avoid reflection.
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.RequestContext;

import static org.apache.commons.fileupload2.core.AbstractFileUpload.MULTIPART_FORM_DATA;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import org.springframework.stereotype.Component;

/**
 * Extracts fields from a multipart form storing files in the appropriate blob store.
 *
 * @since 3.16
 */
@Component
public class UploadComponentMultipartHelper
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final TempBlobFactory tempBlobFactory;

  @Autowired
  public UploadComponentMultipartHelper(final TempBlobFactory tempBlobFactory) {
    this.tempBlobFactory = checkNotNull(tempBlobFactory);
  }

  /**
   * Parse a multipart-form submission creating file uploads in the blob store of the repository. Reminder, callers must
   * call {@code close} on {@code TempBlobs} returned from this method.
   */
  public BlobStoreMultipartForm parse(
      final Repository repository,
      final HttpServletRequest request) throws FileUploadException
  {
    BlobStoreMultipartForm multipartForm = new BlobStoreMultipartForm();
    TempBlobServletFileUpload upload = new TempBlobServletFileUpload(repository, multipartForm);

    upload.parseRequest(request);

    // ExtJs results in fields with the upload name for some reason
    multipartForm.getFiles().keySet().forEach(assetName -> multipartForm.getFormFields().remove(assetName));

    return multipartForm;
  }

  private class TempBlobServletFileUpload
      extends JakartaServletDiskFileUpload
  {
    private final Repository repository;

    private final BlobStoreMultipartForm multipartForm;

    private final Predicate<String> assetPattern = Pattern.compile("^(\\w+\\.)?asset\\d*$").asPredicate();

    TempBlobServletFileUpload(
        final Repository repository,
        final BlobStoreMultipartForm multipartForm)
    {
      this.repository = repository;
      this.multipartForm = multipartForm;
    }

    // NEXUS-46395: JakartaServletDiskFileUpload's parseRequest returns List<DiskFileItem>
    // (was List<FileItem> in 1.x).
    @Override
    public List<DiskFileItem> parseRequest(final RequestContext ctx) throws FileUploadException {
      boolean successful = false;
      try {
        FileItemInputIterator iter = getItemIterator(ctx);
        while (iter.hasNext()) {
          createField(iter.next());
        }
        successful = true;
        return Collections.emptyList();
      }
      catch (IOException e) {
        // NEXUS-46395: in fileupload 2.x, FileUploadException extends IOException; one
        // catch handles both. The previous FileUploadIOException unwrap is unnecessary.
        if (e instanceof FileUploadException fue) {
          throw fue;
        }
        throw new FileUploadException(e.getMessage(), e);
      }
      finally {
        if (!successful) {
          for (TempBlobFormField tempBlob : multipartForm.getFiles().values()) {
            tempBlob.getTempBlob().close();
          }
        }
      }
    }

    private void createField(final FileItemInput item) throws FileUploadException {
      try (InputStream in = item.getInputStream()) {
        // isFormField() is derived from whether the filename in the form was non-null, at least for some of our tests
        // this is not sufficient.
        if (!item.isFormField() || (assetPattern.test(item.getFieldName()) && item.getContentType() != null)) {
          // NEXUS-46395: commons-fileupload 1.x exposed the raw `name` field on the package-
          // private FileItemStreamImpl, and we used reflection here to bypass
          // getName()'s InvalidFileNameException. In 2.x the public FileItemInput#getName()
          // is the supported accessor; it returns the raw client-supplied filename and only
          // throws java.nio.file.InvalidPathException for genuinely-pathological inputs
          // (NUL bytes, OS-invalid paths). Catching InvalidPathException converts those to
          // a clean 4xx-style FileUploadException instead of letting it bubble up as a 500.
          String fileName = item.getName();
          multipartForm.putFile(item.getFieldName(), new TempBlobFormField(item.getFieldName(), fileName,
              tempBlobFactory.create(repository, in, HashAlgorithm.ALL_HASH_ALGORITHMS.values())));
        }
        else {
          multipartForm.putFormField(item.getFieldName(), IOUtils.toString(in, getCharSet(item.getContentType())));
        }
      }
      catch (InvalidPathException e) {
        throw new FileUploadException(
            format("Invalid filename in %s upload (field '%s'): %s", MULTIPART_FORM_DATA, item.getFieldName(),
                e.getMessage()),
            e);
      }
      catch (IOException e) {
        if (e instanceof FileUploadException fue) {
          throw fue;
        }
        throw new FileUploadException(
            format("Processing of %s request failed. %s", MULTIPART_FORM_DATA, e.getMessage()), e);
      }
    }

    private String getCharSet(final String contentType) {
      // NEXUS-46395: ParameterParser was removed from commons-fileupload 2.x core; do a
      // simple manual parse since this is a content-type charset extraction only.
      if (contentType == null) {
        return StandardCharsets.UTF_8.name();
      }
      for (String part : contentType.split(";")) {
        String trimmed = part.trim().toLowerCase();
        if (trimmed.startsWith("charset=")) {
          String charset = trimmed.substring("charset=".length()).trim();
          if (charset.startsWith("\"") && charset.endsWith("\"") && charset.length() > 1) {
            charset = charset.substring(1, charset.length() - 1);
          }
          return charset;
        }
      }
      return StandardCharsets.UTF_8.name();
    }
  }
}
