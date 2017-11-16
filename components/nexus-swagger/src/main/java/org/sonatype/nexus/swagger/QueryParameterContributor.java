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

import org.sonatype.goodies.common.ComponentSupport;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.QueryParameter;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

/**
 * A custom {@link SwaggerContributor} that contributes query parameters to the {@link Swagger}
 * definition for a given {@link HttpMethod} for all the paths provided.
 *
 * @since 3.7
 */
public abstract class QueryParameterContributor
    extends ComponentSupport
    implements SwaggerContributor
{
  private final HttpMethod httpMethod;

  private final Collection<String> paths;

  private final Collection<QueryParameter> params;

  private final Map<String, Boolean> contributed;

  private boolean allContributed;

  public QueryParameterContributor(final HttpMethod httpMethod,
                                   final Collection<String> paths,
                                   final Collection<QueryParameter> params)
  {
    this.httpMethod = checkNotNull(httpMethod);
    this.paths = checkNotNull(paths);
    this.params = checkNotNull(params);
    this.contributed = paths.stream().collect(toMap(p -> p, p -> false));
  }

  @Override
  public void contribute(final Swagger swagger) {
    if (allContributed) {
      return;
    }

    paths.forEach(p -> {
      if (!contributed.get(p) && contributeGetParameters(swagger, httpMethod, p, params)) {
        contributed.put(p, true);
      }
    });

    allContributed = contributed.entrySet().stream().allMatch(Entry::getValue);
  }

  private boolean contributeGetParameters(final Swagger swagger,
                                          final HttpMethod httpMethod,
                                          final String path,
                                          final Collection<QueryParameter> queryParams)
  {
    boolean contrib = false;
    Optional<Operation> operation = getOperation(swagger, httpMethod, path);
    if (operation.isPresent()) {
      final Operation op = operation.get();
      queryParams.forEach(qp -> {
        if (!op.getParameters().contains(qp)) {
          log.debug("adding query parameter, method: {}, path: {}, parameter: {}", httpMethod, path, qp.getName());
          op.addParameter(qp);
        }
      });
      contrib = true;
    }
    return contrib;
  }

  private Optional<Operation> getOperation(final Swagger swagger, final HttpMethod httpMethod, final String path) {
    return Optional.ofNullable(swagger.getPaths())
        .orElseGet(Collections::emptyMap)
        .entrySet().stream()
        .filter(e -> path.equals(e.getKey()))
        .findFirst()
        .map(Entry::getValue)
        .map(Path::getOperationMap)
        .map(m -> m.get(httpMethod));
  }
}
