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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * {@link ItemselectFormField} tests.
 */
public class ItemselectFormFieldTest
{
  private static final String ID = "testId";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  private static final String INITIAL_VALUE = "testInitialValue";

  private ItemselectFormField underTest;

  @Before
  public void setUp() {
    underTest = new ItemselectFormField(ID, LABEL, HELP_TEXT, FormField.MANDATORY, INITIAL_VALUE);
  }

  @Test
  public void when_CreatingNew_WithFullConstructor_Expect_AllValuesSet() {
    assertThat(underTest.getId(), equalTo(ID));
    assertThat(underTest.getLabel(), equalTo(LABEL));
    assertThat(underTest.getHelpText(), equalTo(HELP_TEXT));
    assertThat(underTest.isRequired(), is(true));
    assertThat(underTest.getInitialValue(), equalTo(INITIAL_VALUE));
    assertThat(underTest.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithRequiredConstructor_Expect_NullInitialValue() {
    ItemselectFormField field = new ItemselectFormField(ID, LABEL, HELP_TEXT, FormField.MANDATORY);

    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertThat(field.isRequired(), is(true));
    assertThat(field.getInitialValue(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithHelpTextConstructor_Expect_OptionalAndNotRequired() {
    ItemselectFormField field = new ItemselectFormField(ID, LABEL, HELP_TEXT);

    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertThat(field.isRequired(), is(false));
    assertThat(field.getInitialValue(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithLabelConstructor_Expect_NullHelpText() {
    ItemselectFormField field = new ItemselectFormField(ID, LABEL);

    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), nullValue());
    assertThat(field.isRequired(), is(false));
    assertThat(field.getInitialValue(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void getType_Expect_ItemselectConstant() {
    assertThat(ItemselectFormField.TYPE, equalTo("itemselect"));
    assertThat(underTest.getType(), equalTo("itemselect"));
  }

  @Test
  public void getStoreApi_Expect_NullByDefault() {
    assertThat(underTest.getStoreApi(), nullValue());
  }

  @Test
  public void setStoreApi_Expect_StoreApiSet() {
    underTest.setStoreApi("coreui_Repository.read");

    assertThat(underTest.getStoreApi(), equalTo("coreui_Repository.read"));
  }

  @Test
  public void getStoreFilters_Expect_NullWhenEmpty() {
    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void addStoreFilter_Expect_FilterPresent() {
    underTest.addStoreFilter("format", "maven2");

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.get("format"), equalTo("maven2"));
  }

  @Test
  public void getIdMapping_Expect_NullByDefault() {
    assertThat(underTest.getIdMapping(), nullValue());
  }

  @Test
  public void setIdMapping_Expect_IdMappingSet() {
    underTest.setIdMapping("recordId");

    assertThat(underTest.getIdMapping(), equalTo("recordId"));
  }

  @Test
  public void getNameMapping_Expect_NullByDefault() {
    assertThat(underTest.getNameMapping(), nullValue());
  }

  @Test
  public void setNameMapping_Expect_NameMappingSet() {
    underTest.setNameMapping("recordName");

    assertThat(underTest.getNameMapping(), equalTo("recordName"));
  }

  @Test
  public void setButtons_Expect_ButtonsAttributeSet() {
    String[] buttons = {"add", "remove"};
    underTest.setButtons(buttons);

    assertThat(underTest.getAttributes().get("buttons"), sameInstance((Object) buttons));
  }

  @Test
  public void setFromTitle_Expect_FromTitleAttributeSet() {
    underTest.setFromTitle("Available");

    assertThat(underTest.getAttributes().get("fromTitle"), equalTo("Available"));
  }

  @Test
  public void setToTitle_Expect_ToTitleAttributeSet() {
    underTest.setToTitle("Selected");

    assertThat(underTest.getAttributes().get("toTitle"), equalTo("Selected"));
  }

  @Test
  public void setValueAsString_Expect_ValueAsStringAttributeSet() {
    underTest.setValueAsString(true);

    assertThat(underTest.getAttributes().get("valueAsString"), equalTo((Object) true));
  }

  @Test
  public void withStoreApi_Expect_FluentReturnAndStateSet() {
    ItemselectFormField result = underTest.withStoreApi("coreui_Repository.read");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getStoreApi(), equalTo("coreui_Repository.read"));
  }

  @Test
  public void withIdMapping_Expect_FluentReturnAndStateSet() {
    ItemselectFormField result = underTest.withIdMapping("recordId");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getIdMapping(), equalTo("recordId"));
  }

  @Test
  public void withNameMapping_Expect_FluentReturnAndStateSet() {
    ItemselectFormField result = underTest.withNameMapping("recordName");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getNameMapping(), equalTo("recordName"));
  }

  @Test
  public void withStoreFilter_Expect_FluentReturnAndFilterPresent() {
    ItemselectFormField result = underTest.withStoreFilter("format", "maven2");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getStoreFilters().get("format"), equalTo("maven2"));
  }

  @Test
  public void withButtons_Expect_FluentReturnAndButtonsSet() {
    String[] buttons = {"add", "remove"};
    ItemselectFormField result = underTest.withButtons(buttons);

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getAttributes().get("buttons"), sameInstance((Object) buttons));
  }

  @Test
  public void withFromTitle_Expect_FluentReturnAndFromTitleSet() {
    ItemselectFormField result = underTest.withFromTitle("Available");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getAttributes().get("fromTitle"), equalTo("Available"));
  }

  @Test
  public void withToTitle_Expect_FluentReturnAndToTitleSet() {
    ItemselectFormField result = underTest.withToTitle("Selected");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getAttributes().get("toTitle"), equalTo("Selected"));
  }

  @Test
  public void withValueAsString_Expect_FluentReturnAndValueAsStringSet() {
    ItemselectFormField result = underTest.withValueAsString(false);

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getAttributes().get("valueAsString"), equalTo((Object) false));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void withListener_Expect_CreatesNewListenerMap() {
    ItemselectFormField result = underTest.withListener("change", "onChange");

    assertThat(result, sameInstance(underTest));
    Object listeners = underTest.getAttributes().get("listeners");
    assertThat(listeners, instanceOf(Map.class));
    assertThat(((Map<String, String>) listeners).get("change"), equalTo("onChange"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void withListener_Expect_ReusesExistingListenerMap() {
    underTest.withListener("change", "onChange");
    Map<String, String> firstMap = (Map<String, String>) underTest.getAttributes().get("listeners");

    underTest.withListener("select", "onSelect");
    Map<String, String> secondMap = (Map<String, String>) underTest.getAttributes().get("listeners");

    assertThat(secondMap, sameInstance(firstMap));
    assertThat(secondMap.size(), is(2));
    assertThat(secondMap.get("change"), equalTo("onChange"));
    assertThat(secondMap.get("select"), equalTo("onSelect"));
  }

  @Test
  public void withSelectionPlaceholderText_Expect_FluentReturnAndAttributeSet() {
    ItemselectFormField result = underTest.withSelectionPlaceholderText("Choose...");

    assertThat(result, sameInstance(underTest));
    assertThat(underTest.getAttributes().get("selectionPlaceholderText"), equalTo("Choose..."));
  }

  @Test
  public void when_CreatingNew_WithRequiredConstructor_AndOptional_Expect_NotRequired() {
    ItemselectFormField field = new ItemselectFormField(ID, LABEL, HELP_TEXT, FormField.OPTIONAL);

    assertThat(field.isRequired(), is(false));
    assertThat(field.getInitialValue(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void getAttributes_Expect_EmptyByDefault() {
    assertThat(underTest.getAttributes(), notNullValue());
    assertThat(underTest.getAttributes().isEmpty(), is(true));
  }

  @Test
  public void addStoreFilter_WithMultipleProperties_Expect_AllFiltersPresent() {
    underTest.addStoreFilter("format", "maven2");
    underTest.addStoreFilter("repositoryName", "central");

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters, notNullValue());
    assertThat(filters.size(), is(2));
    assertThat(filters.get("format"), equalTo("maven2"));
    assertThat(filters.get("repositoryName"), equalTo("central"));
  }

  @Test
  public void addStoreFilter_WithSameProperty_Expect_ValueOverwritten() {
    underTest.addStoreFilter("format", "maven2");
    underTest.addStoreFilter("format", "npm");

    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters.size(), is(1));
    assertThat(filters.get("format"), equalTo("npm"));
  }

  @Test
  public void getStoreFilters_WhenPopulated_Expect_SameInstanceAcrossCalls() {
    underTest.addStoreFilter("format", "maven2");

    assertThat(underTest.getStoreFilters(), sameInstance(underTest.getStoreFilters()));
  }

  @Test
  public void withStoreFilter_WithMultipleProperties_Expect_AllFiltersInSameMap() {
    underTest.addStoreFilter("format", "maven2");
    ItemselectFormField result = underTest.withStoreFilter("repositoryName", "central");

    assertThat(result, sameInstance(underTest));
    Map<String, String> filters = underTest.getStoreFilters();
    assertThat(filters.size(), is(2));
    assertThat(filters.get("format"), equalTo("maven2"));
    assertThat(filters.get("repositoryName"), equalTo("central"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void withListener_WithSameEvent_Expect_ListenerOverwritten() {
    underTest.withListener("change", "onChange");
    underTest.withListener("change", "onChangeUpdated");

    Map<String, String> listeners = (Map<String, String>) underTest.getAttributes().get("listeners");
    assertThat(listeners.size(), is(1));
    assertThat(listeners.get("change"), equalTo("onChangeUpdated"));
  }
}
