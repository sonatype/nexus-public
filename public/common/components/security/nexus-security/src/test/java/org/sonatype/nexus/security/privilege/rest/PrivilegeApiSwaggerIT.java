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
package org.sonatype.nexus.security.privilege.rest;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.nexus.swagger.internal.PrivilegeApiSwaggerContributor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test verifying that {@link PrivilegeApiSwaggerContributor} correctly patches
 * the OpenAPI spec generated from the real {@link PrivilegeApiResourceV1}.
 *
 * <p>
 * Scans the actual JAX-RS resource (no Spring context needed — the Reader only reads
 * annotations) and asserts that the GET privilege operations' 200 responses expose a
 * {@code oneOf} union rather than the abstract {@code ApiPrivilege} base schema.
 */
public class PrivilegeApiSwaggerIT
{
  private OpenAPI openApi;

  @BeforeEach
  public void setUp() {
    // Scan the real resource the same way SwaggerModel does at runtime
    Reader reader = new Reader(new OpenAPI());
    reader.read(PrivilegeApiResourceV1.class);
    openApi = reader.getOpenAPI();

    // Apply the contributor under test
    new PrivilegeApiSwaggerContributor().contribute(openApi);
  }

  @Test
  public void listPrivileges_200Response_isArrayOfOneOf() {
    Schema<?> schema = get200Schema("/v1/security/privileges");

    assertThat("list schema should be an array", schema, instanceOf(ArraySchema.class));

    Schema<?> items = ((ArraySchema) schema).getItems();
    assertThat("array items should be a oneOf union", items, instanceOf(ComposedSchema.class));
    assertThat("oneOf should contain at least the two nexus-security subtypes",
        ((ComposedSchema) items).getOneOf(), hasSize(greaterThanOrEqualTo(2)));
  }

  @Test
  public void getPrivilege_200Response_isOneOf() {
    Schema<?> schema = get200Schema("/v1/security/privileges/{privilegeName}");

    assertThat("single-get schema should be a oneOf union", schema, instanceOf(ComposedSchema.class));
    assertThat("oneOf should contain at least the two nexus-security subtypes",
        ((ComposedSchema) schema).getOneOf(), hasSize(greaterThanOrEqualTo(2)));
  }

  @Test
  public void listPrivileges_oneOf_containsApplicationAndWildcard() {
    // This IT lives in nexus-security whose test classpath includes only nexus-security classes.
    // ApiPrivilegeRepositoryAdmin/View/ContentSelector (nexus-repository-services) and
    // ApiPrivilegeScript (nexus-script-plugin) are NOT on the test classpath here, so the
    // contributor's Class.forName silently skips them — only 2 of 6 subtypes are verifiable.
    // The full 6-subtype union is exercised at runtime where all bundles are on the TCCL.
    Schema<?> items = ((ArraySchema) get200Schema("/v1/security/privileges")).getItems();
    ComposedSchema oneOf = (ComposedSchema) items;

    boolean hasApplication = oneOf.getOneOf()
        .stream()
        .anyMatch(s -> s.get$ref() != null && s.get$ref().contains("ApiPrivilegeApplication"));
    boolean hasWildcard = oneOf.getOneOf()
        .stream()
        .anyMatch(s -> s.get$ref() != null && s.get$ref().contains("ApiPrivilegeWildcard"));

    assertThat("oneOf should reference ApiPrivilegeApplication", hasApplication, is(true));
    assertThat("oneOf should reference ApiPrivilegeWildcard", hasWildcard, is(true));
  }

  @Test
  public void listPrivileges_oneOf_hasTypeDiscriminator() {
    Schema<?> items = ((ArraySchema) get200Schema("/v1/security/privileges")).getItems();
    ComposedSchema oneOf = (ComposedSchema) items;

    assertThat(oneOf.getDiscriminator(), is(notNullValue()));
    assertThat(oneOf.getDiscriminator().getPropertyName(), is("type"));
  }

  @Test
  public void listPrivileges_subtypeSchemas_registeredInComponents() {
    assertThat(openApi.getComponents(), is(notNullValue()));
    assertThat("ApiPrivilegeApplication should be in components",
        openApi.getComponents().getSchemas().containsKey("ApiPrivilegeApplication"), is(true));
    assertThat("ApiPrivilegeWildcard should be in components",
        openApi.getComponents().getSchemas().containsKey("ApiPrivilegeWildcard"), is(true));
  }

  // ---- helper ----

  private Schema<?> get200Schema(final String path) {
    assertThat("path " + path + " not found in spec", openApi.getPaths().get(path), notNullValue());
    return openApi.getPaths()
        .get(path)
        .getGet()
        .getResponses()
        .get("200")
        .getContent()
        .get("application/json")
        .getSchema();
  }
}
