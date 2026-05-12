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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.sonatype.nexus.rest.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflectively scans JAX-RS {@link Resource} classes for HTTP metadata, Shiro security annotations, and Swagger tags.
 */
public final class EndpointPermissionScanner
{
  static final String SERVICE_REST_PREFIX = "/service/rest";

  private EndpointPermissionScanner() {
  }

  /**
   * @param resourceClasses concrete resource classes (typically {@code Resource} implementations)
   */
  public static List<ApiEndpointPermission> scan(final Iterable<Class<? extends Resource>> resourceClasses) {
    Map<String, ApiEndpointPermission> byKey = new LinkedHashMap<>();
    for (Class<? extends Resource> clazz : resourceClasses) {
      Class<?> userClass = org.springframework.util.ClassUtils.getUserClass(clazz);
      scanClass(userClass, byKey);
    }
    return new ArrayList<>(byKey.values());
  }

  private static void scanClass(final Class<?> type, final Map<String, ApiEndpointPermission> byKey) {
    Path typePath = type.getAnnotation(Path.class);
    if (typePath == null) {
      return;
    }
    String typeSegment = trimSlashes(typePath.value());
    RequiresPermissions typePermissions = type.getAnnotation(RequiresPermissions.class);
    RequiresAuthentication typeAuth = type.getAnnotation(RequiresAuthentication.class);
    Api typeApi = type.getAnnotation(Api.class);

    for (Method method : type.getMethods()) {
      if (method.isBridge() || method.isSynthetic()) {
        continue;
      }
      Optional<String> httpMethod = httpVerb(method);
      if (httpMethod.isEmpty()) {
        continue;
      }
      Path methodPath = method.getAnnotation(Path.class);
      String methodSegment = methodPath == null ? "" : trimSlashes(methodPath.value());
      String fullPath = joinServiceRestPath(typeSegment, methodSegment);

      RequiresPermissions methodPermissions = method.getAnnotation(RequiresPermissions.class);
      RequiresAuthentication methodAuth = method.getAnnotation(RequiresAuthentication.class);
      RequiresPermissions effectivePerm = methodPermissions != null ? methodPermissions : typePermissions;
      boolean authenticated = methodAuth != null || typeAuth != null;

      ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
      String description = describeOperation(apiOperation);
      String tag = firstTag(typeApi, apiOperation);

      List<ApiPermissionRequirement> requirements = toRequirements(effectivePerm);
      ApiEndpointPermission row = new ApiEndpointPermission(
          httpMethod.get(),
          fullPath,
          requirements,
          description,
          tag,
          authenticated);

      byKey.put(endpointKey(httpMethod.get(), fullPath), row);
    }
  }

  private static Optional<String> httpVerb(final Method method) {
    for (Annotation a : method.getAnnotations()) {
      Class<? extends Annotation> at = a.annotationType();
      if (at == GET.class) {
        return Optional.of("GET");
      }
      if (at == POST.class) {
        return Optional.of("POST");
      }
      if (at == PUT.class) {
        return Optional.of("PUT");
      }
      if (at == DELETE.class) {
        return Optional.of("DELETE");
      }
      if (at == PATCH.class) {
        return Optional.of("PATCH");
      }
      if (at == HEAD.class) {
        return Optional.of("HEAD");
      }
      if (at == OPTIONS.class) {
        return Optional.of("OPTIONS");
      }
      HttpMethod custom = at.getAnnotation(HttpMethod.class);
      if (custom != null && StringUtils.isNotBlank(custom.value())) {
        return Optional.of(custom.value().toUpperCase());
      }
    }
    return Optional.empty();
  }

  private static List<ApiPermissionRequirement> toRequirements(@Nullable final RequiresPermissions ann) {
    if (ann == null) {
      return List.of();
    }
    String logical = ann.logical() == Logical.OR ? "OR" : "AND";
    List<ApiPermissionRequirement> list = new ArrayList<>();
    for (String value : ann.value()) {
      list.add(new ApiPermissionRequirement(value, logical));
    }
    return list;
  }

  private static String describeOperation(@Nullable final ApiOperation apiOperation) {
    if (apiOperation == null) {
      return null;
    }
    if (StringUtils.isNotBlank(apiOperation.value())) {
      return apiOperation.value();
    }
    if (StringUtils.isNotBlank(apiOperation.notes())) {
      return apiOperation.notes();
    }
    return null;
  }

  @Nullable
  private static String firstTag(@Nullable final Api typeApi, @Nullable final ApiOperation op) {
    if (op != null && op.tags() != null && op.tags().length > 0 && StringUtils.isNotBlank(op.tags()[0])) {
      return op.tags()[0];
    }
    if (typeApi != null && typeApi.tags() != null && typeApi.tags().length > 0
        && StringUtils.isNotBlank(typeApi.tags()[0])) {
      return typeApi.tags()[0];
    }
    return null;
  }

  static String joinServiceRestPath(final String typeSegment, final String methodSegment) {
    checkNotNull(typeSegment);
    String joined;
    if (methodSegment.isEmpty()) {
      joined = typeSegment;
    }
    else {
      joined = typeSegment + "/" + methodSegment;
    }
    return normalizeFullPath(SERVICE_REST_PREFIX + "/" + joined);
  }

  static String normalizeFullPath(final String path) {
    String p = path.replaceAll("/+", "/");
    if (p.length() > 1 && p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }

  private static String trimSlashes(final String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    String t = raw;
    while (t.startsWith("/")) {
      t = t.substring(1);
    }
    while (t.endsWith("/")) {
      t = t.substring(0, t.length() - 1);
    }
    return t;
  }

  static String endpointKey(final String httpMethod, final String fullPath) {
    return httpMethod.toUpperCase() + "|" + normalizeFullPath(fullPath);
  }

  /**
   * Converts a JAX-RS path pattern into a Java regex for URL matching.
   * Static segments are quoted with {@link Pattern#quote} so that dots and other
   * regex metacharacters in paths like {@code /v1.1/} match literally.
   * Template forms:
   * - Simple: {name} -> [^/]+ (single segment, no slashes)
   * - Extended: {name: regex} -> .* (may span multiple segments)
   */
  static String buildTemplateRegex(final String pathPattern) {
    StringBuilder sb = new StringBuilder("^");
    for (String part : pathPattern.split("(?=\\{)|(?<=\\})")) {
      sb.append(part.startsWith("{") && part.endsWith("}")
          ? (part.contains(":") ? ".*" : "[^/]+")
          : Pattern.quote(part));
    }
    return sb.append("$").toString();
  }
}
