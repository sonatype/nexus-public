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
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class ApiPermissionsResourceTest
{
  @Mock
  private EndpointPermissionRegistry registry;

  private ApiPermissionsResource underTest;

  private static final Instant TEST_INSTANT = Instant.parse("2026-01-15T10:30:00Z");

  @BeforeEach
  void setup() {
    underTest = new ApiPermissionsResource(registry);
  }

  @Test
  void list_returnsEndpointsWhenRegistryReady() {
    ApiEndpointPermission endpoint = new ApiEndpointPermission(
        "GET",
        "/service/rest/v1/security/users",
        List.of(new ApiPermissionRequirement("nexus:users:read", "AND")),
        "List users",
        "Security",
        true);

    List<ApiEndpointPermission> endpoints = List.of(endpoint);

    when(registry.isReady()).thenReturn(true);
    when(registry.getEndpoints()).thenReturn(endpoints);
    when(registry.getGeneratedAt()).thenReturn(TEST_INSTANT);
    when(registry.getUnmappedSwaggerOperations()).thenReturn(0);

    Response response = underTest.list(null, null, null, null);

    assertThat(response.getStatus(), is(OK.getStatusCode()));

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body, is(notNullValue()));
    assertThat(body.getEndpoints(), hasSize(1));
    assertThat(body.getEndpoints().get(0).getHttpMethod(), is(equalTo("GET")));
    assertThat(body.getEndpoints().get(0).getPathPattern(), is(equalTo("/service/rest/v1/security/users")));
    assertThat(body.getTotalEndpoints(), is(1));
    assertThat(body.getUnmappedEndpoints(), is(0));
    assertThat(body.getGeneratedAt(), is(equalTo("2026-01-15T10:30:00Z")));
    assertThat(body.getError(), is(nullValue()));
  }

  @Test
  void list_returnsEmptyWhenRegistryHasNoEndpoints() {
    when(registry.isReady()).thenReturn(true);
    when(registry.getEndpoints()).thenReturn(List.of());
    when(registry.getGeneratedAt()).thenReturn(TEST_INSTANT);
    when(registry.getUnmappedSwaggerOperations()).thenReturn(0);

    Response response = underTest.list(null, null, null, null);

    assertThat(response.getStatus(), is(OK.getStatusCode()));

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body.getEndpoints(), is(empty()));
    assertThat(body.getTotalEndpoints(), is(0));
    assertThat(body.getError(), is(nullValue()));
  }

  @Test
  void list_returns500WhenRegistryNotReady() {
    when(registry.isReady()).thenReturn(false);

    ApiPermissionsResponse snapshot = new ApiPermissionsResponse(
        List.of(), "1970-01-01T00:00:00Z", 0, 0, "Permission registry unavailable");
    when(registry.snapshot("Permission registry unavailable")).thenReturn(snapshot);

    Response response = underTest.list(null, null, null, null);

    assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR.getStatusCode()));

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body.getError(), is(equalTo("Permission registry unavailable")));
  }

  @Test
  void list_filtersEndpointsByMethod() {
    ApiEndpointPermission getEndpoint = new ApiEndpointPermission(
        "GET", "/service/rest/v1/users",
        List.of(new ApiPermissionRequirement("nexus:users:read", "AND")),
        "List users", null, true);

    ApiEndpointPermission deleteEndpoint = new ApiEndpointPermission(
        "DELETE", "/service/rest/v1/users/{id}",
        List.of(new ApiPermissionRequirement("nexus:users:delete", "AND")),
        "Delete user", null, true);

    List<ApiEndpointPermission> endpoints = List.of(getEndpoint, deleteEndpoint);

    when(registry.isReady()).thenReturn(true);
    when(registry.getEndpoints()).thenReturn(endpoints);
    when(registry.getGeneratedAt()).thenReturn(TEST_INSTANT);
    when(registry.getUnmappedSwaggerOperations()).thenReturn(0);

    Response response = underTest.list("GET", null, null, null);

    assertThat(response.getStatus(), is(OK.getStatusCode()));

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body.getEndpoints(), hasSize(1));
    assertThat(body.getEndpoints().get(0).getHttpMethod(), is(equalTo("GET")));
    assertThat(body.getTotalEndpoints(), is(2));
  }

  @Test
  void list_filtersEndpointsByPathSubstring() {
    ApiEndpointPermission usersEndpoint = new ApiEndpointPermission(
        "GET", "/service/rest/v1/security/users",
        List.of(new ApiPermissionRequirement("nexus:users:read", "AND")),
        "List users", null, true);

    ApiEndpointPermission rolesEndpoint = new ApiEndpointPermission(
        "GET", "/service/rest/v1/security/roles",
        List.of(new ApiPermissionRequirement("nexus:roles:read", "AND")),
        "List roles", null, true);

    List<ApiEndpointPermission> endpoints = List.of(usersEndpoint, rolesEndpoint);

    when(registry.isReady()).thenReturn(true);
    when(registry.getEndpoints()).thenReturn(endpoints);
    when(registry.getGeneratedAt()).thenReturn(TEST_INSTANT);
    when(registry.getUnmappedSwaggerOperations()).thenReturn(0);

    Response response = underTest.list(null, "users", null, null);

    assertThat(response.getStatus(), is(OK.getStatusCode()));

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body.getEndpoints(), hasSize(1));
    assertThat(body.getEndpoints().get(0).getPathPattern(), is(equalTo("/service/rest/v1/security/users")));
  }

  @Test
  void list_includesUnmappedEndpointCount() {
    when(registry.isReady()).thenReturn(true);
    when(registry.getEndpoints()).thenReturn(List.of());
    when(registry.getGeneratedAt()).thenReturn(TEST_INSTANT);
    when(registry.getUnmappedSwaggerOperations()).thenReturn(5);

    Response response = underTest.list(null, null, null, null);

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body.getUnmappedEndpoints(), is(5));
  }

  @Test
  void list_multipleEndpointsReturnedUnfiltered() {
    ApiEndpointPermission ep1 = new ApiEndpointPermission(
        "GET", "/service/rest/v1/a", List.of(), "A", null, true);
    ApiEndpointPermission ep2 = new ApiEndpointPermission(
        "POST", "/service/rest/v1/b", List.of(), "B", null, true);
    ApiEndpointPermission ep3 = new ApiEndpointPermission(
        "DELETE", "/service/rest/v1/c", List.of(), "C", null, false);

    List<ApiEndpointPermission> endpoints = List.of(ep1, ep2, ep3);

    when(registry.isReady()).thenReturn(true);
    when(registry.getEndpoints()).thenReturn(endpoints);
    when(registry.getGeneratedAt()).thenReturn(TEST_INSTANT);
    when(registry.getUnmappedSwaggerOperations()).thenReturn(0);

    Response response = underTest.list(null, null, null, null);

    assertThat(response.getStatus(), is(OK.getStatusCode()));

    ApiPermissionsResponse body = (ApiPermissionsResponse) response.getEntity();
    assertThat(body.getEndpoints(), hasSize(3));
    assertThat(body.getTotalEndpoints(), is(3));
  }
}
