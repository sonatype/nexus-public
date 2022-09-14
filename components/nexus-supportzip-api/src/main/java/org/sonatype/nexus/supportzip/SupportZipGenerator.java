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
package org.sonatype.nexus.supportzip;

import java.io.OutputStream;
import java.io.Serializable;

import org.sonatype.nexus.common.log.SupportZipGeneratorRequest;

/**
 * Generates a support ZIP file.
 *
 * @since 2.7
 */
public interface SupportZipGenerator
{
  /**
   * Result of support ZIP generate request.
   */
  class Result implements Serializable
  {
    private static final long serialVersionUID = -3253827134366752809L;

    /**
     * True if the ZIP or any of its contents had been truncated.
     */
    private boolean truncated;

    /**
     * The name of the generated ZIP file.
     */
    private String filename;

    /**
     * The local path of the generated ZIP file.
     */
    private String localPath;

    /**
     * The size of the generated ZIP file.
     */
    private long size;

    public boolean isTruncated() {
      return truncated;
    }

    public void setTruncated(final boolean truncated) {
      this.truncated = truncated;
    }

    public String getFilename() {
      return filename;
    }

    public void setFilename(final String filename) {
      this.filename = filename;
    }

    public String getLocalPath() {
      return localPath;
    }

    public void setLocalPath(final String localPath) {
      this.localPath = localPath;
    }

    public long getSize() {
      return size;
    }

    public void setSize(final long size) {
      this.size = size;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "truncated=" + truncated +
          ", filename=" + filename +
          ", localPath=" + localPath +
          ", size=" + size +
          '}';
    }
  }

  /**
   * Generate a support ZIP for the given request.
   */
  Result generate(SupportZipGeneratorRequest request);

  /**
   * Generate a support ZIP for the given request with a custom prefix.
   */
  Result generate(SupportZipGeneratorRequest request, String prefix);

  /**
   * Generate a support ZIP.
   * @param outputStream the output stream to write the ZIP to.
   * @param prefix directory prefix applied to files in the ZIP.
   * @return true if the zip was truncated.
   * @since 3.5
   */
  boolean generate(SupportZipGeneratorRequest request, String prefix, OutputStream outputStream);
}
