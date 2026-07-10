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
package org.sonatype.nexus.formfields;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * {@link ComboboxFormField} tests.
 */
public class ComboboxFormFieldTest
{
  private static final String ID = "testId";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  private static final String INITIAL_VALUE = "testInitialValue";

  private ComboboxFormField<String> underTest;

  @Before
  public void setUp() {
    underTest = new ComboboxFormField<>(ID, LABEL, HELP_TEXT, FormField.MANDATORY, INITIAL_VALUE);
  }

  @Test
  public void when_CreatingWithAllArgs_Expect_CorrectState() {
    assertThat(underTest.getId(), equalTo(ID));
    assertThat(underTest.getLabel(), equalTo(LABEL));
    assertThat(underTest.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(underTest.isRequired());
    assertThat(underTest.getInitialValue(), equalTo(INITIAL_VALUE));
    assertThat(underTest.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingWithRequiredArg_Expect_NullInitialValue() {
    ComboboxFormField<String> field = new ComboboxFormField<>(ID, LABEL, HELP_TEXT, FormField.MANDATORY);

    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(field.isRequired());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingWithHelpText_Expect_OptionalAndNullInitialValue() {
    ComboboxFormField<String> field = new ComboboxFormField<>(ID, LABEL, HELP_TEXT);

    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertThat(field.isRequired(), is(FormField.OPTIONAL));
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingWithLabel_Expect_NullHelpTextAndOptional() {
    ComboboxFormField<String> field = new ComboboxFormField<>(ID, LABEL);

    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), nullValue());
    assertThat(field.isRequired(), is(FormField.OPTIONAL));
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_GettingType_Expect_ComboboxConstant() {
    assertThat(underTest.getType(), equalTo("combobox"));
  }

  @Test
  public void when_NoStoreApiSet_Expect_NullStoreApi() {
    assertThat(underTest.getStoreApi(), nullValue());
  }

  @Test
  public void when_NoStoreFiltersSet_Expect_NullStoreFilters() {
    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void when_NoIdMappingSet_Expect_NullIdMapping() {
    assertThat(underTest.getIdMapping(), nullValue());
  }

  @Test
  public void when_NoNameMappingSet_Expect_NullNameMapping() {
    assertThat(underTest.getNameMapping(), nullValue());
  }

  @Test
  public void when_WithStoreApi_Expect_StoreApiSetAndFluentReturn() {
    ComboboxFormField<String> result = underTest.withStoreApi("some_StoreApi");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getStoreApi(), equalTo("some_StoreApi"));
  }

  @Test
  public void when_WithStoreApiNull_Expect_NullPointerException() {
    assertThrows(NullPointerException.class, () -> underTest.withStoreApi(null));
  }

  @Test
  public void when_WithStoreFilter_Expect_FilterAddedAndFluentReturn() {
    Combobox<String> result = underTest.withStoreFilter("format", "maven");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getStoreFilters(), notNullValue());
    assertThat(underTest.getStoreFilters().get("format"), is("maven"));
  }

  @Test
  public void when_WithStoreFilterNullProperty_Expect_NullPointerException() {
    NullPointerException thrown =
        assertThrows(NullPointerException.class, () -> underTest.withStoreFilter(null, "maven"));
    assertThat(thrown.getMessage(), containsString("property"));
  }

  @Test
  public void when_WithStoreFilterNullValue_Expect_NullPointerException() {
    NullPointerException thrown =
        assertThrows(NullPointerException.class, () -> underTest.withStoreFilter("format", null));
    assertThat(thrown.getMessage(), containsString("value"));
  }

  @Test
  public void when_WithIdMapping_Expect_IdMappingSetAndFluentReturn() {
    ComboboxFormField<String> result = underTest.withIdMapping("idField");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getIdMapping(), equalTo("idField"));
  }

  @Test
  public void when_WithIdMappingNull_Expect_NullIdMapping() {
    ComboboxFormField<String> result = underTest.withIdMapping(null);

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getIdMapping(), nullValue());
  }

  @Test
  public void when_WithNameMapping_Expect_NameMappingSetAndFluentReturn() {
    ComboboxFormField<String> result = underTest.withNameMapping("nameField");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getNameMapping(), equalTo("nameField"));
  }

  @Test
  public void when_WithNameMappingNull_Expect_NullNameMapping() {
    ComboboxFormField<String> result = underTest.withNameMapping(null);

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getNameMapping(), nullValue());
  }

  @Test
  public void when_WithStoreApiEmptyString_Expect_EmptyStringSet() {
    ComboboxFormField<String> result = underTest.withStoreApi("");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getStoreApi(), equalTo(""));
    // getStoreFilters() is independent of storeApi; it stays null while no filters are set
    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void when_MultipleStoreFiltersAdded_Expect_AllPresent() {
    underTest.withStoreFilter("format", "maven");
    underTest.withStoreFilter("type", "hosted");

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.size(), is(2));
    assertThat(filters.get("format"), is("maven"));
    assertThat(filters.get("type"), is("hosted"));
  }

  @Test
  public void when_WithStoreFilterSameProperty_Expect_ValueOverwritten() {
    underTest.withStoreFilter("format", "maven");
    underTest.withStoreFilter("format", "npm");

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters.size(), is(1));
    assertThat(filters.get("format"), is("npm"));
  }

  @Test
  public void when_WithStoreFilterEmptyStrings_Expect_FilterAdded() {
    underTest.withStoreFilter("", "");

    assertThat(underTest.getStoreFilters().get(""), is(""));
  }

  @Test
  public void when_StoreFiltersPopulated_Expect_SameMapInstanceOnRepeatedCalls() {
    underTest.withStoreFilter("format", "maven");

    Map<String, String> first = underTest.getStoreFilters();
    Map<String, String> second = underTest.getStoreFilters();
    assertThat(first, notNullValue());
    assertThat(second, sameInstance(first));
  }
}
