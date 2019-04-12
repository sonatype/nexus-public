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

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.PartPayload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * PartPayload backed by a TempBlob. Useful for file uploads surrounded by other multipart data.
 *
 * @since 3.1
 */
public class TempBlobPartPayload
    implements PartPayload
{
  private final String name;

  private final String fieldName;

  private final boolean isFormField;

  private final String contentType;

  private final TempBlob tempBlob;

  public TempBlobPartPayload(final PartPayload payload, final TempBlob tempBlob) throws IOException {
    this.tempBlob = checkNotNull(tempBlob);
    checkNotNull(payload);
    this.name = payload.getName();
    this.fieldName = payload.getFieldName();
    this.isFormField = payload.isFormField();
    this.contentType = payload.getContentType();
  }

  /**
   * @since 3.16
   */
  public TempBlobPartPayload(final String fieldName,
                             final boolean isFormField,
                             @Nullable final String name,
                             @Nullable final String contentType,
                             final TempBlob tempBlob)
  {
    this.tempBlob = checkNotNull(tempBlob);
    this.fieldName = checkNotNull(fieldName);
    this.name = name;
    this.isFormField = isFormField;
    this.contentType = contentType;
  }

  @Nullable
  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getFieldName() {
    return fieldName;
  }

  @Override
  public boolean isFormField() {
    return isFormField;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return tempBlob.get();
  }

  @Override
  public long getSize() {
    return UNKNOWN_SIZE;
  }

  @Nullable
  @Override
  public String getContentType() {
    return contentType;
  }

  public TempBlob getTempBlob() {
    return tempBlob;
  }

  @Override
  public void close() throws IOException {
    tempBlob.close();
  }
}
