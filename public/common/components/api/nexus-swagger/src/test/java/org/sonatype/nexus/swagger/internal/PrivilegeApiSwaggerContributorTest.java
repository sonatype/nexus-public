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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link PrivilegeApiSwaggerContributor}.
 *
 * <p>
 * Uses the package-private constructor with fake in-classpath subtype FQCNs so tests
 * do not require the actual privilege subtype classes from nexus-security /
 * nexus-repository-services / nexus-script-plugin.
 */
public class PrivilegeApiSwaggerContributorTest
{
  /**
   * Fake privilege subtypes that are always on the test classpath.
   * They play the role of the real cross-module subtypes during tests.
   * {@code @Schema} annotations are intentional — tests verify that annotation
   * metadata survives {@code ModelConverters.readAll()}.
   */
  static class FakePrivilegeA
  {
    @io.swagger.v3.oas.annotations.media.Schema(description = "The privilege type discriminator")
    public String type;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Domain for fake privilege A")
    public String fieldA;
  }

  static class FakePrivilegeB
  {
    @io.swagger.v3.oas.annotations.media.Schema(description = "The privilege type discriminator")
    public String type;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Pattern for fake privilege B")
    public String fieldB;
  }

  private static final List<String> FAKE_SUBTYPES = List.of(
      PrivilegeApiSwaggerContributorTest.FakePrivilegeA.class.getName(),
      PrivilegeApiSwaggerContributorTest.FakePrivilegeB.class.getName());

  private PrivilegeApiSwaggerContributor underTest;

  @BeforeEach
  public void setUp() {
    underTest = new PrivilegeApiSwaggerContributor(FAKE_SUBTYPES);
  }

  @Test
  public void testContribute_noOp_whenPathsNull() {
    OpenAPI openApi = new OpenAPI();
    // No exception should be thrown
    underTest.contribute(openApi);
  }

  @Test
  public void testContribute_noOp_whenPrivilegePathsMissing() {
    OpenAPI openApi = new OpenAPI().paths(new Paths());
    underTest.contribute(openApi);
    assertThat(openApi.getPaths().isEmpty(), is(true));
  }

  @Test
  public void testContribute_patchesListOperation_itemsBecomesOneOf() {
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);

