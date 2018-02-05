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
package org.sonatype.nexus.coreui

import javax.annotation.Nullable

import org.sonatype.nexus.formfields.FormField
import org.sonatype.nexus.formfields.NumberTextFormField
import org.sonatype.nexus.formfields.Selectable

import groovy.transform.ToString

import static com.google.common.base.Preconditions.checkNotNull

/**
 * {@link FormField} exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class FormFieldXO
{
  String id

  String type

  String label

  String helpText

  Boolean required

  Boolean disabled

  Boolean readOnly

  @Nullable
  String regexValidation

  @Nullable
  String initialValue

  /**
   * @since 3.1
   */
  Map<String,Object> attributes

  // NumberTextFormField extensions; before attributes were introduced.  Use attributes for future extensions.

  /**
   * @see NumberTextFormField
   */
  @Nullable
  String minValue

  /**
   * @see NumberTextFormField
   */
  @Nullable
  String maxValue

  // Selectable extensions; before attributes were introduced.  Use attributes for future extensions.

  /**
   * @see Selectable
   */
  @Nullable
  String storeApi

  /**
   * @see Selectable
   */
  @Nullable
  Map<String, String> storeFilters

  /**
   * @see Selectable
   */
  @Nullable
  String idMapping

  /**
   * @see Selectable
   */
  @Nullable
  String nameMapping

  /**
   * Create transfer object from field source.
   *
   * @since 3.1
   */
  static FormFieldXO create(final FormField<?> source) {
    checkNotNull(source)

    FormFieldXO result = new FormFieldXO(
        id: source.id,
        type: source.type,
        label: source.label,
        helpText: source.helpText,
        required: source.required,
        disabled: source.disabled,
        readOnly: source.readOnly,
        regexValidation: source.regexValidation,
        initialValue: source.initialValue,
        attributes: source.attributes
    )

    // FIXME: transfer objects really should not change the field names; adds unneeded confusion and complexity
    if (source instanceof NumberTextFormField) {
      result.minValue = source.minimumValue
      result.maxValue = source.maximumValue
    }

    if (source instanceof Selectable) {
      result.storeApi = source.storeApi
      result.storeFilters = source.storeFilters
      result.idMapping = source.idMapping
      result.nameMapping = source.nameMapping
    }

    return result
  }
}
