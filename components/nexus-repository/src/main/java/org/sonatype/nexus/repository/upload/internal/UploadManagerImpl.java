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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.ValidationErrorsException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.text.Strings2.isEmpty;

/**
 * @since 3.7
 */
@Named
@Singleton
public class UploadManagerImpl
    implements UploadManager
{
  private List<UploadDefinition> uploadDefinitions;

  private Map<String, UploadHandler> uploadHandlers;

  @Inject
  public UploadManagerImpl(final Map<String, UploadHandler> uploadHandlers)
  {
    this.uploadHandlers = checkNotNull(uploadHandlers);
    this.uploadDefinitions = Collections
        .unmodifiableList(uploadHandlers.values().stream().map(handler -> handler.getDefinition()).collect(toList()));
  }

  @Override
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return uploadDefinitions;
  }

  @Override
  public Collection<String> handle(final Repository repository, final ComponentUpload upload) throws IOException {
    checkNotNull(repository);
    checkNotNull(upload);

    UploadHandler uploadHandler = getUploadHandler(repository);
    UploadDefinition uploadDefinition = uploadHandler.getDefinition();
    validateFields(uploadDefinition, upload);

    return uploadHandler.handle(repository, upload);
  }

  @Override
  public UploadDefinition getByFormat(final String format) {
    checkNotNull(format);

    UploadHandler handler = uploadHandlers.get(format);
    return handler != null ? handler.getDefinition() : null;
  }

  private UploadHandler getUploadHandler(final Repository repository)
  {
    String repositoryFormat = repository.getFormat().toString();
    UploadHandler uploadHandler = uploadHandlers.get(repositoryFormat);

    if (uploadHandler == null) {
      throw new ValidationErrorsException(
          format("Uploading components to '%s' repositories is unsupported", repositoryFormat));
    }

    return uploadHandler;
  }

  private void validateFields(final UploadDefinition uploadDefinition, final ComponentUpload componentUpload)
  {
    ValidationErrorsException validation = new ValidationErrorsException();

    validateComponentFields(uploadDefinition, componentUpload, validation);
    validateAssets(componentUpload, validation);
    validateAssetFields(uploadDefinition, componentUpload, validation);

    if (validation.hasValidationErrors()) {
      throw validation;
    }
  }

  private void validateComponentFields(final UploadDefinition uploadDefinition,
                                       final ComponentUpload componentUpload,
                                       final ValidationErrorsException validation)
  {
    for (UploadFieldDefinition componentField : uploadDefinition.getComponentFields()) {
      if (!componentField.isOptional()) {
        String componentFieldName = componentField.getName();
        String componentFieldValue = componentUpload.getFields().get(componentFieldName);

        if (isEmpty(componentFieldValue)) {
          validation.withError(componentFieldName, format("Missing required component field '%s'", componentFieldName));
        }
      }
    }
  }

  private void validateAssets(final ComponentUpload componentUpload, final ValidationErrorsException validation)
  {
    if (componentUpload.getAssetUploads().isEmpty()) {
      validation.withError("No assets found in upload");
    }
  }

  private void validateAssetFields(final UploadDefinition uploadDefinition,
                                   final ComponentUpload componentUpload,
                                   final ValidationErrorsException validation)
  {
    for (UploadFieldDefinition assetFieldDefinition : uploadDefinition.getAssetFields()) {
      if (!assetFieldDefinition.isOptional()) {
        String assetFieldName = assetFieldDefinition.getName();

        for (AssetUpload assetUpload : componentUpload.getAssetUploads()) {
          String assetFieldValue = assetUpload.getFields().get(assetFieldName);

          if (isEmpty(assetFieldValue)) {
            validation.withError(assetFieldName, format("Missing required asset field '%s'", assetFieldName));
          }
        }
      }
    }
  }
}