    Schema<?> listSchema = get200Schema(openApi, "/v1/security/privileges");
    assertThat("list 200 schema should be ArraySchema", listSchema, instanceOf(ArraySchema.class));
    Schema<?> items = ((ArraySchema) listSchema).getItems();
    assertThat("items should be ComposedSchema (oneOf)", items, instanceOf(ComposedSchema.class));
    assertThat("oneOf should contain an entry per fake subtype",
        ((ComposedSchema) items).getOneOf(), hasSize(greaterThan(0)));
  }

  @Test
  public void testContribute_patchesSingleGetOperation_schemaBecomesOneOf() {
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);

    Schema<?> schema = get200Schema(openApi, "/v1/security/privileges/{privilegeName}");
    assertThat("single-get 200 schema should be ComposedSchema (oneOf)",
        schema, instanceOf(ComposedSchema.class));
    assertThat("oneOf should contain an entry per fake subtype",
        ((ComposedSchema) schema).getOneOf(), hasSize(greaterThan(0)));
  }

  @Test
  public void testContribute_listOneOfHasDiscriminator() {
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);

    Schema<?> items = ((ArraySchema) get200Schema(openApi, "/v1/security/privileges")).getItems();
    assertThat(((ComposedSchema) items).getDiscriminator(), is(notNullValue()));
    assertThat(((ComposedSchema) items).getDiscriminator().getPropertyName(), is("type"));
  }

  @Test
  public void testContribute_singleGetOneOfHasDiscriminator() {
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);

    Schema<?> schema = get200Schema(openApi, "/v1/security/privileges/{privilegeName}");
    assertThat(((ComposedSchema) schema).getDiscriminator(), is(notNullValue()));
    assertThat(((ComposedSchema) schema).getDiscriminator().getPropertyName(), is("type"));
  }

  @Test
  public void testContribute_subtypeSchemaRegisteredInComponents() {
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);

    assertThat("components should be present", openApi.getComponents(), is(notNullValue()));
    assertThat("FakePrivilegeA schema should be registered",
        openApi.getComponents().getSchemas().containsKey("FakePrivilegeA"), is(true));
    assertThat("FakePrivilegeB schema should be registered",
        openApi.getComponents().getSchemas().containsKey("FakePrivilegeB"), is(true));
  }

  @Test
  public void testContribute_isIdempotent() {
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);
    int schemaCountAfterFirst = openApi.getComponents().getSchemas().size();

    underTest.contribute(openApi);

    // Schema count must not grow — no duplicate or extra registrations on second call
    assertThat(openApi.getComponents().getSchemas().size(), is(schemaCountAfterFirst));
    Schema<?> items = ((ArraySchema) get200Schema(openApi, "/v1/security/privileges")).getItems();
    assertThat(items, instanceOf(ComposedSchema.class));
  }

  @Test
  public void testContribute_schemaAnnotationMetadataSurvivesReadAll() {
    // Verifies that @Schema annotations on fake subtypes are reflected in the registered
    // component schemas — i.e. ModelConverters.readAll() honours annotation metadata.
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    underTest.contribute(openApi);

    Schema<?> fakeA = openApi.getComponents().getSchemas().get("FakePrivilegeA");
    assertThat("FakePrivilegeA schema should be registered", fakeA, is(notNullValue()));
    assertThat("fieldA description should be preserved from @Schema annotation",
        fakeA.getProperties().get("fieldA").getDescription(),
        is("Domain for fake privilege A"));
  }

  @Test
  public void testContribute_discriminatorContainsOnlyResolvedSubtypes() {
    // When a subtype FQCN cannot be resolved, its discriminator mapping must not appear.
    PrivilegeApiSwaggerContributor partial =
        new PrivilegeApiSwaggerContributor(List.of(
            PrivilegeApiSwaggerContributorTest.FakePrivilegeA.class.getName(),
            "com.example.DoesNotExist"));
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    partial.contribute(openApi);

    Schema<?> schema = get200Schema(openApi, "/v1/security/privileges/{privilegeName}");
    ComposedSchema oneOf = (ComposedSchema) schema;
    assertThat("oneOf should contain only the resolved subtype", oneOf.getOneOf(), hasSize(1));
    // Discriminator should be absent or empty since FakePrivilegeA has no TYPE_VALUE_BY_SIMPLE_NAME entry
    // (the fake isn't in the production map — the mapping is simply not added, no dangling ref)
  }

  @Test
  public void testDefaultSubtypesSyncWithTypeValueMap_contractTest() {
    // Contract: every FQCN in DEFAULT_SUBTYPE_FQCNS must have a matching entry in
    // TYPE_VALUE_BY_SIMPLE_NAME (keyed by simple class name), and the sizes must match.
    // This test fails if a developer adds a subtype to one list but forgets the other.
    assertThat("FQCN list and type-value map must have the same number of entries",
        PrivilegeApiSwaggerContributor.DEFAULT_SUBTYPE_FQCNS.size(),
        is(PrivilegeApiSwaggerContributor.TYPE_VALUE_BY_SIMPLE_NAME.size()));

    for (String fqcn : PrivilegeApiSwaggerContributor.DEFAULT_SUBTYPE_FQCNS) {
      String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
      assertThat("TYPE_VALUE_BY_SIMPLE_NAME must contain an entry for " + simpleName,
          PrivilegeApiSwaggerContributor.TYPE_VALUE_BY_SIMPLE_NAME.containsKey(simpleName),
          is(true));
    }
  }

  @Test
  public void testContribute_noOp_whenListPathHasNoGetOperation() {
    OpenAPI openApi = new OpenAPI().paths(new Paths()
        .addPathItem("/v1/security/privileges", new PathItem())
        .addPathItem("/v1/security/privileges/{privilegeName}",
            new PathItem().get(buildGetWith403())));

    underTest.contribute(openApi);

    // Only the single-get is patched; no NPE on the list path
    Schema<?> schema = get200Schema(openApi, "/v1/security/privileges/{privilegeName}");
    assertThat(schema, instanceOf(ComposedSchema.class));
  }

  @Test
  public void testContribute_noOp_whenAllSubtypesUnresolvable() {
    PrivilegeApiSwaggerContributor noClassesContributor =
        new PrivilegeApiSwaggerContributor(List.of("com.example.DoesNotExist"));
    OpenAPI openApi = buildOpenApiWithPrivilegePaths();

    noClassesContributor.contribute(openApi);

    // Contributor exits early; no 200 response is created when no subtypes could be resolved
    ApiResponse response200 = openApi.getPaths()
        .get("/v1/security/privileges/{privilegeName}")
        .getGet()
        .getResponses()
        .get("200");
    assertThat("no 200 response should be created when all subtypes are unresolvable",
        response200, is(nullValue()));
  }

  @Test
  public void testContribute_patchesList_whenExistingSchemaIsNotArraySchema() {
    // Covers the else-branch in patchListResponse where existing schema is not an ArraySchema
    OpenAPI openApi = new OpenAPI().paths(new Paths()
        .addPathItem("/v1/security/privileges",
            new PathItem().get(buildGet200("application/json", new Schema<>().$ref("ApiPrivilege"))))
        .addPathItem("/v1/security/privileges/{privilegeName}",
            new PathItem().get(buildGetWith403())));

    underTest.contribute(openApi);

    Schema<?> listSchema = get200Schema(openApi, "/v1/security/privileges");
    assertThat("non-ArraySchema should be replaced with ArraySchema wrapping oneOf",
        listSchema, instanceOf(ArraySchema.class));
    assertThat(((ArraySchema) listSchema).getItems(), instanceOf(ComposedSchema.class));
  }

  @Test
  public void testContribute_createsResponsesMap_whenOperationHasNullResponses() {
    // Covers the null-responses branch in ensureResponse200
    Operation getWithNullResponses = new Operation(); // no responses set at all
    OpenAPI openApi = new OpenAPI().paths(new Paths()
        .addPathItem("/v1/security/privileges/{privilegeName}",
            new PathItem().get(getWithNullResponses)));

    underTest.contribute(openApi);

    ApiResponse response200 = openApi.getPaths()
        .get("/v1/security/privileges/{privilegeName}")
        .getGet()
        .getResponses()
        .get("200");
    assertThat("200 response should be created even when operation had no responses",
        response200, is(notNullValue()));
    assertThat(response200.getContent().get("application/json").getSchema(),
        instanceOf(ComposedSchema.class));
  }

  // ---- helpers ----

  /** Simulates what the swagger-jaxrs2 Reader actually generates: no 200, only 403. */
  private OpenAPI buildOpenApiWithPrivilegePaths() {
    return new OpenAPI().paths(new Paths()
        .addPathItem("/v1/security/privileges",
            new PathItem().get(buildGetWith403()))
        .addPathItem("/v1/security/privileges/{privilegeName}",
            new PathItem().get(buildGetWith403())));
  }

  private Operation buildGetWith403() {
    return new Operation()
        .responses(new ApiResponses()
            .addApiResponse("403", new ApiResponse().description("Insufficient permissions")));
  }

  private Operation buildGet200(final String mediaType, final Schema<?> schema) {
    return new Operation()
        .responses(new ApiResponses()
            .addApiResponse("200", new ApiResponse()
                .content(new Content()
                    .addMediaType(mediaType, new MediaType().schema(schema)))));
  }

  private Schema<?> get200Schema(final OpenAPI openApi, final String path) {
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
