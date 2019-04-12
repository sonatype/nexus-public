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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.storage.TempBlob;

/**
 * @since 3.16
 */
public class BlobStoreMultipartForm implements AutoCloseable
{
  private Map<String, TempBlobFormField> files = new HashMap<>();

  private Map<String, String> formFields = new HashMap<>();

  public TempBlobFormField getFile(final String fileName) {
    return files.get(fileName);
  }

  public Map<String, TempBlobFormField> getFiles() {
    return files;
  }

  public String getFormField(final String formField) {
    return formFields.get(formField);
  }

  public Map<String, String> getFormFields() {
    return formFields;
  }

  public void putFile(final String fieldName, final TempBlobFormField file) {
    files.put(fieldName, file);
  }

  public void putFormField(final String name, final String value) {
    formFields.put(name, value);
  }

  @Override
  public void close() throws Exception {
    for (TempBlobFormField file : files.values()) {
      file.getTempBlob().close();
    }
  }

  public static class TempBlobFormField
  {
    private final String fieldName;

    private final String fileName;

    private final TempBlob tempBlob;

    public TempBlobFormField(final String fieldName, final String fileName, final TempBlob tempBlob) {
      this.fieldName = fieldName;
      this.fileName = fileName;
      this.tempBlob = tempBlob;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String getFileName() {
      return fileName;
    }

    public TempBlob getTempBlob() {
      return tempBlob;
    }
  }
}
