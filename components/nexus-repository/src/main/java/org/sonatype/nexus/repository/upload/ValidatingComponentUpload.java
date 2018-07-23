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
package org.sonatype.nexus.repository.upload;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sonatype.nexus.rest.ValidationErrorsException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * A holder of {@link ComponentUpload} that's meant to validate it based on provided {@link UploadDefinition}
 *
 * @since 3.8
 */
public class ValidatingComponentUpload
{
  private final UploadDefinition uploadDefinition;

  protected final ComponentUpload componentUpload;

  private final ValidationErrorsException validationErrorsException;

  public ValidatingComponentUpload(final UploadDefinition uploadDefinition, final ComponentUpload componentUpload) {
    this.uploadDefinition = checkNotNull(uploadDefinition);
    this.componentUpload = checkNotNull(componentUpload);
    this.validationErrorsException = new ValidationErrorsException();
  }

  public ComponentUpload getComponentUpload() {
    validate();
    return componentUpload;
  }

  private void validate() {
    validateAssetPresent();
    validateAllowedComponentFields();
    validateRequiredComponentFieldPresent();
    validateAssetFields();
    validateDuplicatesAbsent();

    if (!validationErrorsException.getValidationErrors().isEmpty()) {
      throw validationErrorsException;
    }
  }

  private void validateAssetPresent() {
    if (componentUpload.getAssetUploads().isEmpty()) {
      validationErrorsException.withError("No assets found in upload");
    }
  }

  private void validateAllowedComponentFields() {
    collectNotAllowedFields(componentUpload.getFields(), getAllowedComponentFields()).forEach(field ->
        validationErrorsException.withError(field, format("Unknown component field '%s'", field)));
  }

  protected void validateRequiredComponentFieldPresent() {
    uploadDefinition.getComponentFields().stream()
        .filter(field -> !field.isOptional())
        .filter(field -> isBlank(componentUpload.getField(field.getName())))
        .forEach(field -> validationErrorsException.withError(field.getName(),
            format("Missing required component field '%s'", field.getDisplayName())));
  }

  private void validateAssetFields() {
    for (int assetIndex = 1; assetIndex < componentUpload.getAssetUploads().size() + 1; assetIndex++) {
      AssetUpload asset = componentUpload.getAssetUploads().get(assetIndex - 1);
      validatePayloadPresent(asset, assetIndex);
      validateAllowedAssetFields(asset, assetIndex);
      validateRequiredAssetFieldPresent(asset, assetIndex);
    }
  }

  private void validateDuplicatesAbsent() {
    List<AssetUpload> uploads = componentUpload.getAssetUploads();
    Map<Map<String, String>, Integer> matches = new HashMap<>();

    IntStream.range(1, uploads.size() + 1).forEach(i -> {
      AssetUpload upload = uploads.get(i - 1);
      if (matches.containsKey(upload.getFields())) {
        validationErrorsException.withError(
            String.format("The assets %s and %s have identical coordinates", matches.get(upload.getFields()), i));
      }
      matches.put(upload.getFields(), i);
    });
  }

  private void validatePayloadPresent(final AssetUpload asset, final int assetIndex) {
    if (asset.getPayload() == null) {
      validationErrorsException.withError("file", format("Missing file on asset '%s'", assetIndex));
    }
  }

  private void validateAllowedAssetFields(final AssetUpload asset, final int assetIndex) {
    collectNotAllowedFields(asset.getFields(), getAllowedAssetFields()).forEach(field ->
        validationErrorsException.withError(field, format("Unknown field '%s' on asset '%s'", field, assetIndex)));
  }

  private void validateRequiredAssetFieldPresent(final AssetUpload asset, final int assetIndex) {
    uploadDefinition.getAssetFields().stream()
        .filter(field -> !field.isOptional())
        .filter(field -> isBlank(asset.getField(field.getName())))
        .forEach(field -> validationErrorsException.withError(field.getName(),
            format("Missing required asset field '%s' on '%s'", field.getDisplayName(), assetIndex)));
  }

  private Collection<String> collectNotAllowedFields(final Map<String, String> fields,
                                                     final Collection<String> allowedFields)
  {
    Set<String> keys = Optional.ofNullable(fields).map(Map::keySet).orElse(Collections.emptySet());
    return keys.stream().filter(field -> !allowedFields.contains(field)).collect(Collectors.toList());
  }

  private Set<String> getAllowedAssetFields() {
    return uploadDefinition.getAssetFields().stream()
        .map(UploadFieldDefinition::getName)
        .collect(Collectors.toSet());
  }

  private Set<String> getAllowedComponentFields() {
    return uploadDefinition.getComponentFields().stream()
        .map(UploadFieldDefinition::getName)
        .collect(Collectors.toSet());
  }
}
