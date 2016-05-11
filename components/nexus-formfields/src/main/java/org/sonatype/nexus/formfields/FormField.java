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

import javax.annotation.Nullable;

/**
 * Form field.
 *
 * @param <T> The data type of the field.
 */
public interface FormField<T>
{
  /**
   * Mandatory ({@code true}) symbol.
   *
   * @see #isRequired()
   */
  boolean MANDATORY = true;

  /**
   * Optional ({@code false}) symbol.
   *
   * @see #isRequired()
   */
  boolean OPTIONAL = false;

  /**
   * Field type.
   *
   * This is a symbolic type to match up the widget implementation for the field in the UI.
   */
  String getType();

  /**
   * Field label.
   */
  String getLabel();

  /**
   * Field identifier.
   */
  String getId();

  /**
   * True if field is required.
   */
  boolean isRequired();

  /**
   * Help text of field.
   */
  String getHelpText();

  /**
   * Optional regular-expression to validate field.
   */
  @Nullable
  String getRegexValidation();

  /**
   * Optional initial value of the field.
   *
   * @since 2.3
   */
  @Nullable
  T getInitialValue();

  /**
   * Optional field attributes.
   *
   * Used to encode additional data to widget implementation in UI.
   *
   * Care must be used to ensure that values are transferable, and likely should remain simple values,
   * collections of simple values or simple transfer objects.
   *
   * @since 3.1
   */
  Map<String,Object> getAttributes();
}
