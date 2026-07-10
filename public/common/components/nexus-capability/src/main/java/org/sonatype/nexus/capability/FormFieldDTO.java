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
package org.sonatype.nexus.capability;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FormField")
public class FormFieldDTO
{
  @Schema(description = "Field identifier")
  private String id;

  @Schema(description = "Field type (e.g., string, password, number, checkbox, combobox)")
  private String type;

  @Schema(description = "Display label for the field")
  private String label;

  @Schema(description = "Help text shown to users")
  private String helpText;

  @Schema(description = "Whether the field is required")
  private Boolean required;

  @Schema(description = "Whether the field is disabled")
  private Boolean disabled;

  @Schema(description = "Whether the field is read-only")
  private Boolean readOnly;

  @Nullable
  @Schema(description = "Regular expression for field validation")
  private String regexValidation;

  @Nullable
  @Schema(description = "Initial value for the field")
  private String initialValue;

  @Schema(description = "Additional attributes for the field")
  private Map<String, Object> attributes;

  @Nullable
  @Schema(description = "Minimum value (for number fields)")
  private String minimumValue;

  @Nullable
  @Schema(description = "Maximum value (for number fields)")
  private String maximumValue;

  @Nullable
  @Schema(description = "API endpoint for fetching selectable options")
  private String storeApi;

  @Nullable
  @Schema(description = "Filters to apply when fetching options from the store API")
  private Map<String, String> storeFilters;

  @Nullable
  @Schema(description = "Property path for the ID field in store API response")
  private String idMapping;

  @Nullable
  @Schema(description = "Property path for the name/display field in store API response")
  private String nameMapping;

  @Schema(description = "Whether autocomplete is enabled for this field")
  private boolean allowAutocomplete;

  protected FormFieldDTO() {
    // deserialization
  }

  public FormFieldDTO(
      final String id,
      final String type,
      final String label,
      final String helpText,
      final Boolean required,
      final Boolean disabled,
      final Boolean readOnly,
      @Nullable final String regexValidation,
      @Nullable final String initialValue,
      final Map<String, Object> attributes,
      @Nullable final String minimumValue,
      @Nullable final String maximumValue,
      @Nullable final String storeApi,
      @Nullable final Map<String, String> storeFilters,
      @Nullable final String idMapping,
      @Nullable final String nameMapping,
      final boolean allowAutocomplete)
  {
    this.id = id;
    this.type = type;
    this.label = label;
    this.helpText = helpText;
    this.required = required;
    this.disabled = disabled;
    this.readOnly = readOnly;
    this.regexValidation = regexValidation;
    this.initialValue = initialValue;
    this.attributes = attributes;
    this.minimumValue = minimumValue;
    this.maximumValue = maximumValue;
    this.storeApi = storeApi;
    this.storeFilters = storeFilters;
    this.idMapping = idMapping;
    this.nameMapping = nameMapping;
    this.allowAutocomplete = allowAutocomplete;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public String getHelpText() {
    return helpText;
  }

  public void setHelpText(final String helpText) {
    this.helpText = helpText;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(final Boolean required) {
    this.required = required;
  }

  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(final Boolean disabled) {
    this.disabled = disabled;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  public void setReadOnly(final Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getRegexValidation() {
    return regexValidation;
  }

  public void setRegexValidation(final String regexValidation) {
    this.regexValidation = regexValidation;
  }

  public String getInitialValue() {
    return initialValue;
  }

  public void setInitialValue(final String initialValue) {
    this.initialValue = initialValue;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public String getMinimumValue() {
    return minimumValue;
  }

  public void setMinimumValue(final String minimumValue) {
    this.minimumValue = minimumValue;
  }

  public String getMaximumValue() {
    return maximumValue;
  }

  public void setMaximumValue(final String maximumValue) {
    this.maximumValue = maximumValue;
  }

  public String getStoreApi() {
    return storeApi;
  }

  public void setStoreApi(final String storeApi) {
    this.storeApi = storeApi;
  }

  public Map<String, String> getStoreFilters() {
    return storeFilters;
  }

  public void setStoreFilters(final Map<String, String> storeFilters) {
    this.storeFilters = storeFilters;
  }

  public String getIdMapping() {
    return idMapping;
  }

  public void setIdMapping(final String idMapping) {
    this.idMapping = idMapping;
  }

  public String getNameMapping() {
    return nameMapping;
  }

  public void setNameMapping(final String nameMapping) {
    this.nameMapping = nameMapping;
  }

  public boolean isAllowAutocomplete() {
    return allowAutocomplete;
  }

  public void setAllowAutocomplete(final boolean allowAutocomplete) {
    this.allowAutocomplete = allowAutocomplete;
  }

  @Override
  public String toString() {
    return "FormFieldDTO{" +
        "id='" + id + '\'' +
        ", type='" + type + '\'' +
        ", label='" + label + '\'' +
        ", helpText='" + helpText + '\'' +
        ", required=" + required +
        ", disabled=" + disabled +
        ", regexValidation='" + regexValidation + '\'' +
        ", initialValue='" + initialValue + '\'' +
        ", attributes=" + attributes +
        ", minimumValue='" + minimumValue + '\'' +
        ", maximumValue='" + maximumValue + '\'' +
        ", storeApi='" + storeApi + '\'' +
        ", storeFilters=" + storeFilters +
        ", idMapping='" + idMapping + '\'' +
        ", nameMapping='" + nameMapping + '\'' +
        ", allowAutocomplete=" + allowAutocomplete +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FormFieldDTO that = (FormFieldDTO) o;
    return allowAutocomplete == that.allowAutocomplete && Objects.equals(id, that.id) &&
        Objects.equals(type, that.type) && Objects.equals(label, that.label) &&
        Objects.equals(helpText, that.helpText) && Objects.equals(required, that.required) &&
        Objects.equals(disabled, that.disabled) && Objects.equals(readOnly, that.readOnly) &&
        Objects.equals(regexValidation, that.regexValidation) &&
        Objects.equals(initialValue, that.initialValue) &&
        Objects.equals(attributes, that.attributes) && Objects.equals(minimumValue, that.minimumValue) &&
        Objects.equals(maximumValue, that.maximumValue) && Objects.equals(storeApi, that.storeApi) &&
        Objects.equals(storeFilters, that.storeFilters) &&
        Objects.equals(idMapping, that.idMapping) && Objects.equals(nameMapping, that.nameMapping);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, label, helpText, required, disabled, readOnly, regexValidation, initialValue,
        attributes, minimumValue, maximumValue, storeApi, storeFilters, idMapping, nameMapping, allowAutocomplete);
  }
}
