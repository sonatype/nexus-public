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
package org.sonatype.nexus.repository.upload;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link UploadFieldDefinition}
 */
public class UploadFieldDefinitionTest
{

  @Test
  public void testConstructor_withGroupHelpText() {
    // When: Creating field definition with group help text
    String fieldName = "testField";
    String displayName = "Test Field";
    String helpText = "This is help text";
    String group = "Test Group";
    String groupHelpText = "This is group help text";

    UploadFieldDefinition field = new UploadFieldDefinition(
        fieldName,
        displayName,
        helpText,
        true,
        UploadFieldDefinition.Type.STRING,
        group,
        groupHelpText,
        null);

    // Then: All fields should be properly set
    assertThat("Field name should be set", field.getName(), equalTo(fieldName));
    assertThat("Display name should be set", field.getDisplayName(), equalTo(displayName));
    assertThat("Help text should be set", field.getHelpText(), equalTo(helpText));
    assertThat("Should be optional", field.isOptional(), is(true));
    assertThat("Type should be STRING", field.getType(), equalTo(UploadFieldDefinition.Type.STRING));
    assertThat("Group should be set", field.getGroup(), equalTo(group));
    assertThat("Group help text should be set", field.getGroupHelpText(), equalTo(groupHelpText));
  }

  @Test
  public void testConstructor_withoutGroupHelpText() {
    // When: Creating field definition using older constructor without group help text
    String fieldName = "testField";
    String group = "Test Group";

    UploadFieldDefinition field = new UploadFieldDefinition(
        fieldName,
        "Test Field",
        "Help text",
        false,
        UploadFieldDefinition.Type.STRING,
        group);

    // Then: Group help text should be null
    assertThat("Field name should be set", field.getName(), equalTo(fieldName));
    assertThat("Group should be set", field.getGroup(), equalTo(group));
    assertThat("Group help text should be null", field.getGroupHelpText(), is(nullValue()));
  }

  @Test
  public void testConstructor_groupHelpTextNull() {
    // When: Creating field definition with explicit null group help text
    UploadFieldDefinition field = new UploadFieldDefinition(
        "testField",
        "Test Field",
        "Help text",
        true,
        UploadFieldDefinition.Type.STRING,
        "Test Group",
        null,
        null);

    // Then: Group help text should be null
    assertThat("Group help text should be null", field.getGroupHelpText(), is(nullValue()));
  }

  @Test
  public void testEqualsAndHashCode_groupHelpTextIncluded() {
    // Given: Two field definitions with different group help texts
    UploadFieldDefinition field1 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        "help text 1",
        null);

