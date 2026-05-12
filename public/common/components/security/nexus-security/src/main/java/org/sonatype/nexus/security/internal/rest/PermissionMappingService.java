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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import jakarta.inject.Singleton;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service for mapping Nexus permissions to REST API endpoints.
 * This provides a bidirectional mapping between permissions and endpoints.
 *
 * NEXUS-51956: {@link EndpointPermissionRegistry} now scans {@code @RequiresPermissions} annotations
 * at runtime and is the authoritative source. This class is retained only to supply manual mapping
 * rows that the registry merges in via {@code applyManualMappings}.
 */
@Component
@Singleton
public class PermissionMappingService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final List<ApiEndpointMapping> endpointMappings;

  private final Map<String, List<ApiEndpointMapping>> permissionToEndpointsMap;

  public PermissionMappingService() {
    this.endpointMappings = initializeEndpointMappings();
    this.permissionToEndpointsMap = buildPermissionToEndpointsMap();
  }

  /**
   * Gets the required permission for an API endpoint.
   *
   * @param endpoint the API endpoint path
   * @param method the HTTP method
   * @return the required permission, or null if not found
   */
  @Nullable
  public String getPermissionForEndpoint(final String endpoint, final String method) {
    checkNotNull(endpoint);
    checkNotNull(method);

    String normalizedEndpoint = normalizeEndpoint(endpoint);
    String normalizedMethod = method.toUpperCase();

    // Try exact match first
    Optional<ApiEndpointMapping> exactMatch = endpointMappings.stream()
        .filter(m -> m.matches(normalizedEndpoint, normalizedMethod))
        .findFirst();

    if (exactMatch.isPresent()) {
      return exactMatch.get().getPermission();
    }

    // Try pattern match
    return endpointMappings.stream()
        .filter(m -> m.matchesPattern(normalizedEndpoint, normalizedMethod))
        .findFirst()
        .map(ApiEndpointMapping::getPermission)
        .orElse(null);
  }

  /**
   * Gets related API endpoints for a permission.
   *
   * @param permission the permission string
   * @return list of related endpoint mappings
   */
  public List<ApiEndpointMapping> getEndpointsForPermission(final String permission) {
    checkNotNull(permission);

    // First check for exact match
    List<ApiEndpointMapping> exactMatches = permissionToEndpointsMap.get(permission);
    if (exactMatches != null && !exactMatches.isEmpty()) {
      return exactMatches;
    }

    // Try to find similar permissions (same domain)
    String domain = extractDomain(permission);
    if (domain != null) {
      return endpointMappings.stream()
          .filter(m -> m.getPermission() != null && m.getPermission().startsWith("nexus:" + domain))
          .collect(Collectors.toList());
    }

    return List.of();
  }

  /**
   * Gets all endpoint mappings.
   */
  public List<ApiEndpointMapping> getAllEndpointMappings() {
    return List.copyOf(endpointMappings);
  }

  private String normalizeEndpoint(final String endpoint) {
    // Remove trailing slash and normalize to lowercase
    String normalized = endpoint.trim();
    if (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    // Remove query parameters
    int queryIndex = normalized.indexOf('?');
    if (queryIndex > 0) {
      normalized = normalized.substring(0, queryIndex);
    }
    return normalized;
  }

  @Nullable
  private String extractDomain(final String permission) {
    // Permission format: nexus:domain:action
    String[] parts = permission.split(":");
    if (parts.length >= 2) {
      return parts[1];
    }
    return null;
  }

  private Map<String, List<ApiEndpointMapping>> buildPermissionToEndpointsMap() {
    Map<String, List<ApiEndpointMapping>> map = new HashMap<>();
    for (ApiEndpointMapping mapping : endpointMappings) {
      map.computeIfAbsent(mapping.getPermission(), k -> new ArrayList<>()).add(mapping);
    }
    return map;
  }

  private List<ApiEndpointMapping> initializeEndpointMappings() {
    List<ApiEndpointMapping> mappings = new ArrayList<>();

    // Repository endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/repositories",
        "nexus:repository-admin:*:*:read", "List all repositories"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/repositories/{name}",
        "nexus:repository-admin:*:*:read", "Get repository by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/repositories/{format}/{type}",
        "nexus:repository-admin:*:*:add", "Create a repository"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/repositories/{format}/{type}/{name}",
        "nexus:repository-admin:*:*:edit", "Update a repository"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/repositories/{name}",
        "nexus:repository-admin:*:*:delete", "Delete a repository"));

    // Components endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/components",
        "nexus:repository-view:*:*:browse", "List components"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/components/{id}",
        "nexus:repository-view:*:*:browse", "Get component by ID"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/components",
        "nexus:repository-view:*:*:add", "Upload component"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/components/{id}",
        "nexus:repository-view:*:*:delete", "Delete component"));

    // Assets endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/assets",
        "nexus:repository-view:*:*:browse", "List assets"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/assets/{id}",
        "nexus:repository-view:*:*:browse", "Get asset by ID"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/assets/{id}",
        "nexus:repository-view:*:*:delete", "Delete asset"));

    // Search endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/search",
        "nexus:repository-view:*:*:browse", "Search components"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/search/assets",
        "nexus:repository-view:*:*:browse", "Search assets"));

    // Security - Users endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/users",
        "nexus:users:read", "List users"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/security/users",
        "nexus:users:create", "Create user"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/users/{userId}",
        "nexus:users:update", "Update user"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/security/users/{userId}",
        "nexus:users:delete", "Delete user"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/users/{userId}/change-password",
        "nexus:users:update", "Change user password"));

    // Security - Roles endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/roles",
        "nexus:roles:read", "List roles"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/roles/{id}",
        "nexus:roles:read", "Get role by ID"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/security/roles",
        "nexus:roles:create", "Create role"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/roles/{id}",
        "nexus:roles:update", "Update role"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/security/roles/{id}",
        "nexus:roles:delete", "Delete role"));

    // Security - Privileges endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/privileges",
        "nexus:privileges:read", "List privileges"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/privileges/{privilegeName}",
        "nexus:privileges:read", "Get privilege by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/security/privileges/{type}",
        "nexus:privileges:create", "Create privilege"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/privileges/{type}/{privilegeName}",
        "nexus:privileges:update", "Update privilege"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/security/privileges/{privilegeName}",
        "nexus:privileges:delete", "Delete privilege"));

    // Security - Anonymous access
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/anonymous",
        "nexus:settings:read", "Get anonymous access settings"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/anonymous",
        "nexus:settings:update", "Update anonymous access settings"));

    // Security - Realms
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/realms/active",
        "nexus:settings:read", "Get active security realms"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/realms/active",
        "nexus:settings:update", "Set active security realms"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/realms/available",
        "nexus:settings:read", "List available security realms"));

    // Blob stores endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/blobstores",
        "nexus:blobstores:read", "List blob stores"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/blobstores/{name}",
        "nexus:blobstores:read", "Get blob store by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/blobstores/{type}",
        "nexus:blobstores:create", "Create blob store"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/blobstores/{type}/{name}",
        "nexus:blobstores:update", "Update blob store"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/blobstores/{name}",
        "nexus:blobstores:delete", "Delete blob store"));

    // Tasks endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/tasks",
        "nexus:tasks:read", "List scheduled tasks"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/tasks/{id}",
        "nexus:tasks:read", "Get task by ID"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/tasks/{id}/run",
        "nexus:tasks:start", "Run a task"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/tasks/{id}/stop",
        "nexus:tasks:stop", "Stop a task"));

    // Status endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/status",
        null, "Get system status (no auth required)"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/status/check",
        null, "Health check (no auth required)"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/status/writable",
        "nexus:*", "Check if system is writable"));

    // Email configuration
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/email",
        "nexus:settings:read", "Get email configuration"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/email",
        "nexus:settings:update", "Update email configuration"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/email/verify",
        "nexus:settings:update", "Verify email configuration"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/email",
        "nexus:settings:update", "Delete email configuration"));

    // Licensing endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/system/license",
        "nexus:licensing:read", "Get license details"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/system/license",
        "nexus:licensing:update", "Install license"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/system/license",
        "nexus:licensing:delete", "Remove license"));

    // Support endpoints
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/support/supportzip",
        "nexus:atlas:create", "Create support ZIP"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/support/supportzippath",
        "nexus:atlas:create", "Create support ZIP at path"));

    // Read-only mode
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/read-only",
        "nexus:*", "Get read-only state"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/read-only/freeze",
        "nexus:*", "Enable read-only mode"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/read-only/release",
        "nexus:*", "Disable read-only mode"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/read-only/force-release",
        "nexus:*", "Force disable read-only mode"));

    // Content selectors
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/content-selectors",
        "nexus:selectors:read", "List content selectors"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/content-selectors/{name}",
        "nexus:selectors:read", "Get content selector by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/security/content-selectors",
        "nexus:selectors:create", "Create content selector"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/content-selectors/{name}",
        "nexus:selectors:update", "Update content selector"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/security/content-selectors/{name}",
        "nexus:selectors:delete", "Delete content selector"));

    // LDAP endpoints
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/ldap",
        "nexus:ldap:read", "List LDAP servers"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/security/ldap/{name}",
        "nexus:ldap:read", "Get LDAP server by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/security/ldap",
        "nexus:ldap:create", "Create LDAP server"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/security/ldap/{name}",
        "nexus:ldap:update", "Update LDAP server"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/security/ldap/{name}",
        "nexus:ldap:delete", "Delete LDAP server"));

    // Routing rules
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/routing-rules",
        "nexus:routing-rules:read", "List routing rules"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/routing-rules/{name}",
        "nexus:routing-rules:read", "Get routing rule by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/routing-rules",
        "nexus:routing-rules:create", "Create routing rule"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/routing-rules/{name}",
        "nexus:routing-rules:update", "Update routing rule"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/routing-rules/{name}",
        "nexus:routing-rules:delete", "Delete routing rule"));

    // Script endpoints (deprecated but still available)
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/script",
        "nexus:script:*", "List scripts"));
    mappings.add(new ApiEndpointMapping("GET", "/service/rest/v1/script/{name}",
        "nexus:script:*", "Get script by name"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/script",
        "nexus:script:*", "Create script"));
    mappings.add(new ApiEndpointMapping("PUT", "/service/rest/v1/script/{name}",
        "nexus:script:*", "Update script"));
    mappings.add(new ApiEndpointMapping("DELETE", "/service/rest/v1/script/{name}",
        "nexus:script:*", "Delete script"));
    mappings.add(new ApiEndpointMapping("POST", "/service/rest/v1/script/{name}/run",
        "nexus:script:*", "Run script"));

    return mappings;
  }

  /**
   * Represents a mapping between an API endpoint and its required permission.
   */
  public static class ApiEndpointMapping
  {
    private final String method;

    private final String endpoint;

    private final String permission;

    private final String description;

    private final Pattern endpointPattern;

    public ApiEndpointMapping(
        final String method,
        final String endpoint,
        @Nullable final String permission,
        final String description)
    {
      this.method = checkNotNull(method);
      this.endpoint = checkNotNull(endpoint);
      this.permission = permission;
      this.description = checkNotNull(description);

      String patternStr = endpoint.replaceAll("\\{[^}]+\\}", "[^/]+");
      this.endpointPattern = Pattern.compile("^" + patternStr + "$");
    }

    public String getMethod() {
      return method;
    }

    public String getEndpoint() {
      return endpoint;
    }

    @Nullable
    public String getPermission() {
      return permission;
    }

    public String getDescription() {
      return description;
    }

    public boolean matches(final String endpoint, final String method) {
      return this.method.equalsIgnoreCase(method) && this.endpoint.equals(endpoint);
    }

    public boolean matchesPattern(final String endpoint, final String method) {
      return this.method.equalsIgnoreCase(method) && endpointPattern.matcher(endpoint).matches();
    }
  }
}
