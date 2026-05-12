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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ApiEndpointPermissionSerializationTest
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void serializesSampleEndpointMatchingSpecShape() throws Exception {
    ApiEndpointPermission endpoint = new ApiEndpointPermission(
        "DELETE",
        "/v1/repositories/{repositoryName}",
        List.of(new ApiPermissionRequirement("nexus:repositories:delete", "AND")),
        "Delete repository",
        "Repository Management",
        true);

    ApiPermissionsResponse response = new ApiPermissionsResponse(
        List.of(endpoint),
        "2026-03-23T10:00:00Z",
        247,
        3,
        null);

    JsonNode root = mapper.readTree(mapper.writeValueAsString(response));

    assertThat(root.get("totalEndpoints").asInt(), is(247));
    assertThat(root.get("unmappedEndpoints").asInt(), is(3));
    assertThat(root.get("generatedAt").asText(), is("2026-03-23T10:00:00Z"));

    JsonNode ep = root.get("endpoints").get(0);
    assertThat(ep.get("httpMethod").asText(), is("DELETE"));
    assertThat(ep.get("pathPattern").asText(), is("/v1/repositories/{repositoryName}"));
    assertThat(ep.get("description").asText(), is("Delete repository"));
    assertThat(ep.get("tag").asText(), is("Repository Management"));
    assertThat(ep.get("authenticated").asBoolean(), is(true));

    JsonNode perm = ep.get("permissions").get(0);
    assertThat(perm.get("permission").asText(), is("nexus:repositories:delete"));
    assertThat(perm.get("logical").asText(), is("AND"));
  }

  @Test
  void roundTripPreservesFields() throws Exception {
    ApiPermissionsResponse original = new ApiPermissionsResponse(
        List.of(new ApiEndpointPermission(
            "GET",
            "/v1/status",
            List.of(),
            null,
            null,
            false)),
        "2026-01-01T00:00:00Z",
        1,
        0,
        null);

    String json = mapper.writeValueAsString(original);
    ApiPermissionsResponse copy = mapper.readValue(json, ApiPermissionsResponse.class);

    assertThat(copy.getTotalEndpoints(), is(1));
    assertThat(copy.getEndpoints().get(0).getHttpMethod(), is("GET"));
    assertThat(copy.getEndpoints().get(0).isAuthenticated(), is(false));
  }
}
