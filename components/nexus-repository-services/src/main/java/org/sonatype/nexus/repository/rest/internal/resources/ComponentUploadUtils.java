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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.internal.BlobStoreMultipartForm;
import org.sonatype.nexus.repository.upload.internal.BlobStoreMultipartForm.TempBlobFormField;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;

/**
 * Utility for processing component upload data.
 *
 * @since 3.8
 */
public class ComponentUploadUtils
{
  private ComponentUploadUtils() {
    // empty
  }

  /**
   * Converts multipart form into ComponentUpload.
   *
   * @since 3.16
   *
   * @param format the repository format
   * @param multipartInput the multipart form
   * @return the ComponentUpload
   * @throws IOException
   */
  public static ComponentUpload createComponentUpload(final String format, final BlobStoreMultipartForm multipartInput)
      throws IOException
  {
    Map<String, TempBlobFormField> assetsPayloads = mapFields(format, multipartInput.getFiles());
    Map<String, String> formFields = mapFields(format, multipartInput.getFormFields());
    Map<String, Map<String, String>> assetFields = new HashMap<>();
    Map<String, String> componentFields = new HashMap<>();

    formFields.forEach((key, value) -> {
      if (Strings2.isBlank(value)) {
        return;
      }
      int indexOfDot = key.indexOf('.');
      boolean isAssetField = (indexOfDot != -1) && (key.length() > indexOfDot + 2)
          && assetsPayloads.containsKey(key.substring(0, indexOfDot));
      if (isAssetField) {
        String assetName = key.substring(0, indexOfDot);
        assetFields.putIfAbsent(assetName, new HashMap<>());
        assetFields.get(assetName).put(key.substring(indexOfDot + 1), value);
      }
      else {
        componentFields.put(key, value);
      }
    });

    List<AssetUpload> assetUploads = assetsPayloads.entrySet().stream()
        .map(asset -> createAssetUpload(asset.getValue(), assetFields.get(asset.getKey())))
        .collect(Collectors.toList());

    ComponentUpload componentUpload = new ComponentUpload();
    componentUpload.setFields(componentFields);
    componentUpload.setAssetUploads(assetUploads);
    return componentUpload;
  }

  /**
   * Map field names from API to internal
   */
  private static <T> Map<String, T> mapFields(final String format, final Map<String, T> assetBlobs) {
    if (format == null) {
      return assetBlobs;
    }
    Map<String, T> result = new HashMap<>();
    for (Entry<String, T> formField : assetBlobs.entrySet()) {
      if (formField.getKey().startsWith(format + '.')) {
        result.put(formField.getKey().substring(format.length() + 1), formField.getValue());
      }
      else {
        result.put(formField.getKey(), formField.getValue());
      }
    }

    return result;
  }

  private static AssetUpload createAssetUpload(final TempBlobFormField assetPayload,
                                               final Map<String, String> assetFields)
  {
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(new TempBlobPartPayload(assetPayload.getFieldName(), false, assetPayload.getFileName(), null,
        assetPayload.getTempBlob()));
    if (assetFields != null) {
      assetUpload.setFields(assetFields);
    }
    return assetUpload;
  }
}
