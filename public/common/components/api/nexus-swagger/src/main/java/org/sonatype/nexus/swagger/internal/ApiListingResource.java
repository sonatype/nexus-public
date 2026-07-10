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

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.rest.Resource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nexus REST endpoint serving the OpenAPI 3 description of the Nexus API.
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x ({@code io.swagger.jaxrs.listing.ApiListingResource})
 * to OpenAPI 3.x. The Swagger 1.x base class hooked into a {@code process()} template method to
 * produce a {@code Swagger} model; the OpenAPI 3.x equivalent (
 * {@code io.swagger.v3.jaxrs2.integration.resources.OpenApiResource}) does not expose a similar
 * hook, so we serve the OpenAPI document directly from {@link SwaggerModel}.
 *
 * <p>
 * The output format is selected by URL extension ({@code .json} or {@code .yaml}); the path
 * is unchanged from the Swagger 1.x predecessor for backward compatibility with consumers.
 */
@Component
@Path("/swagger.{type:json|yaml}")
public class ApiListingResource
    implements Resource
{
  private static final String APPLICATION_YAML = "application/yaml";

  private static final Logger log = LoggerFactory.getLogger(ApiListingResource.class);

  private final LoadingCache<ServerTypeKey, String> cache;

  @Autowired
  public ApiListingResource(
      final SwaggerModel swaggerModel,
      @Value("${nexus.openapi.cache.size:10}") final int maxCacheSize)
  {
    cache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(maxCacheSize)
        .build(new OpenApiCacheLoader(swaggerModel));
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON, APPLICATION_YAML})
  public Response getOpenApi(@PathParam("type") final String type) throws Exception {
    boolean isYaml = isYamlType(type);
    String body = cache.getUnchecked(new ServerTypeKey(getUrl(), isYaml));

    final String contentType = isYaml ? APPLICATION_YAML : MediaType.APPLICATION_JSON;

    return Response.ok(body, contentType).build();
  }

  private static String getUrl() {
    try {
      return Strings.CI.appendIfMissing(URI.create(BaseUrlHolder.get()).getPath(), "/");
    }
    catch (Exception e) {
      log.debug("An error occurred trying to extract the base path", e);
      return "/";
    }
  }

  private static boolean isYamlType(final String type) {
    return "yaml".equalsIgnoreCase(type);
  }

  private static final class OpenApiCacheLoader
      extends CacheLoader<ServerTypeKey, String>
  {
    private final SwaggerModel swaggerModel;

    private OpenApiCacheLoader(final SwaggerModel swaggerModel) {
      this.swaggerModel = checkNotNull(swaggerModel);
    }

    /*
     * Synchronized due to the model being shared
     */
    @Override
    public synchronized String load(final ServerTypeKey key) throws Exception {
      OpenAPI openApi = swaggerModel.getOpenApi();
      openApi.setServers(List.of(new Server().url(key.baseUrl + "service/rest")));

      String body;
      if (key.isYaml()) {
        body = Yaml.pretty().writeValueAsString(openApi);
      }
      else {
        body = Json.pretty().writeValueAsString(openApi);
      }
      return body;
    }
  }

  private record ServerTypeKey(String baseUrl, boolean isYaml)
  {
  }
}
