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

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response object for API access check results.
 */
public class ApiAccessResultXo
{
  @Schema(description = "Whether the user/role has access to the endpoint", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean hasAccess;

  @Nullable
  @Schema(description = "The permission required to access the endpoint")
  private String requiredPermission;

  @Schema(description = "The permission chains showing how access is granted (user -> role -> privilege -> permission)")
  private List<PermissionChainXo> chains;

  @Schema(description = "Related API endpoints that use similar permissions")
  private List<RelatedEndpointXo> relatedEndpoints;

  public ApiAccessResultXo() {
    // Default constructor for Jackson
  }

  public ApiAccessResultXo(
      final boolean hasAccess,
      @Nullable final String requiredPermission,
      final List<PermissionChainXo> chains,
      final List<RelatedEndpointXo> relatedEndpoints)
  {
    this.hasAccess = hasAccess;
    this.requiredPermission = requiredPermission;
    this.chains = chains;
    this.relatedEndpoints = relatedEndpoints;
  }

  public boolean isHasAccess() {
    return hasAccess;
  }

  public void setHasAccess(final boolean hasAccess) {
    this.hasAccess = hasAccess;
  }

  @Nullable
  public String getRequiredPermission() {
    return requiredPermission;
  }

  public void setRequiredPermission(@Nullable final String requiredPermission) {
    this.requiredPermission = requiredPermission;
  }

  public List<PermissionChainXo> getChains() {
    return chains;
  }

  public void setChains(final List<PermissionChainXo> chains) {
    this.chains = chains;
  }

  public List<RelatedEndpointXo> getRelatedEndpoints() {
    return relatedEndpoints;
  }

  public void setRelatedEndpoints(final List<RelatedEndpointXo> relatedEndpoints) {
    this.relatedEndpoints = relatedEndpoints;
  }

  /**
   * Represents a chain showing how permission is granted from user to permission.
   */
  public static class PermissionChainXo
  {
    @Nullable
    @Schema(description = "The user in the permission chain")
    private EntityRefXo user;

    @Nullable
    @Schema(description = "The role in the permission chain")
    private EntityRefXo role;

    @Nullable
    @Schema(description = "The privilege in the permission chain")
    private EntityRefXo privilege;

    @Nullable
    @Schema(description = "The permission string")
    private String permission;

    public PermissionChainXo() {
      // Default constructor for Jackson
    }

    public PermissionChainXo(
        @Nullable final EntityRefXo user,
        @Nullable final EntityRefXo role,
        @Nullable final EntityRefXo privilege,
        @Nullable final String permission)
    {
      this.user = user;
      this.role = role;
      this.privilege = privilege;
      this.permission = permission;
    }

    @Nullable
    public EntityRefXo getUser() {
      return user;
    }

    public void setUser(@Nullable final EntityRefXo user) {
      this.user = user;
    }

    @Nullable
    public EntityRefXo getRole() {
      return role;
    }

    public void setRole(@Nullable final EntityRefXo role) {
      this.role = role;
    }

    @Nullable
    public EntityRefXo getPrivilege() {
      return privilege;
    }

    public void setPrivilege(@Nullable final EntityRefXo privilege) {
      this.privilege = privilege;
    }

    @Nullable
    public String getPermission() {
      return permission;
    }

    public void setPermission(@Nullable final String permission) {
      this.permission = permission;
    }
  }

  /**
   * Reference to a security entity (user, role, or privilege).
   */
  public static class EntityRefXo
  {
    @Schema(description = "The entity ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Nullable
    @Schema(description = "The entity display name")
    private String name;

    public EntityRefXo() {
      // Default constructor for Jackson
    }

    public EntityRefXo(final String id, @Nullable final String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public void setId(final String id) {
      this.id = id;
    }

    @Nullable
    public String getName() {
      return name;
    }

    public void setName(@Nullable final String name) {
      this.name = name;
    }
  }

  /**
   * Represents a related API endpoint.
   */
  public static class RelatedEndpointXo
  {
    @Schema(description = "The HTTP method", requiredMode = Schema.RequiredMode.REQUIRED)
    private String method;

    @Schema(description = "The API endpoint path", requiredMode = Schema.RequiredMode.REQUIRED)
    private String endpoint;

    @Nullable
    @Schema(description = "Description of the endpoint")
    private String description;

    @Nullable
    @Schema(description = "The permission required for this endpoint")
    private String permission;

    public RelatedEndpointXo() {
      // Default constructor for Jackson
    }

    public RelatedEndpointXo(
        final String method,
        final String endpoint,
        @Nullable final String description,
        @Nullable final String permission)
    {
      this.method = method;
      this.endpoint = endpoint;
      this.description = description;
      this.permission = permission;
    }

    public String getMethod() {
      return method;
    }

    public void setMethod(final String method) {
      this.method = method;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(final String endpoint) {
      this.endpoint = endpoint;
    }

    @Nullable
    public String getDescription() {
      return description;
    }

    public void setDescription(@Nullable final String description) {
      this.description = description;
    }

    @Nullable
    public String getPermission() {
      return permission;
    }

    public void setPermission(@Nullable final String permission) {
      this.permission = permission;
    }
  }
}
