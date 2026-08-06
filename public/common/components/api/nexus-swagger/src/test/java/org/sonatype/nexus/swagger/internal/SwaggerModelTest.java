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

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.swagger.SwaggerContributor;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SwaggerModel}.
 */
@ExtendWith(MockitoExtension.class)
public class SwaggerModelTest
{
  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private SwaggerContributor contributor;

  private SwaggerModel swaggerModel;

  @BeforeEach
  public void setUp() {
    lenient().when(applicationVersion.getVersion()).thenReturn("3.0.0");
  }

  @AfterEach
  public void tearDown() {
    // Reset ModelConverters instance between tests to avoid state leakage
    ModelConverters.reset();
  }

  @Test
  public void testSwaggerModelCreation() {
    // When
    swaggerModel = new SwaggerModel(applicationVersion, List.of(), List.of());

    // Then
    assertThat(swaggerModel, is(notNullValue()));
    assertThat(swaggerModel.getOpenApi(), is(notNullValue()));
    assertThat(swaggerModel.getOpenApi().getInfo().getTitle(), is("Nexus Repository Manager REST API"));
    assertThat(swaggerModel.getOpenApi().getInfo().getVersion(), is("3.0.0"));
  }

  @Test
  public void testEnumSerializationDoesNotUseToString() {
    // Given: an enum with a custom toString method
    // When SwaggerModel is created, it should configure ModelConverters to NOT use toString

    swaggerModel = new SwaggerModel(applicationVersion, List.of(), List.of());

    // Then: verify that the ModelResolver is configured to not use toString for enums
    ModelConverters instance = ModelConverters.getInstance();

    ModelResolver modelResolver = instance.getConverters()
        .stream()
        .filter(ModelResolver.class::isInstance)
        .map(ModelResolver.class::cast)
        .findFirst()
        .orElse(null);

    assertThat(modelResolver, is(notNullValue()));

    // Verify WRITE_ENUMS_USING_TO_STRING is disabled
    boolean writeEnumsUsingToString = modelResolver.objectMapper()
        .getSerializationConfig()
        .isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

    assertThat("WRITE_ENUMS_USING_TO_STRING should be disabled", writeEnumsUsingToString, is(false));
  }

  @Test
  public void testEnumValuesAreSerializedByName() {
    // Given
    swaggerModel = new SwaggerModel(applicationVersion, List.of(), List.of());

    // When: we resolve a schema for an enum type
    Schema<?> schema = ModelConverters.getInstance().readAllAsResolvedSchema(TestEnum.class).schema;

    // Then: the schema should contain the enum constant names, not toString values
    // containsInAnyOrder confirms length matches
    assertThat(schema.getEnum(), containsInAnyOrder("VALUE_ONE", "VALUE_TWO", "VALUE_THREE"));
  }

  @Test
  public void testModelResolverIsPresentAfterInitialization() {
    // When
    swaggerModel = new SwaggerModel(applicationVersion, List.of(), List.of());

    // Then: ModelConverters should have a ModelResolver registered
    ModelConverters instance = ModelConverters.getInstance();
    boolean hasModelResolver = instance.getConverters().stream().anyMatch(ModelResolver.class::isInstance);

    assertThat("ModelResolver should be present in converters", hasModelResolver, is(true));
  }

  @Test
  public void testRegisterConvertersFailsIfNoModelResolverPresent() {
    // Given: ModelConverters with no ModelResolver (cleared state)
    ModelConverters instance = ModelConverters.getInstance();
    instance.getConverters().forEach(instance::removeConverter);

    // When: trying to create a SwaggerModel without ModelResolver
    // Note: This test demonstrates the behavior of orElseThrow() in registerConverters
    // The actual behavior depends on whether a default ModelResolver gets re-registered
    assertThrows(NoSuchElementException.class, () -> new SwaggerModel(applicationVersion, List.of(), List.of()));
  }

  @Test
  public void testSwaggerModelWithContributors() {
    // Given
    List<SwaggerContributor> contributors = Collections.singletonList(contributor);

    // When
    swaggerModel = new SwaggerModel(applicationVersion, List.of(), contributors);

    // Then
    assertThat(swaggerModel, is(notNullValue()));
    assertThat(swaggerModel.getOpenApi(), is(notNullValue()));
  }

  @Test
  public void testOpenApiTitleAndVersion() {
    // Given
    when(applicationVersion.getVersion()).thenReturn("3.45.0-TEST");

    // When
    swaggerModel = new SwaggerModel(applicationVersion, List.of(), List.of());

    // Then
    OpenAPI openApi = swaggerModel.getOpenApi();
    assertThat(openApi.getInfo().getTitle(), is("Nexus Repository Manager REST API"));
    assertThat(openApi.getInfo().getVersion(), is("3.45.0-TEST"));
  }

  /**
   * Test enum with custom toString to verify the fix for NEXUS-63695.
   */
  private enum TestEnum
  {
    VALUE_ONE,
    VALUE_TWO,
    VALUE_THREE;

    @Override
    public String toString() {
      // Return lowercase to differentiate from name()
      return name().toLowerCase();
    }
  }
}
