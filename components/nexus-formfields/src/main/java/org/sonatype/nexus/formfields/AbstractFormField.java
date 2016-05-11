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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Support for {@link FormField} implementations.
 */
public abstract class AbstractFormField<T>
    implements FormField<T>
{
  private String helpText;

  private String id;

  @Nullable
  private String regexValidation;

  private boolean required;

  private String label;

  @Nullable
  private T initialValue;

  /**
   * @since 3.1
   */
  private Map<String,Object> attributes;

  // NOTE: avoid adding anymore constructors

  public AbstractFormField(final String id,
                           final String label,
                           final String helpText,
                           final boolean required,
                           @Nullable final String regexValidation,
                           @Nullable final T initialValue)
  {
    this(id, label, helpText, required, regexValidation);
    this.initialValue = initialValue;
  }

  public AbstractFormField(final String id,
                           final String label,
                           final String helpText,
                           final boolean required,
                           @Nullable final String regexValidation)
  {
    this(id, label, helpText, required);
    this.regexValidation = regexValidation;
  }

  public AbstractFormField(final String id,
                           final String label,
                           final String helpText,
                           final boolean required)
  {
    this(id);
    this.label = label;
    this.helpText = helpText;
    this.required = required;
  }

  public AbstractFormField(final String id) {
    this.id = id;
  }

  public String getLabel() {
    return this.label;
  }

  public String getHelpText() {
    return this.helpText;
  }

  public String getId() {
    return this.id;
  }

  @Nullable
  public String getRegexValidation() {
    return this.regexValidation;
  }

  public boolean isRequired() {
    return this.required;
  }

  @Nullable
  public T getInitialValue() {
    return initialValue;
  }

  public void setHelpText(final String helpText) {
    this.helpText = helpText;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setRegexValidation(@Nullable final String regex) {
    this.regexValidation = regex;
  }

  public void setRequired(final boolean required) {
    this.required = required;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public void setInitialValue(@Nullable final T value) {
    this.initialValue = value;
  }

  /**
   * @since 3.1
   */
  @Override
  public Map<String, Object> getAttributes() {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    return attributes;
  }
}
