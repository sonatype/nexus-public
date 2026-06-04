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

import java.util.Map;

import org.sonatype.nexus.swagger.SwaggerContributor;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Swagger;
import jakarta.inject.Named;

/**
 * SwaggerContributor that post-processes the Swagger model to fix the InputStream model definition.
 * Changes the InputStream definition from type: object to type: string, format: binary.
 * This fixes all endpoints that reference #/definitions/InputStream (e.g., POST /v1/system/license).
 */
@Named
public class InputStreamSwaggerContributor
    implements SwaggerContributor
{

  @Override
  public void contribute(final Swagger swagger) {
    // Fix the InputStream model definition to be binary type
    Map<String, Model> definitions = swagger.getDefinitions();
    if (definitions != null && definitions.containsKey("InputStream")) {
      ModelImpl binaryModel = new ModelImpl();
      binaryModel.setType("string");
      binaryModel.setFormat("binary");
      definitions.put("InputStream", binaryModel);
    }
  }
}
