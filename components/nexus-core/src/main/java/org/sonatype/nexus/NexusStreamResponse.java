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
package org.sonatype.nexus;

import java.io.InputStream;

public class NexusStreamResponse
{
  private String name;

  private InputStream inputStream;

  private long size;

  private String mimeType;

  private long fromByte;

  private long bytesCount;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long contentLength) {
    this.size = contentLength;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String contentType) {
    this.mimeType = contentType;
  }

  public long getFromByte() {
    return fromByte;
  }

  public void setFromByte(long fromByte) {
    this.fromByte = fromByte;
  }

  public long getBytesCount() {
    return bytesCount;
  }

  public void setBytesCount(long bytesCount) {
    this.bytesCount = bytesCount;
  }
}
