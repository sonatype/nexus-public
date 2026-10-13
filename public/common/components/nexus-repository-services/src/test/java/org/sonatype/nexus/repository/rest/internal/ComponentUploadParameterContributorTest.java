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
package org.sonatype.nexus.repository.rest.internal;

import java.util.List;

import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ComponentUploadParameterContributor}.
 *
 * <p>
 * Each test stubs {@link UploadManager#getAvailableDefinitions()} and then calls
 * {@link ComponentUploadParameterContributor#contribute(OpenAPI)} to assert the
 * {@code multipart/form-data} RequestBody schema written to the POST /v1/components operation.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentUploadParameterContributorTest
{
  @Mock
  private UploadManager uploadManager;

  // ---- helpers ----

  private OpenAPI apiWithComponentsPost() {
    Operation post = new Operation();
    PathItem item = new PathItem();
    item.operation(PathItem.HttpMethod.POST, post);
    return new OpenAPI().paths(new Paths().addPathItem("/v1/components", item));
  }

  @SuppressWarnings("unchecked")
  private Schema<?> multipartSchema(final OpenAPI api) {
    return api.getPaths()
        .get("/v1/components")
        .getPost()
        .getRequestBody()
        .getContent()
        .get("multipart/form-data")
        .getSchema();
  }

  /** Retrieves a named property from the schema's property map (handles raw-type conversion). */
  @SuppressWarnings("unchecked")
  private Schema<?> property(final Schema<?> schema, final String key) {
    return schema.getProperties().get(key);
  }

  // ---- tests ----

  /**
   * A single-upload format contributes one asset slot (unindexed), its component fields,
   * and its asset fields to the multipart schema.
   * <p>
   * For single-upload the asset index is empty, so the asset property key is
   * {@code "maven2.asset"}, its description is {@code "maven2 Asset"}, and an asset-field
   * description is {@code "maven2 Asset Extension"} (single spaces).
   */
  @Test
  public void contributesMultipartRequestBodyForSingleUploadFormat() {
    UploadDefinition maven2 = new UploadDefinition("maven2", true, true, false,
        List.of(new UploadFieldDefinition("groupId", false, UploadFieldDefinition.Type.STRING)),
        List.of(new UploadFieldDefinition("extension", false, UploadFieldDefinition.Type.STRING)));
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(maven2));
    ComponentUploadParameterContributor underTest = new ComponentUploadParameterContributor(uploadManager);

    OpenAPI api = apiWithComponentsPost();
    underTest.contribute(api);

    Schema<?> schema = multipartSchema(api);
    assertThat(schema, notNullValue());

    // component field: STRING "groupId" -> StringSchema, description "maven2 GroupId"
    Schema<?> groupIdProp = property(schema, "maven2.groupId");
    assertThat(groupIdProp, instanceOf(StringSchema.class));
    assertThat(groupIdProp.getDescription(), is("maven2 GroupId"));

    // single-upload asset slot: assetIndex="" -> key "maven2.asset", single-space description
    Schema<?> assetProp = property(schema, "maven2.asset");
    assertThat(assetProp, instanceOf(StringSchema.class));
    assertThat(assetProp.getFormat(), is("binary"));
    assertThat(assetProp.getDescription(), is("maven2 Asset"));

    // asset field "extension" under the single-upload slot -> single space in description
    Schema<?> assetExtProp = property(schema, "maven2.asset.extension");
    assertThat(assetExtProp, instanceOf(StringSchema.class));
    assertThat(assetExtProp.getDescription(), is("maven2 Asset Extension"));
  }

  /**
   * A multiple-upload format contributes exactly three indexed asset slots (asset1, asset2,
   * asset3) and no unindexed "asset" property.
   */
  @Test
  public void contributesThreeAssetSlotsForMultipleUploadFormat() {
    UploadDefinition npm = new UploadDefinition("npm", true, true, true,
        List.of(),
        List.of());
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(npm));
    ComponentUploadParameterContributor underTest = new ComponentUploadParameterContributor(uploadManager);

    OpenAPI api = apiWithComponentsPost();
    underTest.contribute(api);

    Schema<?> schema = multipartSchema(api);
    assertThat(schema, notNullValue());

    Schema<?> asset1 = property(schema, "npm.asset1");
    assertThat(asset1, instanceOf(StringSchema.class));
    assertThat(asset1.getFormat(), is("binary"));
    assertThat(asset1.getDescription(), is("npm Asset 1"));

    Schema<?> asset2 = property(schema, "npm.asset2");
    assertThat(asset2, instanceOf(StringSchema.class));
    assertThat(asset2.getFormat(), is("binary"));
    assertThat(asset2.getDescription(), is("npm Asset 2"));

    Schema<?> asset3 = property(schema, "npm.asset3");
    assertThat(asset3, instanceOf(StringSchema.class));
    assertThat(asset3.getFormat(), is("binary"));
    assertThat(asset3.getDescription(), is("npm Asset 3"));

    // No unindexed "npm.asset" property for multiple-upload formats
    assertThat(schema.getProperties(), not(hasKey("npm.asset")));
  }

  /**
   * BOOLEAN fields map to {@link BooleanSchema}; SELECT fields map to {@link StringSchema}.
   */
  @Test
  public void mapsFieldTypesToSchemaTypes() {
    UploadDefinition raw = new UploadDefinition("raw", true, true, false,
        List.of(
            new UploadFieldDefinition("enabled", false, UploadFieldDefinition.Type.BOOLEAN),
            new UploadFieldDefinition("layout", false, UploadFieldDefinition.Type.SELECT)),
        List.of());
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(raw));
    ComponentUploadParameterContributor underTest = new ComponentUploadParameterContributor(uploadManager);

    OpenAPI api = apiWithComponentsPost();
    underTest.contribute(api);

    Schema<?> schema = multipartSchema(api);

    assertThat(property(schema, "raw.enabled"), instanceOf(BooleanSchema.class));
    assertThat(property(schema, "raw.layout"), instanceOf(StringSchema.class));
  }

  /**
   * Calling {@code contribute} twice must not duplicate the contributor's own properties, and
   * must preserve properties added to the same multipart schema by another contributor.
   */
  @Test
  public void isIdempotentAndPreservesForeignProperties() {
    UploadDefinition maven2 = new UploadDefinition("maven2", true, true, false,
        List.of(new UploadFieldDefinition("groupId", false, UploadFieldDefinition.Type.STRING)),
        List.of());
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(maven2));
    ComponentUploadParameterContributor underTest = new ComponentUploadParameterContributor(uploadManager);

    OpenAPI api = apiWithComponentsPost();
    underTest.contribute(api);

    Schema<?> schemaAfterFirst = multipartSchema(api);
    String descAfterFirst = property(schemaAfterFirst, "maven2.groupId").getDescription();

    // simulate a property contributed elsewhere onto the same multipart schema
    schemaAfterFirst.addProperty("repository", new StringSchema().description("injected by another contributor"));
    int propCountWithForeign = schemaAfterFirst.getProperties().size();

    // second call must not duplicate our own properties nor discard the foreign one
    underTest.contribute(api);

    Schema<?> schemaAfterSecond = multipartSchema(api);
    assertThat(schemaAfterSecond.getProperties().size(), is(propCountWithForeign));
    assertThat(property(schemaAfterSecond, "maven2.groupId").getDescription(), is(descAfterFirst));
    Schema<?> foreign = property(schemaAfterSecond, "repository");
    assertThat(foreign, notNullValue());
    assertThat(foreign.getDescription(), is("injected by another contributor"));
  }

  /**
   * When the {@link OpenAPI} object has no paths, or has paths that do not include
   * {@code /v1/components}, {@code contribute} must be a no-op and must not throw.
   * <p>
   * No stub is set on {@code uploadManager} here because the contributor returns before
   * ever reaching {@code getAvailableDefinitions()} when the target path is absent.
   */
  @Test
  public void ignoresWhenComponentsPostAbsent() {
    // null paths: must not throw; paths must remain null
    ComponentUploadParameterContributor underTestNullPaths =
        new ComponentUploadParameterContributor(uploadManager);
    OpenAPI apiWithNullPaths = new OpenAPI();
    underTestNullPaths.contribute(apiWithNullPaths);
    assertThat(apiWithNullPaths.getPaths(), nullValue());

    // paths present but /v1/components is absent: must not throw and must not add the path
    ComponentUploadParameterContributor underTestOtherPaths =
        new ComponentUploadParameterContributor(uploadManager);
    OpenAPI apiWithOtherPaths = new OpenAPI().paths(
        new Paths().addPathItem("/v1/assets", new PathItem()));
    underTestOtherPaths.contribute(apiWithOtherPaths);
    assertThat(apiWithOtherPaths.getPaths().get("/v1/components"), nullValue());
  }

  /**
   * Formats that do not support API upload ({@code isApiUpload() == false}) must not contribute
   * any form fields, since POST /v1/components is the API upload endpoint.
   */
  @Test
  public void skipsFormatsThatDoNotSupportApiUpload() {
    UploadDefinition uiOnly = new UploadDefinition("uionly", true, false, false,
        List.of(new UploadFieldDefinition("groupId", false, UploadFieldDefinition.Type.STRING)),
        List.of());
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(uiOnly));
    ComponentUploadParameterContributor underTest = new ComponentUploadParameterContributor(uploadManager);

    OpenAPI api = apiWithComponentsPost();
    underTest.contribute(api);

    Schema<?> schema = multipartSchema(api);
    assertThat(schema.getProperties(), anyOf(nullValue(), is(anEmptyMap())));
  }
}
