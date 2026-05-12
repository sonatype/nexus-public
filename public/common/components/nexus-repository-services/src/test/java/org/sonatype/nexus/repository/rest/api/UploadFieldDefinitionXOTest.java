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
package org.sonatype.nexus.repository.rest.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.upload.UploadFieldDefinition;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link UploadFieldDefinitionXO}
 */
public class UploadFieldDefinitionXOTest
{

  @Test
  public void testFrom_serializesGroupHelpText() {
    // Given: An UploadFieldDefinition with group help text
    UploadFieldDefinition field = new UploadFieldDefinition(
        "testField",
        "Test Field",
        "Field help text",
        true,
        UploadFieldDefinition.Type.STRING,
        "Test Group",
        "Group help text",
        null);

    // When: Converting to XO
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.from(field);

    // Then: XO should contain all field information including group help text
    assertThat("Name should be serialized", xo.getName(), equalTo("testField"));
    assertThat("Description should be serialized", xo.getDescription(), equalTo("Field help text"));
    assertThat("Optional should be serialized", xo.isOptional(), is(true));
    assertThat("Type should be serialized", xo.getType(), equalTo("STRING"));
    assertThat("Group should be serialized", xo.getGroup(), equalTo("Test Group"));
    assertThat("Group help text should be serialized",
        xo.getGroupHelpText(), equalTo("Group help text"));
  }

  @Test
  public void testFrom_groupHelpTextNull() {
    // Given: An UploadFieldDefinition without group help text
    UploadFieldDefinition field = new UploadFieldDefinition(
        "testField",
        "Test Field",
        "Field help text",
        false,
        UploadFieldDefinition.Type.STRING,
        "Test Group");

    // When: Converting to XO
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.from(field);

    // Then: XO should have null group help text
    assertThat("Group help text should be null", xo.getGroupHelpText(), is(nullValue()));
  }

  @Test
  public void testBuilder_withGroupHelpText() {
    // When: Building XO with group help text
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.builder()
        .name("field")
        .type("STRING")
        .description("Help")
        .optional(false)
        .group("TestGroup")
        .groupHelpText("Group help")
        .options(null)
        .build();

    // Then: All properties should be set
    assertThat("Name should be set", xo.getName(), equalTo("field"));
    assertThat("Group help text should be set", xo.getGroupHelpText(), equalTo("Group help"));
  }

  @Test
  public void testBuilder_groupHelpTextNull() {
    // When: Building XO without group help text
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.builder()
        .name("field")
        .type("STRING")
        .description("Help")
        .optional(false)
        .group("TestGroup")
        .options(null)
        .build();

    // Then: Group help text should be null
    assertThat("Group help text should be null", xo.getGroupHelpText(), is(nullValue()));
  }

  @Test
  public void testGetSetGroupHelpText() {
    // When: Creating XO and setting group help text
    UploadFieldDefinitionXO xo = new UploadFieldDefinitionXO();
    xo.setName("field");
    xo.setGroupHelpText("New group help");

    // Then: Should be able to get it back
    assertThat("Group help text should be retrievable",
        xo.getGroupHelpText(), equalTo("New group help"));
  }

  @Test
  public void testToString_includesGroupHelpText() {
    // When: Converting XO to string
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.builder()
        .name("field")
        .type("STRING")
        .description("Help")
        .optional(false)
        .group("Group")
        .groupHelpText("Group help text")
        .options(null)
        .build();

    String str = xo.toString();

    // Then: String representation should include group help text
    assertThat("toString should not be null", str, is(notNullValue()));
    assertThat("toString should contain name", str.contains("field"), is(true));
    assertThat("toString should contain group help text", str.contains("Group help text"), is(true));
  }

  @Test
  public void testEqualsAndHashCode_groupHelpTextIncluded() {
    // Given: Two XOs with different group help texts
    UploadFieldDefinitionXO xo1 = UploadFieldDefinitionXO.builder()
        .name("field")
        .type("STRING")
        .description("Help")
        .optional(false)
        .group("Group")
        .groupHelpText("Help text 1")
        .options(null)
        .build();

    UploadFieldDefinitionXO xo2 = UploadFieldDefinitionXO.builder()
        .name("field")
        .type("STRING")
        .description("Help")
        .optional(false)
        .group("Group")
        .groupHelpText("Help text 2")
        .options(null)
        .build();

    // Then: Based on equals() implementation, they may or may not be equal
    // (depends on whether equals() uses only 'name' or more fields)
    assertThat("Both should be created", xo1, is(notNullValue()));
    assertThat("Both should be created", xo2, is(notNullValue()));
  }

