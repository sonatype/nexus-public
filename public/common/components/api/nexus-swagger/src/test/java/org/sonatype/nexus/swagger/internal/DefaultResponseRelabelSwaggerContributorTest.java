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
package org.sonatype.nexus.swagger.internal;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link DefaultResponseRelabelSwaggerContributor}.
 */
class DefaultResponseRelabelSwaggerContributorTest
{
  private final DefaultResponseRelabelSwaggerContributor contributor = new DefaultResponseRelabelSwaggerContributor();

  private ApiResponse jsonResponse(final String description) {
    return new ApiResponse().description(description)
        .content(new Content().addMediaType("application/json", new MediaType().schema(new Schema<>())));
  }

  private OpenAPI apiWith(final ApiResponses responses) {
    Operation op = new Operation().responses(responses);
    PathItem item = new PathItem();
    item.operation(PathItem.HttpMethod.GET, op);
    return new OpenAPI().paths(new Paths().addPathItem("/v1/thing", item));
  }

  private ApiResponses responsesOf(final OpenAPI api) {
    return api.getPaths().get("/v1/thing").getGet().getResponses();
  }

  @Test
  void relabelsDefaultOnlyWithContentTo200() {
    OpenAPI api = apiWith(new ApiResponses().addApiResponse("default", jsonResponse("default response")));
    contributor.contribute(api);
    ApiResponses r = responsesOf(api);
    assertThat(r.get("default"), is(nullValue()));
    assertThat(r.get("200"), is(notNullValue()));
    assertThat(r.get("200").getContent().get("application/json"), is(notNullValue()));
    assertThat(r.get("200").getDescription(), is("successful operation"));
  }

  @Test
  void keepsMeaningfulDescription() {
    OpenAPI api = apiWith(new ApiResponses().addApiResponse("default", jsonResponse("List of plans found")));
    contributor.contribute(api);
    assertThat(responsesOf(api).get("200").getDescription(), is("List of plans found"));
  }

  @Test
  void ignoresDefaultWithoutContent() {
    OpenAPI api = apiWith(new ApiResponses().addApiResponse("default", new ApiResponse().description("x")));
    contributor.contribute(api);
    ApiResponses r = responsesOf(api);
    assertThat(r.get("default"), is(notNullValue()));
    assertThat(r.get("200"), is(nullValue()));
  }

  @Test
  void ignoresWhenOtherStatusCodesPresent() {
    ApiResponses in = new ApiResponses()
        .addApiResponse("default", jsonResponse("x"))
        .addApiResponse("404", new ApiResponse().description("not found"));
    OpenAPI api = apiWith(in);
    contributor.contribute(api);
    assertThat(responsesOf(api).get("default"), is(notNullValue()));
    assertThat(responsesOf(api).get("200"), is(nullValue()));
  }

  @Test
  void isIdempotent() {
    OpenAPI api = apiWith(new ApiResponses().addApiResponse("default", jsonResponse("default response")));
    contributor.contribute(api);
    contributor.contribute(api);
    assertThat(responsesOf(api).get("200"), is(notNullValue()));
    assertThat(responsesOf(api).get("default"), is(nullValue()));
  }
}
