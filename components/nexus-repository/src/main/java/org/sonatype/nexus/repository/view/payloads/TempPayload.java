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

import org.sonatype.nexus.common.io.TempStreamSupplier;
import org.sonatype.nexus.repository.view.PartPayload;

/**
 * PartPayload backed by a TempStreamSupplier. Useful for file uploads surrounded by other multipart data.
 *
 * @since 3.1
 */
public class TempPayload
    implements PartPayload, AutoCloseable
{
  private final PartPayload payload;

  private final TempStreamSupplier supplier;

  public TempPayload(final PartPayload payload) throws IOException {
    this.payload = payload;
    this.supplier = new TempStreamSupplier(payload.openInputStream());
  }

  @Nullable
  @Override
  public String getName() {
    return payload.getName();
  }

  @Override
  public String getFieldName() {
    return payload.getFieldName();
  }

  @Override
  public boolean isFormField() {
    return payload.isFormField();
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return supplier.get();
  }

  @Override
  public long getSize() {
    return UNKNOWN_SIZE;
  }

  @Nullable
  @Override
  public String getContentType() {
    return payload.getContentType();
  }

  @Override
  public void close() throws IOException {
    supplier.close();
  }
}
