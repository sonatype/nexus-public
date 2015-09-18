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

public interface FormField<T>
{
  public static final boolean MANDATORY = true;

  public static final boolean OPTIONAL = false;

  /**
   * Get the type of this form field
   */
  String getType();

  /**
   * Get the label of this form field
   */
  String getLabel();

  /**
   * Get the ID of this form field
   */
  String getId();

  /**
   * get the required flag of this field
   */
  boolean isRequired();

  /**
   * Get the help text of this field
   */
  String getHelpText();

  /**
   * Get the regex validation of this field
   */
  String getRegexValidation();

  /**
   * @return initial value of this field (can be null - no initial value)
   * @since 2.3
   */
  T getInitialValue();
}
