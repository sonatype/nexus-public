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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonatype.nexus.swagger.SwaggerContributor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import jakarta.inject.Named;

/**
 * Replaces auto-generated numeric operationIds (e.g. {@code createRepository_2}) with stable,
 * path-derived names (e.g. {@code createRepositoriesMavenProxy}).
 *
 * The operationId is built from the HTTP method verb + PascalCase path segments, skipping
 * version prefixes ({@code v1}, {@code beta}) and path parameters ({@code {name}}).
 */
@Named
public class OperationIdSwaggerContributor
    implements SwaggerContributor
{
  // NEXUS-46395: ported from Swagger 1.x (io.swagger.models.Swagger) to OpenAPI 3.x
  // (io.swagger.v3.oas.models.OpenAPI). Type names map cleanly:
  // io.swagger.models.Swagger -> io.swagger.v3.oas.models.OpenAPI
  // io.swagger.models.HttpMethod -> io.swagger.v3.oas.models.PathItem.HttpMethod
  // io.swagger.models.Operation -> io.swagger.v3.oas.models.Operation
  // io.swagger.models.Path -> io.swagger.v3.oas.models.PathItem
  // Plus path.getOperationMap() -> pathItem.readOperationsMap() (same Map<HttpMethod,Operation> shape).
  @Override
  public void contribute(final OpenAPI openApi) {
    if (openApi == null || openApi.getPaths() == null) {
      return;
    }
    openApi.getPaths().forEach((pathStr, pathItem) -> {
      if (pathItem == null) {
        return;
      }
      Map<HttpMethod, Operation> operationMap = pathItem.readOperationsMap();
      if (operationMap == null) {
        return;
      }
      operationMap.forEach((httpMethod, operation) -> {
        if (operation != null) {
          operation.setOperationId(generateOperationId(httpMethod, pathStr));
        }
      });
    });
  }

  private static String generateOperationId(final HttpMethod httpMethod, final String path) {
    return methodToVerb(httpMethod, path) + toPathPart(path);
  }

  private static String methodToVerb(final HttpMethod httpMethod, final String path) {
    switch (httpMethod) {
      case POST:
        return "create";
      case PUT:
      case PATCH:
        return "update";
      case DELETE:
        return "delete";
      case GET:
        return path.contains("{") ? "get" : "list";
      default:
        return httpMethod.name().toLowerCase();
    }
  }

  private static String toPathPart(final String path) {
    return Arrays.stream(path.split("/"))
        .filter(s -> !s.isEmpty())
        .filter(s -> !s.matches("v\\d+") && !s.equals("beta"))
        .filter(s -> !s.startsWith("{"))
        .map(OperationIdSwaggerContributor::toPascalCase)
        .collect(Collectors.joining());
  }

  private static String toPascalCase(final String segment) {
    return Arrays.stream(segment.split("[-_]"))
        .filter(w -> !w.isEmpty())
        .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
        .collect(Collectors.joining());
  }
}
