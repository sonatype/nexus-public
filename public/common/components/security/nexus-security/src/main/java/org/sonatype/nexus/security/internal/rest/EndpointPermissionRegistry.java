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
package org.sonatype.nexus.security.internal.rest;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.internal.rest.PermissionMappingService.ApiEndpointMapping;
import org.sonatype.nexus.swagger.internal.SwaggerModel;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builds the UI-facing permission map from JAX-RS resources, merged with {@link PermissionMappingService} rows.
 */
@Component
@Singleton
public class EndpointPermissionRegistry
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final PermissionMappingService permissionMappingService;

  private final ObjectProvider<SwaggerModel> swaggerModelProvider;

  private record RegistryState(List<ApiEndpointPermission> endpoints, Map<String, Pattern> patterns)
  {
  }

  private volatile RegistryState state = new RegistryState(List.of(), Map.of());

  private volatile Instant generatedAt = Instant.EPOCH;

  private volatile int unmappedSwaggerOperations;

  private volatile boolean ready;

  @Inject
  public EndpointPermissionRegistry(
      final PermissionMappingService permissionMappingService,
      final ObjectProvider<SwaggerModel> swaggerModelProvider)
  {
    this.permissionMappingService = checkNotNull(permissionMappingService);
    this.swaggerModelProvider = checkNotNull(swaggerModelProvider);
  }

  /**
   * Rebuild after the application context (and {@link SwaggerModel}) has refreshed.
   */
  @EventListener
  @Order(Ordered.LOWEST_PRECEDENCE)
  public void onContextRefreshed(final ContextRefreshedEvent event) {
    try {
      Map<String, Resource> beans = event.getApplicationContext().getBeansOfType(Resource.class);
      List<Class<? extends Resource>> classes = new ArrayList<>();
      for (Resource resource : beans.values()) {
        @SuppressWarnings("unchecked")
        Class<? extends Resource> userClass =
            (Class<? extends Resource>) org.springframework.util.ClassUtils.getUserClass(resource.getClass());
        if (!classes.contains(userClass)) {
          classes.add(userClass);
        }
      }

      Map<String, ApiEndpointPermission> byKey = new LinkedHashMap<>();
      for (ApiEndpointPermission scanned : EndpointPermissionScanner.scan(classes)) {
        byKey.put(
            EndpointPermissionScanner.endpointKey(scanned.getHttpMethod(), scanned.getPathPattern()),
            scanned);
      }

      applyManualMappings(byKey);

      SwaggerModel swaggerModel = swaggerModelProvider.getIfAvailable();
      if (swaggerModel != null) {
        unmappedSwaggerOperations = countAndLogUnmapped(swaggerModel.getSwagger(), byKey);
      }
      else {
        unmappedSwaggerOperations = 0;
        log.debug("SwaggerModel unavailable; skipping swagger conformance logging");
      }

      List<ApiEndpointPermission> newEndpoints = new ArrayList<>(byKey.values());
      Map<String, Pattern> patterns = new LinkedHashMap<>();
      for (ApiEndpointPermission ep : newEndpoints) {
        String p = ep.getPathPattern();
        if (p != null && p.contains("{")) {
          patterns.put(p, Pattern.compile(EndpointPermissionScanner.buildTemplateRegex(p)));
        }
      }
      state = new RegistryState(newEndpoints, Map.copyOf(patterns));
      generatedAt = Instant.now();
      ready = true;
      log.info("Built API endpoint permission registry with {} entries", newEndpoints.size());
    }
    catch (Exception e) {
      log.error("Failed to build API endpoint permission registry", e);
      state = new RegistryState(List.of(), Map.of());
      ready = false;
      unmappedSwaggerOperations = 0;
    }
  }

  private void applyManualMappings(final Map<String, ApiEndpointPermission> byKey) {
    for (ApiEndpointMapping mapping : permissionMappingService.getAllEndpointMappings()) {
      String key = EndpointPermissionScanner.endpointKey(mapping.getMethod(), mapping.getEndpoint());
      List<ApiPermissionRequirement> requirements = new ArrayList<>();
      if (StringUtils.isNotBlank(mapping.getPermission())) {
        requirements.add(new ApiPermissionRequirement(mapping.getPermission(), "AND"));
      }
      boolean authenticated = StringUtils.isNotBlank(mapping.getPermission());
      ApiEndpointPermission manual = new ApiEndpointPermission(
          mapping.getMethod().toUpperCase(Locale.US),
          EndpointPermissionScanner.normalizeFullPath(mapping.getEndpoint()),
          requirements,
          mapping.getDescription(),
          null,
          authenticated);
      if (byKey.containsKey(key)) {
        log.debug("Manual mapping for {} {} overrides scanned annotation", mapping.getMethod(), mapping.getEndpoint());
      }
      byKey.put(key, manual);
    }
  }

  private int countAndLogUnmapped(final Swagger swagger, final Map<String, ApiEndpointPermission> byKey) {
    if (swagger == null || swagger.getPaths() == null) {
      return 0;
    }
    String base = swagger.getBasePath();
    if (StringUtils.isBlank(base)) {
      base = EndpointPermissionScanner.SERVICE_REST_PREFIX;
    }
    base =
        EndpointPermissionScanner.normalizeFullPath(base.endsWith("/") ? base.substring(0, base.length() - 1) : base);

    int unmapped = 0;
    for (Map.Entry<String, io.swagger.models.Path> pathEntry : swagger.getPaths().entrySet()) {
      String rel = pathEntry.getKey();
      if (!rel.startsWith("/")) {
        rel = "/" + rel;
      }
      String fullPath = EndpointPermissionScanner.normalizeFullPath(base + rel);
      io.swagger.models.Path pathModel = pathEntry.getValue();
      if (pathModel == null) {
        continue;
      }
      for (Map.Entry<HttpMethod, Operation> opEntry : pathModel.getOperationMap().entrySet()) {
        HttpMethod method = opEntry.getKey();
        if (method == null) {
          continue;
        }
        String httpMethod = method.name();
        String key = EndpointPermissionScanner.endpointKey(httpMethod, fullPath);
        ApiEndpointPermission row = byKey.get(key);
        if (row == null || row.getPermissions() == null || row.getPermissions().isEmpty()) {
          log.warn(
              "Swagger operation {} {} has no @RequiresPermissions mapping in the API permission registry",
              httpMethod,
              fullPath);
          unmapped++;
        }
      }
    }
    return unmapped;
  }

  /**
   * Returns the first permission string required by the given endpoint and HTTP method.
   * Looks up from the runtime-scanned registry (no static catalog drift).
   * Returns null if the endpoint is public / not found.
   */
  @Nullable
  public String getPermissionForEndpoint(final String endpoint, final String method) {
    if (endpoint == null || method == null) {
      return null;
    }
    String normalizedEndpoint = EndpointPermissionScanner.normalizeFullPath(
        endpoint.contains("?") ? endpoint.substring(0, endpoint.indexOf('?')) : endpoint);
    String upperMethod = method.toUpperCase(Locale.US);

    RegistryState current = state; // single read — both loops see the same consistent snapshot
    // Exact match first — literals must beat templates regardless of list order
    for (ApiEndpointPermission ep : current.endpoints()) {
      if (upperMethod.equals(ep.getHttpMethod()) && normalizedEndpoint.equals(ep.getPathPattern())) {
        return firstPermission(ep);
      }
    }
    // Pattern match (JAX-RS path templates like {id})
    for (ApiEndpointPermission ep : current.endpoints()) {
      if (upperMethod.equals(ep.getHttpMethod())
          && matchesTemplate(current.patterns(), ep.getPathPattern(), normalizedEndpoint)) {
        String perm = firstPermission(ep);
        if (perm != null) {
          return perm;
        }
        // null-permission match (unannotated scanned template): keep iterating for a manual mapping
      }
    }
    return null;
  }

  /**
   * Returns all endpoints whose required permission exactly matches the given permission string.
   */
  public List<ApiEndpointPermission> getEndpointsForPermission(final String permission) {
    if (permission == null) {
      return List.of();
    }

    return state.endpoints()
        .stream()
        .filter(ep -> {
          if (ep.getPermissions() == null || ep.getPermissions().isEmpty()) {
            return false;
          }
          return ep.getPermissions()
              .stream()
              .anyMatch(req -> permission.equals(req.getPermission()));
        })
        .collect(Collectors.toUnmodifiableList());
  }

  @Nullable
  private static String firstPermission(final ApiEndpointPermission ep) {
    if (ep.getPermissions() == null || ep.getPermissions().isEmpty()) {
      return null;
    }
    ApiPermissionRequirement req = ep.getPermissions().get(0);
    return req != null ? req.getPermission() : null;
  }

  /**
   * Returns the full permission metadata for the given endpoint and HTTP method.
   * Returns null if the endpoint is not found or has no registered permission.
   */
  @Nullable
  public ApiEndpointPermission getEndpointInfo(final String endpoint, final String method) {
    if (endpoint == null || method == null) {
      return null;
    }
    String normalizedEndpoint = EndpointPermissionScanner.normalizeFullPath(
        endpoint.contains("?") ? endpoint.substring(0, endpoint.indexOf('?')) : endpoint);
    String upperMethod = method.toUpperCase(Locale.US);

    RegistryState current = state; // single read — both loops see the same consistent snapshot
    // Exact match first — literals must beat templates regardless of list order
    for (ApiEndpointPermission ep : current.endpoints()) {
      if (upperMethod.equals(ep.getHttpMethod()) && normalizedEndpoint.equals(ep.getPathPattern())) {
        return ep;
      }
    }
    for (ApiEndpointPermission ep : current.endpoints()) {
      if (upperMethod.equals(ep.getHttpMethod())
          && matchesTemplate(current.patterns(), ep.getPathPattern(), normalizedEndpoint)) {
        if (firstPermission(ep) != null) {
          return ep;
        }
      }
    }
    return null;
  }

  // Assumes JAX-RS static path segments contain no regex metacharacters.
  private static boolean matchesTemplate(
      final Map<String, Pattern> patterns,
      final String template,
      final String path)
  {
    if (template == null) {
      return false;
    }
    Pattern compiled = patterns.get(template);
    if (compiled == null) {
      return false;
    }
    return compiled.matcher(path).matches();
  }

  public boolean isReady() {
    return ready;
  }

  public List<ApiEndpointPermission> getEndpoints() {
    return state.endpoints();
  }

  public int getUnmappedSwaggerOperations() {
    return unmappedSwaggerOperations;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /**
   * Snapshot for REST responses (immutable copy).
   */
  public ApiPermissionsResponse snapshot(@Nullable final String error) {
    String ts = DateTimeFormatter.ISO_INSTANT.format(generatedAt);
    List<ApiEndpointPermission> snap = state.endpoints();
    return new ApiPermissionsResponse(
        new ArrayList<>(snap),
        ts,
        snap.size(),
        unmappedSwaggerOperations,
        error);
  }
}
