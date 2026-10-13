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
package org.sonatype.nexus.repository.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.swagger.SwaggerContributor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.8
 */
@Component
public class ComponentUploadParameterContributor
    implements SwaggerContributor
{
  private static final String COMPONENTS_PATH = V1_API_PREFIX + "/components";

  private static final String MULTIPART_FORM_DATA = "multipart/form-data";

  private final UploadManager uploadManager;

  @Autowired
  public ComponentUploadParameterContributor(final UploadManager uploadManager) {
    this.uploadManager = requireNonNull(uploadManager);
  }

  @Override
  public void contribute(final OpenAPI openApi) {
    if (openApi.getPaths() == null) {
      return;
    }
    PathItem item = openApi.getPaths().get(COMPONENTS_PATH);
    if (item == null) {
      return;
    }
    Operation op = item.getPost();
    if (op == null) {
      return;
    }

    RequestBody body = op.getRequestBody();
    if (body == null) {
      body = new RequestBody();
      op.setRequestBody(body);
    }
    Content content = body.getContent();
    if (content == null) {
      content = new Content();
      body.setContent(content);
    }

    // Reuse an existing multipart schema if one is already present, so we add to it rather
    // than discarding form fields contributed elsewhere.
    MediaType mediaType = content.get(MULTIPART_FORM_DATA);
    if (mediaType == null) {
      mediaType = new MediaType();
      content.addMediaType(MULTIPART_FORM_DATA, mediaType);
    }
    ObjectSchema form;
    if (mediaType.getSchema() instanceof ObjectSchema) {
      form = (ObjectSchema) mediaType.getSchema();
    }
    else {
      form = new ObjectSchema();
      mediaType.setSchema(form);
    }

    for (UploadDefinition def : uploadManager.getAvailableDefinitions()) {
      if (!def.isApiUpload()) {
        continue;
      }
      for (UploadFieldDefinition field : def.getComponentFields()) {
        String key = def.getFormat() + "." + field.getName();
        Schema<?> schema = schemaFor(field.getType());
        schema.description(def.getFormat() + " " + field.getDisplayName());
        form.addProperty(key, schema);
      }

      int slots = def.isMultipleUpload() ? 3 : 1;
      for (int i = 1; i <= slots; i++) {
        String assetIndex = def.isMultipleUpload() ? Integer.toString(i) : "";
        String assetName = def.getFormat() + ".asset" + assetIndex;
        String assetDisplayName =
            def.isMultipleUpload() ? def.getFormat() + " Asset " + assetIndex : def.getFormat() + " Asset";

        form.addProperty(assetName, new StringSchema().format("binary").description(assetDisplayName));

        for (UploadFieldDefinition assetField : def.getAssetFields()) {
          String assetFieldKey = assetName + "." + assetField.getName();
          Schema<?> assetFieldSchema = schemaFor(assetField.getType());
          assetFieldSchema.description(assetDisplayName + " " + assetField.getDisplayName());
          form.addProperty(assetFieldKey, assetFieldSchema);
        }
      }
    }
  }

  private static Schema<?> schemaFor(final UploadFieldDefinition.Type type) {
    return switch (type) {
      case BOOLEAN -> new BooleanSchema();
      // OpenAPI 3 has no "select" type; a select input is modeled as a plain string
      case STRING, SELECT -> new StringSchema();
    };
  }
}
