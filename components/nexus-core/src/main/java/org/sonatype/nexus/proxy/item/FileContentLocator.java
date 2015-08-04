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
package org.sonatype.nexus.proxy.item;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sonatype.nexus.util.WrappingInputStream;
import org.sonatype.nexus.util.file.DirSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A content locator that is backed by a file. It has ability to create a temporary file for you, and to become
 * "non reusable" locator, when it cleans up the file after it.
 * 
 * @author cstamas
 * @since 2.1
 */
public class FileContentLocator
    extends AbstractContentLocator
{
  private final File file;

  private final boolean deleteOnCloseInput;

  /**
   * Creates a temporary file backed instance, that will be not reusable, once file content is consumed (using
   * {@link #getInputStream()} or {@link #getContent()}).
   */
  public FileContentLocator(final String mimeType) throws IOException {
    this(File.createTempFile("nx-tmp-content-locator", "tmp"), mimeType, true);
  }

  /**
   * Creates a file backed instance that will be backed by passed in {@link File}. It will be reusable, the passed in
   * file should be removed (if needed) by caller.
   */
  public FileContentLocator(final File file, final String mimeType) {
    this(file, mimeType, false);
  }

  /**
   * Creates a file backed instance.
   * 
   * @param file the file to be used with this instance.
   * @param mimeType the mime type of this instance.
   * @param deleteOnCloseInput if {@code true}, the passed in file will be deleted upon consumption (when
   *          {@link InputStream#close()} is invoked) on {@link InputStream} got by and of the {@link #getInputStream()}
   *          or {@link #getContent()} method. Also, this instance will be
   *          marked as
   *          "non reusable" (method {@link #isReusable()} will return {@code false}).
   */
  public FileContentLocator(final File file, final String mimeType, final boolean deleteOnCloseInput) {
    super(mimeType, !deleteOnCloseInput, file.length());
    this.file = checkNotNull(file);
    this.deleteOnCloseInput = deleteOnCloseInput;
  }

  public InputStream getInputStream() throws IOException {
    if (deleteOnCloseInput) {
      return new DeleteOnCloseFileInputStream(getFile());
    }
    else {
      return new FileInputStream(getFile());
    }
  }

  public OutputStream getOutputStream() throws IOException {
    return new FileOutputStream(getFile());
  }

  @Override
  public long getLength() {
    return getFile().length();
  }

  public File getFile() {
    return file;
  }

  public void delete() throws IOException {
    // locator is used against files only, not directories
    // but their existence is not enforced!
    DirSupport.deleteIfExists(getFile().toPath());
  }

  // ==

  @Override
  public InputStream getContent() throws IOException {
    return getInputStream();
  }

  // ==

  public static class DeleteOnCloseFileInputStream
      extends WrappingInputStream
  {
    private final File file;

    public DeleteOnCloseFileInputStream(final File file) throws IOException {
      super(new FileInputStream(file));
      this.file = file;
    }

    public void close() throws IOException {
      super.close();
      // locator is used against files only, not directories
      // but their existence is not enforced!
      DirSupport.deleteIfExists(file.toPath());
    }
  }
}
