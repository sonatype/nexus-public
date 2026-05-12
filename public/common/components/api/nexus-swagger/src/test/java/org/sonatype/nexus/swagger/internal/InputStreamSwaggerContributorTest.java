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

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Swagger;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
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
    // Given: Swagger with incorrect InputStream definition
    Swagger swagger = new Swagger();
    Map<String, Model> definitions = new LinkedHashMap<>();
    ModelImpl incorrectModel = new ModelImpl();
    incorrectModel.setType("object");
    definitions.put("InputStream", incorrectModel);
    definitions.put("OtherModel", new ModelImpl());
    swagger.setDefinitions(definitions);

    // When
    underTest.contribute(swagger);

    // Then: InputStream definition is fixed to binary type
    assertThat(swagger.getDefinitions().containsKey("InputStream"), is(true));
    Model inputStreamModel = swagger.getDefinitions().get("InputStream");
    assertThat(inputStreamModel, is(instanceOf(ModelImpl.class)));

    ModelImpl binaryModel = (ModelImpl) inputStreamModel;
    assertThat(binaryModel.getType(), is("string"));
    assertThat(binaryModel.getFormat(), is("binary"));

    // Other definitions unchanged
    assertThat(swagger.getDefinitions().containsKey("OtherModel"), is(true));
  }

  @Test
  public void testContribute_handlesNoInputStreamDefinition() {
    // Given: Swagger without InputStream definition
    Swagger swagger = new Swagger();
    Map<String, Model> definitions = new LinkedHashMap<>();
    definitions.put("OtherModel", new ModelImpl());
    swagger.setDefinitions(definitions);

    // When
    underTest.contribute(swagger);

    // Then: No InputStream added, other definitions unchanged
    assertThat(swagger.getDefinitions().containsKey("InputStream"), is(false));
    assertThat(swagger.getDefinitions().containsKey("OtherModel"), is(true));
  }

  @Test
  public void testContribute_handlesNullDefinitions() {
    // Given: Swagger with null definitions
    Swagger swagger = new Swagger();
    swagger.setDefinitions(null);

    // When/Then: Should not throw exception
    underTest.contribute(swagger);
    assertThat(swagger.getDefinitions(), is(nullValue()));
  }

  @Test
  public void testContribute_preservesOtherDefinitions() {
    // Given: Swagger with multiple definitions including InputStream
    Swagger swagger = new Swagger();
    Map<String, Model> definitions = new LinkedHashMap<>();

    ModelImpl inputStreamModel = new ModelImpl();
    inputStreamModel.setType("object");
    definitions.put("InputStream", inputStreamModel);

    ModelImpl otherModel1 = new ModelImpl();
    otherModel1.setType("string");
    definitions.put("Model1", otherModel1);

    ModelImpl otherModel2 = new ModelImpl();
    otherModel2.setType("integer");
    definitions.put("Model2", otherModel2);

    swagger.setDefinitions(definitions);

    // When
    underTest.contribute(swagger);

    // Then: Only InputStream is modified, others unchanged
    assertThat(swagger.getDefinitions().size(), is(3));
    assertThat(swagger.getDefinitions().containsKey("InputStream"), is(true));
    assertThat(swagger.getDefinitions().containsKey("Model1"), is(true));
    assertThat(swagger.getDefinitions().containsKey("Model2"), is(true));

    ModelImpl fixed = (ModelImpl) swagger.getDefinitions().get("InputStream");
    assertThat(fixed.getType(), is("string"));
    assertThat(fixed.getFormat(), is("binary"));

    ModelImpl unchanged1 = (ModelImpl) swagger.getDefinitions().get("Model1");
    assertThat(unchanged1.getType(), is("string"));
    assertThat(unchanged1.getFormat(), is(nullValue()));

    ModelImpl unchanged2 = (ModelImpl) swagger.getDefinitions().get("Model2");
    assertThat(unchanged2.getType(), is("integer"));
  }
}
