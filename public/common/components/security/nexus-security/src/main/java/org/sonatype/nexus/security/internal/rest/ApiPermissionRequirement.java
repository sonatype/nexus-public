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

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One required permission entry for an API endpoint (matches UI permission map JSON).
 */
public class ApiPermissionRequirement
{
  @Schema(description = "Permission string as declared on the resource", requiredMode = Schema.RequiredMode.REQUIRED)
  private String permission;

  @Schema(description = "How this permission combines with siblings (AND or OR)",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String logical;

  public ApiPermissionRequirement() {
    // Jackson
  }

  public ApiPermissionRequirement(final String permission, final String logical) {
    this.permission = permission;
    this.logical = logical;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(final String permission) {
    this.permission = permission;
  }

  public String getLogical() {
    return logical;
  }

  public void setLogical(final String logical) {
    this.logical = logical;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiPermissionRequirement that = (ApiPermissionRequirement) o;
    return Objects.equals(permission, that.permission) && Objects.equals(logical, that.logical);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permission, logical);
  }
}
