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

import org.sonatype.nexus.security.internal.rest.PermissionMappingService.ApiEndpointMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class PermissionMappingServiceTest
{
  private PermissionMappingService underTest;

  @BeforeEach
  void setup() {
    underTest = new PermissionMappingService();
  }

  @Test
  void testGetPermissionForEndpoint_exactMatch() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/repositories", "GET");

    assertThat(permission, is(equalTo("nexus:repository-admin:*:*:read")));
  }

  @Test
  void testGetPermissionForEndpoint_patternMatch() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/repositories/my-repo", "GET");

    assertThat(permission, is(equalTo("nexus:repository-admin:*:*:read")));
  }

  @Test
  void testGetPermissionForEndpoint_usersEndpoint() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/security/users", "GET");

    assertThat(permission, is(equalTo("nexus:users:read")));
  }

  @Test
  void testGetPermissionForEndpoint_usersEndpoint_post() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/security/users", "POST");

    assertThat(permission, is(equalTo("nexus:users:create")));
  }

  @Test
  void testGetPermissionForEndpoint_rolesEndpoint() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/security/roles", "GET");

    assertThat(permission, is(equalTo("nexus:roles:read")));
  }

  @Test
  void testGetPermissionForEndpoint_rolesEndpoint_withId() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/security/roles/nx-admin", "GET");

    assertThat(permission, is(equalTo("nexus:roles:read")));
  }

  @Test
  void testGetPermissionForEndpoint_privilegesEndpoint() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/security/privileges", "GET");

    assertThat(permission, is(equalTo("nexus:privileges:read")));
  }

  @Test
  void testGetPermissionForEndpoint_unknownEndpoint() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/unknown", "GET");

    assertThat(permission, is(nullValue()));
  }

  @Test
  void testGetPermissionForEndpoint_statusWritableRequiresNoAuth() {
    // /status/writable is a no-auth health probe — permission must be null so callers
    // that send credentials are not charged the Shiro realm (bcrypt) cost on each probe
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/status/writable", "GET");

    assertThat(permission, is(nullValue()));
  }

  @Test
  void testGetPermissionForEndpoint_normalizesTrailingSlash() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/repositories/", "GET");

    assertThat(permission, is(equalTo("nexus:repository-admin:*:*:read")));
  }

  @Test
  void testGetPermissionForEndpoint_normalizesQueryParams() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/security/users?userId=admin", "GET");

    assertThat(permission, is(equalTo("nexus:users:read")));
  }

  @Test
  void testGetPermissionForEndpoint_caseInsensitiveMethod() {
    String permission = underTest.getPermissionForEndpoint("/service/rest/v1/repositories", "get");

    assertThat(permission, is(equalTo("nexus:repository-admin:*:*:read")));
  }

  @Test
  void testGetEndpointsForPermission_exactMatch() {
    List<ApiEndpointMapping> endpoints = underTest.getEndpointsForPermission("nexus:users:read");

    assertThat(endpoints, hasSize(1));
    assertThat(endpoints.get(0).getEndpoint(), is(equalTo("/service/rest/v1/security/users")));
    assertThat(endpoints.get(0).getMethod(), is(equalTo("GET")));
  }

  @Test
  void testGetEndpointsForPermission_sameDomain() {
    // Use a permission pattern that matches the roles domain
    List<ApiEndpointMapping> endpoints = underTest.getEndpointsForPermission("nexus:roles:custom");

    // Should find roles endpoints since they all start with nexus:roles
    assertThat(endpoints, hasSize(greaterThan(0)));
  }

  @Test
  void testGetEndpointsForPermission_unknownPermission() {
    // Permission domain that doesn't exist in mappings
    List<ApiEndpointMapping> endpoints = underTest.getEndpointsForPermission("nexus:nonexistent:read");

    assertThat(endpoints, hasSize(0));
  }

  @Test
  void testGetAllEndpointMappings() {
    List<ApiEndpointMapping> mappings = underTest.getAllEndpointMappings();

    assertThat(mappings, is(notNullValue()));
    assertThat(mappings.size(), is(greaterThan(50)));
  }

  @Test
  void testApiEndpointMapping_matches() {
    ApiEndpointMapping mapping = new ApiEndpointMapping(
        "GET", "/service/rest/v1/test", "nexus:test:read", "Test endpoint");

    assertThat(mapping.matches("/service/rest/v1/test", "GET"), is(true));
    assertThat(mapping.matches("/service/rest/v1/test", "POST"), is(false));
    assertThat(mapping.matches("/service/rest/v1/other", "GET"), is(false));
  }

  @Test
  void testApiEndpointMapping_matchesPattern() {
    ApiEndpointMapping mapping = new ApiEndpointMapping(
        "GET", "/service/rest/v1/items/{id}", "nexus:items:read", "Get item by ID");

    assertThat(mapping.matchesPattern("/service/rest/v1/items/123", "GET"), is(true));
    assertThat(mapping.matchesPattern("/service/rest/v1/items/abc-def", "GET"), is(true));
    assertThat(mapping.matchesPattern("/service/rest/v1/items", "GET"), is(false));
    assertThat(mapping.matchesPattern("/service/rest/v1/items/123/sub", "GET"), is(false));
  }
}
