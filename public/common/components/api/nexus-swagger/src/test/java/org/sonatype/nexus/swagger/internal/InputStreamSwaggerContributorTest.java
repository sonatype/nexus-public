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

import java.util.LinkedHashMap;
import java.util.Map;

// NEXUS-46395: migrated from Swagger 1.x model types (io.swagger.models.*) to
// OpenAPI 3.x (io.swagger.v3.oas.models.*). The top-level "definitions" map became
// components.schemas, and the Model/ModelImpl hierarchy was unified under Schema<?>.
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for {@link InputStreamSwaggerContributor}.
 */
public class InputStreamSwaggerContributorTest
{
  private InputStreamSwaggerContributor underTest;

  @Before
  public void setup() {
    underTest = new InputStreamSwaggerContributor();
  }

  @Test
  public void testContribute_fixesInputStreamDefinition() {
    // Given: OpenAPI with incorrect InputStream schema (type: object)
    OpenAPI openApi = new OpenAPI();
    Map<String, Schema> schemas = new LinkedHashMap<>();
    schemas.put("InputStream", new ObjectSchema());
    schemas.put("OtherModel", new ObjectSchema());
    openApi.setComponents(new Components().schemas(schemas));

    // When
    underTest.contribute(openApi);

    // Then: InputStream definition is replaced with a string/binary schema
    Map<String, Schema> result = openApi.getComponents().getSchemas();
    assertThat(result.containsKey("InputStream"), is(true));

    Schema<?> inputStreamSchema = result.get("InputStream");
    assertThat(inputStreamSchema, is(notNullValue()));
    assertThat(inputStreamSchema.getType(), is("string"));
    assertThat(inputStreamSchema.getFormat(), is("binary"));

    // Other definitions unchanged
    assertThat(result.containsKey("OtherModel"), is(true));
  }

  @Test
  public void testContribute_handlesNoInputStreamDefinition() {
    // Given: OpenAPI without InputStream definition
    OpenAPI openApi = new OpenAPI();
    Map<String, Schema> schemas = new LinkedHashMap<>();
    schemas.put("OtherModel", new ObjectSchema());
    openApi.setComponents(new Components().schemas(schemas));

    // When
    underTest.contribute(openApi);

    // Then: No InputStream added, other definitions unchanged
    Map<String, Schema> result = openApi.getComponents().getSchemas();
    assertThat(result.containsKey("InputStream"), is(false));
    assertThat(result.containsKey("OtherModel"), is(true));
  }

  @Test
  public void testContribute_handlesNullDefinitions() {
    // Given: OpenAPI without components / null schemas map
    OpenAPI openApi = new OpenAPI();
    // no components set at all
    underTest.contribute(openApi);
    assertThat(openApi.getComponents(), is(nullValue()));

    // and: components present but schemas null
    OpenAPI openApi2 = new OpenAPI();
    openApi2.setComponents(new Components());
    underTest.contribute(openApi2);
    assertThat(openApi2.getComponents().getSchemas(), is(nullValue()));
  }

  @Test
  public void testContribute_preservesOtherDefinitions() {
    // Given: OpenAPI with multiple schemas including InputStream
    OpenAPI openApi = new OpenAPI();
    Map<String, Schema> schemas = new LinkedHashMap<>();

    schemas.put("InputStream", new ObjectSchema());

    Schema<?> model1 = new Schema<>().type("string");
    schemas.put("Model1", model1);

    Schema<?> model2 = new Schema<>().type("integer");
    schemas.put("Model2", model2);

    openApi.setComponents(new Components().schemas(schemas));

    // When
    underTest.contribute(openApi);

    // Then: Only InputStream is modified, others unchanged
    Map<String, Schema> result = openApi.getComponents().getSchemas();
    assertThat(result.size(), is(3));
    assertThat(result.containsKey("InputStream"), is(true));
    assertThat(result.containsKey("Model1"), is(true));
    assertThat(result.containsKey("Model2"), is(true));

    Schema<?> fixed = result.get("InputStream");
    assertThat(fixed.getType(), is("string"));
    assertThat(fixed.getFormat(), is("binary"));

    Schema<?> unchanged1 = result.get("Model1");
    assertThat(unchanged1.getType(), is("string"));
    assertThat(unchanged1.getFormat(), is(nullValue()));

    Schema<?> unchanged2 = result.get("Model2");
    assertThat(unchanged2.getType(), is("integer"));
  }
}
