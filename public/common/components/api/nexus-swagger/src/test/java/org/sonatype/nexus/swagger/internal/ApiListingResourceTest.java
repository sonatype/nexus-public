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

import java.util.List;

import org.sonatype.nexus.common.app.BaseUrlHolder;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link ApiListingResource}.
 */
@ExtendWith(MockitoExtension.class)
class ApiListingResourceTest
{
  @Mock
  private SwaggerModel swaggerModel;

  @Mock
  private ResponseBuilder responseBuilder;

  @Captor
  ArgumentCaptor<String> bodyCaptor;

  @Captor
  ArgumentCaptor<String> typeCaptor;

  private MockedStatic<Response> staticResponse;

  private ApiListingResource underTest;

  @BeforeEach
  void setUp() {
    staticResponse = mockStatic(Response.class, Answers.RETURNS_MOCKS);

    lenient().when(swaggerModel.getOpenApi())
        .thenReturn(new OpenAPI().info(new Info()
            .title("Test API")
            .version("1.0.0")));
    underTest = new ApiListingResource(swaggerModel, 10);
  }

  @AfterEach
  void tearDown() {
    BaseUrlHolder.unset();
    if (staticResponse != null) {
      staticResponse.close();
    }
  }

  @Test
  void getOpenApi_returnsJsonFormat() throws Exception {
    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    assertThat(typeCaptor.getValue(), containsString("application/json"));
    assertThat(bodyCaptor.getValue(), containsString("\"title\" : \"Test API\""));
  }

  @Test
  void getOpenApi_returnsYamlFormat() throws Exception {
    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("yaml");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    assertThat(typeCaptor.getValue(), containsString("application/yaml"));
    assertThat(bodyCaptor.getValue(), containsString("title: Test API"));
  }

  @Test
  void getOpenApi_handlesYamlCaseInsensitive() throws Exception {
    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("YAML");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    assertThat(typeCaptor.getValue(), containsString("application/yaml"));
    assertThat(bodyCaptor.getValue(), containsString("title: Test API"));
  }

  @Test
  void getOpenApi_handlesJsonCaseInsensitive() throws Exception {
    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("JSON");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    assertThat(typeCaptor.getValue(), containsString("application/json"));
  }

  @Test
  void getOpenApi_includesServerUrlWithServiceRestPath() throws Exception {
    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("service/rest"));
  }

  @Test
  void getOpenApi_handlesBaseUrlWithTrailingSlash() throws Exception {
    BaseUrlHolder.set("http://localhost:8081/", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("service/rest"));
    // Should not have double slashes
    assertThat(body, not(containsString("//service/rest")));
  }

  @Test
  void getOpenApi_handlesBaseUrlWithPath() throws Exception {
    BaseUrlHolder.set("http://localhost:8081/nexus", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("/nexus/service/rest"));
  }

  @Test
  void getOpenApi_handlesBaseUrlWithPathAndTrailingSlash() throws Exception {
    BaseUrlHolder.set("http://localhost:8081/nexus/", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("/nexus/service/rest"));
    // Should not have double slashes
    assertThat(body, not(containsString("//service/rest")));
  }

  @Test
  void getOpenApi_handlesMissingBaseUrl() throws Exception {
    // Given - BaseUrlHolder is not set
    underTest.getOpenApi("json");

    // Then - should default to "/" as base path
    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("\"/service/rest\""));
  }

  @Test
  void getOpenApi_returnsDifferentResultsForDifferentFormats() throws Exception {
    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("json");
    underTest.getOpenApi("yaml");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()), times(2));
    List<String> bodies = bodyCaptor.getAllValues();
    assertThat(bodies, hasSize(2));

    assertThat(bodies.get(0), containsString("\"title\""));
    assertThat(bodies.get(1), containsString("title:"));
  }

  @Test
  void constructor_usesDefaultCacheSize() throws Exception {
    assertDoesNotThrow(() -> new ApiListingResource(swaggerModel, 10));
  }

  @Test
  void constructor_usesCustomCacheSize() throws Exception {
    assertDoesNotThrow(() -> new ApiListingResource(swaggerModel, 5));
  }

  @Test
  void getOpenApi_handlesComplexBaseUrl() throws Exception {
    BaseUrlHolder.set("http://localhost:8081/some/path/nexus", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("/some/path/nexus/service/rest"));
  }

  @Test
  void getOpenApi_jsonIsProperlyFormatted() throws Exception {

    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    // JSON should have proper structure with braces
    assertThat(body, containsString("{"));
    assertThat(body, containsString("}"));
    // Should contain openapi version field
    assertThat(body, containsString("openapi"));
  }

  @Test
  void getOpenApi_yamlIsProperlyFormatted() throws Exception {

    BaseUrlHolder.set("http://localhost:8081", "");

    underTest.getOpenApi("yaml");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    // YAML should not have JSON-style braces for the main object
    assertThat(body, containsString("openapi:"));
    assertThat(body, containsString("info:"));
    assertThat(body, containsString("title: Test API"));
  }

  @Test
  void getOpenApi_handlesRootPath() throws Exception {

    BaseUrlHolder.set("http://localhost:8081/", "");

    underTest.getOpenApi("json");

    staticResponse.verify(() -> Response.ok(bodyCaptor.capture(), typeCaptor.capture()));
    String body = bodyCaptor.getValue();
    assertThat(body, containsString("service/rest"));
  }
}
