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
package org.sonatype.nexus.rest;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Validation error exchange object.
 *
 * @since 3.0
 */
@XmlRootElement(name = "validationError")
public class ValidationErrorXO
{
  /**
   * Denotes that validation does not applies to a specific value.
   */
  public static final String GENERIC = "*";

  /**
   * Identifies the value value that is failing validation. A value of "*" denotes that validation
   * does not applies to a specific value.
   *
   * E.g. "name".
   */
  @JsonProperty
  private String id;

  /**
   * Description of failing validation.
   *
   * E.g. "Name cannot be null".
   */
  @JsonProperty
  private String message;

  public ValidationErrorXO() {
    this.id = GENERIC;
  }

  /**
   * Creates a validation error that does not applies to a specific value.
   *
   * @param message validation description
   */
  public ValidationErrorXO(final String message) {
    this(GENERIC, message);
  }

  /**
   * Creates a validation error for a specific value.
   *
   * @param id identifier of value failing validation.
   * @param message validation description
   */
  public ValidationErrorXO(final String id, final String message) {
    this.id = id == null ? GENERIC : id;
    this.message = message;
  }

  /**
   * @return identifier of value failing validation (never null). A value of "*" denotes that validation does
   *         not applies to a specific value.
   */
  public String getId() {
    return id;
  }

  /**
   * @param id of value failing validation
   */
  public void setId(final String id) {
    this.id = id == null ? GENERIC : id;
  }

  /**
   * @param id of value failing validation
   * @return itself, for fluent api usage
   */
  public ValidationErrorXO withId(final String id) {
    setId(id);
    return this;
  }

  /**
   * @return validation description
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message validation description
   */
  public void setMessage(final String message) {
    this.message = message;
  }

  /**
   * @param message validation description
   * @return itself, for fluent api usage
   */
  public ValidationErrorXO withMessage(final String message) {
    this.message = message;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        ", message='" + message + '\'' +
        '}';
  }
}
