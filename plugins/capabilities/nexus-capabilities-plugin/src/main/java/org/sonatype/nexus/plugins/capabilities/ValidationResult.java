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
package org.sonatype.nexus.plugins.capabilities;

import java.util.Set;

import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;

/**
 * Validates result.
 *
 * @since capabilities 2.0
 */
public interface ValidationResult
{

  /**
   * A validation result for the case when there are no validation failures.
   */
  static final ValidationResult VALID = new DefaultValidationResult();

  /**
   * Whether or not the validation was successful.
   *
   * @return true if there were no violations
   */
  boolean isValid();

  Set<Violation> violations();

  /**
   * Describes a violation.
   *
   * @since capabilities 2.0
   */
  interface Violation
  {

    /**
     * The key of property that is invalid or "*" when the violation applies to capability as a whole.
     *
     * @return key or "*"
     */
    String key();

    /**
     * A description of violation.
     *
     * @return violation description
     */
    String message();

  }

}
