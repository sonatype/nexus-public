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
package org.sonatype.nexus.validation.constraint;

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;

/**
 * {@link PortNumber} validator.
 *
 * @since 3.0
 */
public class PortNumberValidator
  extends ConstraintValidatorSupport<PortNumber,Integer>
{
  private int min;

  private int max;

  @Override
  public void initialize(final PortNumber annotation) {
    min = annotation.min();
    max = annotation.max();
  }

  @Override
  public boolean isValid(final Integer value, final ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    return value >= min && value <= max;
  }
}