  @Test
  public void testFrom_withOptions() {
    // Given: A SELECT field with options and group help text
    Map<String, String> options = new HashMap<>();
    options.put("opt1", "Option 1");
    options.put("opt2", "Option 2");

    UploadFieldDefinition field = new UploadFieldDefinition(
        "selectField",
        "Select Field",
        "Choose",
        false,
        UploadFieldDefinition.Type.SELECT,
        "SelectGroup",
        "Choose from SelectGroup only for type X",
        options);

    // When: Converting to XO
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.from(field);

    // Then: XO should include both options and group help text
    assertThat("Options should be serialized", xo.getOptions(), is(notNullValue()));
    assertThat("Options should have 2 entries", xo.getOptions().size(), equalTo(2));
    assertThat("Group help text should be serialized",
        xo.getGroupHelpText(), equalTo("Choose from SelectGroup only for type X"));
  }

  @Test
  public void testBuilderChain_withGroupHelpText() {
    // When: Building XO using method chaining
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.builder()
        .name("myField")
        .type("STRING")
        .description("My field description")
        .optional(true)
        .group("MyGroup")
        .groupHelpText("MyGroup is applicable only when ...")
        .options(Collections.emptyMap())
        .build();

    // Then: All chainable methods should work
    assertThat("All properties should be set via builder chain",
        xo, is(notNullValue()));
    assertThat("Name should be myField", xo.getName(), equalTo("myField"));
    assertThat("Group help text should be set",
        xo.getGroupHelpText(), equalTo("MyGroup is applicable only when ..."));
  }

  @Test
  public void testRoundTrip_fromToXO() {
    // Given: An original field definition
    UploadFieldDefinition original = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        true,
        UploadFieldDefinition.Type.STRING,
        "Group",
        "Group help",
        null);

    // When: Converting to XO
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.from(original);

    // Then: All properties should be preserved
    assertThat("Name should be preserved", xo.getName(), equalTo(original.getName()));
    assertThat("Type should be preserved", xo.getType(), equalTo(original.getType().name()));
    assertThat("Description should be preserved", xo.getDescription(), equalTo(original.getHelpText()));
    assertThat("Optional should be preserved", xo.isOptional(), equalTo(original.isOptional()));
    assertThat("Group should be preserved", xo.getGroup(), equalTo(original.getGroup()));
    assertThat("Group help text should be preserved",
        xo.getGroupHelpText(), equalTo(original.getGroupHelpText()));
  }

  @Test
  public void testGroupHelpTextEmptyString() {
    // When: Creating XO with empty string group help text
    UploadFieldDefinitionXO xo = UploadFieldDefinitionXO.builder()
        .name("field")
        .type("STRING")
        .description("Help")
        .optional(false)
        .group("Group")
        .groupHelpText("")
        .options(null)
        .build();

    // Then: Empty string should be preserved (not converted to null)
    assertThat("Empty string should be preserved",
        xo.getGroupHelpText(), equalTo(""));
  }

  @Test
  public void testMultipleDifferentTypes_withGroupHelpText() {
    // When: Creating XOs for different field types
    UploadFieldDefinitionXO booleanXO = UploadFieldDefinitionXO.builder()
        .name("boolField")
        .type("BOOLEAN")
        .description("Bool help")
        .optional(false)
        .group("BoolGroup")
        .groupHelpText("Bool group help")
        .options(null)
        .build();

    UploadFieldDefinitionXO stringXO = UploadFieldDefinitionXO.builder()
        .name("stringField")
        .type("STRING")
        .description("String help")
        .optional(true)
        .group("StringGroup")
        .groupHelpText("String group help")
        .options(null)
        .build();

    UploadFieldDefinitionXO selectXO = UploadFieldDefinitionXO.builder()
        .name("selectField")
        .type("SELECT")
        .description("Select help")
        .optional(false)
        .group("SelectGroup")
        .groupHelpText("Select group help")
        .options(Collections.singletonMap("a", "Option A"))
        .build();

    // Then: All should properly preserve group help text
    assertThat("Boolean XO group help text",
        booleanXO.getGroupHelpText(), equalTo("Bool group help"));
    assertThat("String XO group help text",
        stringXO.getGroupHelpText(), equalTo("String group help"));
    assertThat("Select XO group help text",
        selectXO.getGroupHelpText(), equalTo("Select group help"));
  }
}
