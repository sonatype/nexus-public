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

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request object for checking API access.
 */
public class ApiAccessCheckXo
{
  private static final String ID_PATTERN = "^[a-zA-Z0-9._-]{1,200}$";

  private static final String ID_PATTERN_MESSAGE =
      "Invalid format: must be 1-200 alphanumeric characters, dots, underscores, or hyphens";

  @Nullable
  @Pattern(regexp = ID_PATTERN, message = ID_PATTERN_MESSAGE)
  @Schema(description = "The user ID to check access for. If omitted, checks access for the current user.",
      example = "john.doe")
  private String userId;

  @Nullable
  @Pattern(regexp = ID_PATTERN, message = ID_PATTERN_MESSAGE)
  @Schema(description = "The role ID to check access for. Mutually exclusive with userId.",
      example = "nx-admin")
  private String roleId;

  @NotEmpty
  @Schema(description = "The API endpoint to check access for.",
      required = true, example = "/service/rest/v1/repositories")
  private String endpoint;

  @NotEmpty
  @Schema(description = "The HTTP method for the endpoint.",
      required = true, example = "GET")
  private String method;

  public ApiAccessCheckXo() {
    // Default constructor for Jackson
  }

  public ApiAccessCheckXo(
      @Nullable final String userId,
      @Nullable final String roleId,
      final String endpoint,
      final String method)
  {
    this.userId = userId;
    this.roleId = roleId;
    this.endpoint = endpoint;
    this.method = method;
  }

  @Nullable
  public String getUserId() {
    return userId;
  }

  public void setUserId(@Nullable final String userId) {
    this.userId = userId;
  }

  @Nullable
  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(@Nullable final String roleId) {
    this.roleId = roleId;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final String endpoint) {
    this.endpoint = endpoint;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(final String method) {
    this.method = method;
  }
}
