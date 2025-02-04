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
package org.sonatype.nexus.repository.rest.api;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Objects;

import org.sonatype.nexus.repository.upload.UploadDefinition;

/**
 * Upload definition transfer object for rest api
 */
public class UploadDefinitionXO
{
  private String format;

  private boolean multipleUpload;

  private List<UploadFieldDefinitionXO> componentFields;

  private List<UploadFieldDefinitionXO> assetFields;

  public static UploadDefinitionXO from(final UploadDefinition uploadDefinition) {
    return builder()
        .format(uploadDefinition.getFormat())
        .multipleUpload(uploadDefinition.isMultipleUpload())
        .componentFields(uploadDefinition.getComponentFields()
            .stream()
            .map(UploadFieldDefinitionXO::from)
            .collect(Collectors.toCollection(ArrayList::new)))
        .assetFields(uploadDefinition.getAssetFields()
            .stream()
            .map(UploadFieldDefinitionXO::from)
            .collect(Collectors.toCollection(ArrayList::new)))
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadDefinitionXO that = (UploadDefinitionXO) o;
    return Objects.equals(format, that.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(format);
  }

  static UploadDefinitionXOBuilder builder() {
    return new UploadDefinitionXOBuilder();
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public boolean getMultipleUpload() {
    return multipleUpload;
  }

  public void setMultipleUpload(boolean multipleUpload) {
    this.multipleUpload = multipleUpload;
  }

  public List<UploadFieldDefinitionXO> getComponentFields() {
    return componentFields;
  }

  public void setComponentFields(List<UploadFieldDefinitionXO> componentFields) {
    this.componentFields = componentFields;
  }

  public List<UploadFieldDefinitionXO> getAssetFields() {
    return assetFields;
  }

  public void setAssetFields(List<UploadFieldDefinitionXO> assetFields) {
    this.assetFields = assetFields;
  }

  public static class UploadDefinitionXOBuilder
  {
    private String format;

    private boolean multipleUpload;

    private List<UploadFieldDefinitionXO> componentFields;

    private List<UploadFieldDefinitionXO> assetFields;

    public UploadDefinitionXOBuilder format(String format) {
      this.format = format;
      return this;
    }

    public UploadDefinitionXOBuilder multipleUpload(boolean multipleUpload) {
      this.multipleUpload = multipleUpload;
      return this;
    }

    public UploadDefinitionXOBuilder componentFields(List<UploadFieldDefinitionXO> componentFields) {
      this.componentFields = componentFields;
      return this;
    }

    public UploadDefinitionXOBuilder assetFields(List<UploadFieldDefinitionXO> assetFields) {
      this.assetFields = assetFields;
      return this;
    }

    public UploadDefinitionXO build() {
      UploadDefinitionXO uploadDefinitionXO = new UploadDefinitionXO();
      uploadDefinitionXO.setFormat(format);
      uploadDefinitionXO.setMultipleUpload(multipleUpload);
      uploadDefinitionXO.setComponentFields(componentFields);
      uploadDefinitionXO.setAssetFields(assetFields);
      return uploadDefinitionXO;
    }
  }
}
