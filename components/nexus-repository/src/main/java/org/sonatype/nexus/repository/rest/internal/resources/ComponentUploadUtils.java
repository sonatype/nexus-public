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
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.view.PartPayload;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

/**
 * Utility for processing component upload data.
 *
 * @since 3.8
 */
class ComponentUploadUtils
{
  private static final Pattern FIELD_NAME_PATTERN = Pattern.compile(".*\\sname\\s*=[\'\"\\s]?([^\'\";]+).*");

  private static final Pattern FILENAME_PATTERN = Pattern.compile(".*\\sfilename\\s*=[\'\"\\s]?([^\'\";]+).*");

  private ComponentUploadUtils() {
    // empty
  }

  /**
   * Extract the value from using given pattern from the Content Disposition header
   * Example: Content-Disposition: form-data; name="fieldName"; filename="filename.jpg"
   * with a pattern of .*\sfilename\s*=['"\s]?([^'";]+).* will return filename.jpg
   *
   * @param header       Content Disposition header
   * @param fieldPattern Regex pattern to find the field
   * @param formatPrefix Format name used as a prefix that shall be removed
   * @return value of the 'name' parameter
   */
  private static Optional<String> extractValue(final String header, final Pattern fieldPattern, final Optional<String> formatPrefix) {
    Matcher matcher = fieldPattern.matcher(header);
    if (matcher.matches()) {
      String value = matcher.group(1);
      if (formatPrefix.isPresent()) {
        value = value.replaceFirst("^" + formatPrefix.get() + "\\.", "");
      }
      return Optional.of(value);
    }
    else {
      return Optional.empty();
    }
  }

  private static Optional<String> extractFieldName(final String format, final String header) {
    return extractValue(header, FIELD_NAME_PATTERN, Optional.of(format));
  }

  private static Optional<String> extractFilename(final String header) {
    return extractValue(header, FILENAME_PATTERN, Optional.empty());
  }

  static ComponentUpload createComponentUpload(final String format, final MultipartInput multipartInput)
      throws IOException
  {
    Map<String, InputStreamPartPayload> assetsPayloads = getAssetsPayloads(format, multipartInput);
    Map<String, String> formFields = getTextFormFields(format, multipartInput, assetsPayloads.keySet());
    Map<String, Map<String, String>> assetFields = new HashMap<>();
    Map<String, String> componentFields = new HashMap<>();

    formFields.forEach((key, value) -> {
      int indexOfDot = key.indexOf('.');
      boolean isAssetField = (indexOfDot != -1) && (key.length() > indexOfDot + 2) &&
          assetsPayloads.containsKey(key.substring(0, indexOfDot));
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

  private static AssetUpload createAssetUpload(final InputStreamPartPayload assetPayload,
                                               final Map<String, String> assetFields)
  {
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(assetPayload);
    assetUpload.setFields(assetFields);
    return assetUpload;
  }

  private static Map<String, String> getTextFormFields(final String format, final MultipartInput multipartInput, final Set<String> assetNames)
      throws IOException
  {
    Map<String, String> fields = new HashMap<>();
    List<InputPart> fieldsParts = multipartInput.getParts().stream()
        .filter(part -> TEXT_PLAIN_TYPE.isCompatible(part.getMediaType()))
        .collect(toList());

    for (InputPart inputPart : fieldsParts) {
      Optional<String> maybeContentDisposition = getContentDisposition(inputPart);
      if (maybeContentDisposition.isPresent()) {
        Optional<String> maybeFieldName = extractFieldName(format, maybeContentDisposition.get());
        if (maybeFieldName.isPresent() && !assetNames.contains(maybeFieldName.get())) {
          fields.put(maybeFieldName.get(), inputPart.getBodyAsString());
        }
      }
    }
    return fields;
  }

  private static Optional<String> getContentDisposition(final InputPart inputPart) {
    return Optional.ofNullable(inputPart.getHeaders().getFirst(CONTENT_DISPOSITION));
  }

  private static Map<String, InputStreamPartPayload> getAssetsPayloads(final String format,
                                                                       final MultipartInput multipartInput)
      throws IOException
  {
    Map<String, InputStreamPartPayload> payloads = new HashMap<>();
    for (InputPart inputPart : multipartInput.getParts()) {
      Optional<String> maybeContentDisposition = getContentDisposition(inputPart);
      if (maybeContentDisposition.isPresent()) {
        String contentDisposition = maybeContentDisposition.get();
        Optional<String> maybeFilename = extractFilename(contentDisposition);
        if (maybeFilename.isPresent()) {
          String name = extractFieldName(format, contentDisposition).orElse(maybeFilename.get());
          InputStream inputStream = inputPart.getBody(InputStream.class, null);
          payloads.put(name, new InputStreamPartPayload(maybeFilename.get(), name, inputStream, inputPart.getMediaType().toString()));
        }
      }
    }
    return payloads;
  }

  private static class InputStreamPartPayload
      implements PartPayload
  {

    final String name;

    final String fieldName;

    final InputStream inputStream;

    final String contentType;

    InputStreamPartPayload(final String name,
                           final String fieldName,
                           final InputStream inputStream,
                           final String contentType)
    {
      this.name = name;
      this.fieldName = fieldName;
      this.inputStream = inputStream;
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
      return false;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return inputStream;
    }

    @Override
    public long getSize() {
      try {
        return inputStream.available();
      }
      catch (IOException ignored) {
        return 0;
      }
    }

    @Nullable
    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public void close() throws IOException {
      inputStream.close();
    }
  }
}
