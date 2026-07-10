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

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Permission metadata for a single REST operation (HTTP method + path pattern).
 */
public class ApiEndpointPermission
{
  @Schema(description = "HTTP method", requiredMode = Schema.RequiredMode.REQUIRED, example = "DELETE")
  private String httpMethod;

  @Schema(description = "Path pattern with JAX-RS template segments", requiredMode = Schema.RequiredMode.REQUIRED)
  private String pathPattern;

  @Schema(description = "Required permissions and combination logic", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<ApiPermissionRequirement> permissions;

  @Nullable
  @Schema(description = "Short description from Swagger annotations, if any")
  private String description;

  @Nullable
  @Schema(description = "Swagger tag / API grouping")
  private String tag;

  @Schema(description = "Whether the endpoint requires an authenticated subject",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean authenticated;

  public ApiEndpointPermission() {
    // Jackson
  }

  public ApiEndpointPermission(
      final String httpMethod,
      final String pathPattern,
      final List<ApiPermissionRequirement> permissions,
      @Nullable final String description,
      @Nullable final String tag,
      final boolean authenticated)
  {
    this.httpMethod = httpMethod;
    this.pathPattern = pathPattern;
    this.permissions = permissions;
    this.description = description;
    this.tag = tag;
    this.authenticated = authenticated;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(final String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getPathPattern() {
    return pathPattern;
  }

  public void setPathPattern(final String pathPattern) {
    this.pathPattern = pathPattern;
  }

  public List<ApiPermissionRequirement> getPermissions() {
    return permissions;
  }

  public void setPermissions(final List<ApiPermissionRequirement> permissions) {
    this.permissions = permissions;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public void setDescription(@Nullable final String description) {
    this.description = description;
  }

  @Nullable
  public String getTag() {
    return tag;
  }

  public void setTag(@Nullable final String tag) {
    this.tag = tag;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public void setAuthenticated(final boolean authenticated) {
    this.authenticated = authenticated;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEndpointPermission that = (ApiEndpointPermission) o;
    return authenticated == that.authenticated
        && Objects.equals(httpMethod, that.httpMethod)
        && Objects.equals(pathPattern, that.pathPattern)
        && Objects.equals(permissions, that.permissions)
        && Objects.equals(description, that.description)
        && Objects.equals(tag, that.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(httpMethod, pathPattern, permissions, description, tag, authenticated);
  }
}