    UploadFieldDefinition field2 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        "help text 2",
        null);

    // Then: They should NOT be equal (group help text is different)
    assertThat("Fields with different group help text should not be equal",
        field1.equals(field2), is(false));
    assertThat("Hash codes should be different",
        field1.hashCode() != field2.hashCode(), is(true));
  }

  @Test
  public void testEqualsAndHashCode_groupHelpTextSame() {
    // Given: Two field definitions with same group help texts
    UploadFieldDefinition field1 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        "same help text",
        null);

    UploadFieldDefinition field2 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        "same help text",
        null);

    // Then: They should be equal
    assertThat("Fields with same group help text should be equal",
        field1.equals(field2), is(true));
    assertThat("Hash codes should be same",
        field1.hashCode() == field2.hashCode(), is(true));
  }

  @Test
  public void testEqualsAndHashCode_oneNullGroupHelpText() {
    // Given: Two field definitions, one with null and one with group help text
    UploadFieldDefinition field1 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        null,
        null);

    UploadFieldDefinition field2 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        "help text",
        null);

    // Then: They should NOT be equal
    assertThat("Fields with different group help text nullability should not be equal",
        field1.equals(field2), is(false));
  }

  @Test
  public void testEqualsAndHashCode_bothNullGroupHelpText() {
    // Given: Two field definitions with both null group help texts
    UploadFieldDefinition field1 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        null,
        null);

    UploadFieldDefinition field2 = new UploadFieldDefinition(
        "field",
        "Display",
        "Help",
        false,
        UploadFieldDefinition.Type.STRING,
        "group",
        null,
        null);

    // Then: They should be equal
    assertThat("Fields with both null group help text should be equal",
        field1.equals(field2), is(true));
  }

  @Test
  public void testGetGroupHelpText_returnsNull() {
    // When: Getting group help text from field without it
    UploadFieldDefinition field = new UploadFieldDefinition(
        "field",
        false,
        UploadFieldDefinition.Type.STRING,
        "group");

    // Then: Should return null gracefully
    assertThat("Should return null", field.getGroupHelpText(), is(nullValue()));
  }

  @Test
  public void testSetGroupHelpText() {
    // When: Setting group help text on field
    UploadFieldDefinition field = new UploadFieldDefinition(
        "field",
        false,
        UploadFieldDefinition.Type.STRING);

    String groupHelpText = "This is the new help text";
    field.setGroupHelpText(groupHelpText);

    // Then: Should return the set value
    assertThat("Should return set group help text",
        field.getGroupHelpText(), equalTo(groupHelpText));
  }

  @Test
  public void testToString_doesNotThrowException() {
    // When: Converting field definition to string
    UploadFieldDefinition field = new UploadFieldDefinition(
        "testField",
        "Test Field",
        "Help text",
        true,
        UploadFieldDefinition.Type.STRING,
        "Test Group",
        "Group help text",
        null);

    String toString = field.toString();

    // Then: toString should not throw exception and return a string
    assertThat("toString should not be null", toString, is(notNullValue()));
    assertThat("toString should return a valid string", toString.length() > 0, is(true));
  }

  @Test
  public void testConstructor_withOptions_andGroupHelpText() {
    // When: Creating SELECT field with options and group help text
    Map<String, String> options = new HashMap<>();
    options.put("option1", "Option 1");
    options.put("option2", "Option 2");

    UploadFieldDefinition field = new UploadFieldDefinition(
        "selectField",
        "Select Field",
        "Choose an option",
        false,
        UploadFieldDefinition.Type.SELECT,
        "Select Group",
        "Choose from this group only when type is X",
        options);

    // Then: All properties should be set correctly
    assertThat("Field name should be selectField", field.getName(), equalTo("selectField"));
    assertThat("Options should be set", field.getOptions(), is(notNullValue()));
    assertThat("Should have 2 options", field.getOptions().size(), equalTo(2));
    assertThat("Group help text should be set",
        field.getGroupHelpText(), equalTo("Choose from this group only when type is X"));
  }

  @Test
  public void testBackwardCompatibility_oldConstructors() {
    // When: Using old constructors without group help text parameter
    UploadFieldDefinition field1 = new UploadFieldDefinition("field", false, UploadFieldDefinition.Type.STRING);
    UploadFieldDefinition field2 =
        new UploadFieldDefinition("field", false, UploadFieldDefinition.Type.STRING, "group");
    UploadFieldDefinition field3 = new UploadFieldDefinition("field", "help", false, UploadFieldDefinition.Type.STRING);
    UploadFieldDefinition field4 =
        new UploadFieldDefinition("field", "help", false, UploadFieldDefinition.Type.STRING, "group");

    // Then: All should be created without errors
    assertThat("Field1 should be created", field1, is(notNullValue()));
    assertThat("Field2 should be created", field2, is(notNullValue()));
    assertThat("Field3 should be created", field3, is(notNullValue()));
    assertThat("Field4 should be created", field4, is(notNullValue()));

    // And group help text should be null for all
    assertThat("Field1 group help text should be null", field1.getGroupHelpText(), is(nullValue()));
    assertThat("Field2 group help text should be null", field2.getGroupHelpText(), is(nullValue()));
    assertThat("Field3 group help text should be null", field3.getGroupHelpText(), is(nullValue()));
    assertThat("Field4 group help text should be null", field4.getGroupHelpText(), is(nullValue()));
  }

  @Test
  public void testGroupHelpTextWithDifferentFieldTypes() {
    // When: Creating different field types with group help text
    UploadFieldDefinition booleanField = new UploadFieldDefinition(
        "boolField",
        "Boolean Field",
        "Boolean help",
        false,
        UploadFieldDefinition.Type.BOOLEAN,
        "Bool Group",
        "Boolean group help",
        null);

    UploadFieldDefinition stringField = new UploadFieldDefinition(
        "stringField",
        "String Field",
        "String help",
        true,
        UploadFieldDefinition.Type.STRING,
        "String Group",
        "String group help",
        null);

    UploadFieldDefinition selectField = new UploadFieldDefinition(
        "selectField",
        "Select Field",
        "Select help",
        false,
        UploadFieldDefinition.Type.SELECT,
        "Select Group",
        "Select group help",
        Collections.singletonMap("opt1", "Option 1"));

    // Then: All should properly store and return group help text
    assertThat("Boolean field group help text",
        booleanField.getGroupHelpText(), equalTo("Boolean group help"));
    assertThat("String field group help text",
        stringField.getGroupHelpText(), equalTo("String group help"));
    assertThat("Select field group help text",
        selectField.getGroupHelpText(), equalTo("Select group help"));
  }
}
