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
package org.sonatype.nexus.swagger.internal;

import org.sonatype.nexus.swagger.SwaggerContributor;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

/**
 * SwaggerContributor that post-processes the OpenAPI model to fix the InputStream schema.
 * Changes the InputStream schema from type: object to type: string, format: binary.
 * This fixes all endpoints that reference #/components/schemas/InputStream (e.g., POST /v1/system/license).
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x to OpenAPI 3.x. The {@code definitions} map at the top
 * level was relocated under {@code components.schemas} in OpenAPI 3.0.
 */
@Component
public class InputStreamSwaggerContributor
    implements SwaggerContributor
{
  @Override
  public void contribute(final OpenAPI openApi) {
    Components components = openApi.getComponents();
    if (components == null) {
      return;
    }
    if (components.getSchemas() == null) {
      return;
    }
    if (!components.getSchemas().containsKey("InputStream")) {
      return;
    }
    Schema<?> binarySchema = new StringSchema().format("binary");
    components.getSchemas().put("InputStream", binarySchema);
  }
}
