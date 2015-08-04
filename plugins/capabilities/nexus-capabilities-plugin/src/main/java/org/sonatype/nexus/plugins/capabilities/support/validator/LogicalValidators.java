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
package org.sonatype.nexus.plugins.capabilities.support.validator;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.internal.validator.ConjunctionValidator;
import org.sonatype.nexus.plugins.capabilities.internal.validator.DisjunctionValidator;
import org.sonatype.nexus.plugins.capabilities.internal.validator.InversionValidator;

/**
 * Factory of logical {@link Validator}s combinations.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class LogicalValidators
{

  /**
   * Creates a new validator that is satisfied when all validators are not failing(logical AND).
   *
   * @param validators to be AND-ed
   * @return created validator
   */
  public Validator and(final Validator... validators) {
    return new ConjunctionValidator(validators);
  }

  /**
   * Creates a new validator that is satisfied when at least one validation is not failing (logical OR).
   *
   * @param validators to be OR-ed
   * @return created validator
   */
  public Validator or(final Validator... validators) {
    return new DisjunctionValidator(validators);
  }

  /**
   * Creates a new validator that is satisfied when another validator is not failing (logical NOT).
   *
   * @param validator negated validator
   * @return created validator
   */
  public Validator not(final Validator validator) {
    return new InversionValidator(validator);
  }

}
