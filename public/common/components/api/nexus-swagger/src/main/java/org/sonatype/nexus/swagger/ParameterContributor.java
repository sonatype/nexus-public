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
package org.sonatype.nexus.swagger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * A custom {@link SwaggerContributor} that contributes parameters to the {@link OpenAPI}
 * definition for a given {@link HttpMethod} for all the paths provided.
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x to OpenAPI 3.x. Key changes:
 * <ul>
 * <li>{@code io.swagger.models.Swagger} \u2192 {@link OpenAPI}</li>
 * <li>{@code io.swagger.models.Path} \u2192 {@link PathItem}</li>
 * <li>{@code io.swagger.models.HttpMethod} \u2192 {@link PathItem.HttpMethod}</li>
 * <li>{@code AbstractSerializableParameter} \u2192 {@link Parameter} (concrete; uses {@code in})</li>
 * <li>{@code Path.getOperationMap()} \u2192 {@link PathItem#readOperationsMap()}</li>
 * <li>{@code Operation.addParameter()} \u2192 {@link Operation#addParametersItem(Parameter)}</li>
 * </ul>
 */
public abstract class ParameterContributor<T extends Parameter>
    implements SwaggerContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Collection<HttpMethod> httpMethods;

  private final Collection<String> paths;

  private final Collection<T> params;

  @VisibleForTesting
  final Map<String, Boolean> contributed;

  private boolean allContributed;

  public ParameterContributor(
      final Collection<HttpMethod> httpMethods,
      final Collection<String> paths,
      final Collection<T> params)
  {
    this.httpMethods = checkNotNull(httpMethods);
    this.paths = checkNotNull(paths);
    this.params = checkNotNull(params);
    this.contributed = httpMethods.stream()
        .flatMap(httpMethod -> paths.stream().map(path -> getKey(httpMethod, path)))
        .collect(toMap(p -> (String) p, p -> false));
  }

  @Override
  public void contribute(final OpenAPI openApi) {
    if (allContributed) {
      return;
    }

    for (HttpMethod httpMethod : httpMethods) {
      for (String path : paths) {
        contributed.compute(getKey(httpMethod, path),
            (key, value) -> value || contributeGetParameters(openApi, httpMethod, path, params));
      }
    }

    allContributed = contributed.entrySet().stream().allMatch(Entry::getValue);
  }

  private boolean contributeGetParameters(
      final OpenAPI openApi,
      final HttpMethod httpMethod,
      final String path,
      final Collection<T> parameters)
  {
    boolean contrib = false;
    Optional<Operation> operation = getOperation(openApi, httpMethod, path);
    if (operation.isPresent()) {
      final Operation op = operation.get();
      parameters.forEach(param -> {
        if (op.getParameters() == null || !op.getParameters().contains(param)) {
          log.debug("adding {}, method: {}, path: {}, parameter: {}",
              param.getClass().getSimpleName(), httpMethod, path, param.getName());
          op.addParametersItem(param);
        }
      });
      contrib = true;
    }
    return contrib;
  }

  private Optional<Operation> getOperation(final OpenAPI openApi, final HttpMethod httpMethod, final String path) {
    return Optional.ofNullable(openApi.getPaths())
        .map(paths -> (Map<String, PathItem>) paths)
        .orElseGet(Collections::emptyMap)
        .entrySet()
        .stream()
        .filter(e -> path.equals(e.getKey()))
        .findFirst()
        .map(Entry::getValue)
        .map(PathItem::readOperationsMap)
        .map(m -> m.get(httpMethod));
  }

  private static String getKey(final HttpMethod httpMethod, final String path) {
    return format("%s-%s", httpMethod.name(), path);
  }
}
