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
package org.sonatype.nexus.plugins.capabilities.internal.validator;

import java.util.Map;

import javax.inject.Inject;

import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Logical OR between {@link Validator}s.
 *
 * @since capabilities 2.0
 */
public class DisjunctionValidator
    implements Validator
{

  private final Validator[] validators;

  @Inject
  public DisjunctionValidator(final Validator... validators) {
    this.validators = checkNotNull(validators);
    checkArgument(validators.length > 0, "There must be at least one validator");
    for (final Validator validator : validators) {
      checkNotNull(validator);
    }
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    final DefaultValidationResult failed = new DefaultValidationResult();
    for (final Validator validator : validators) {
      final ValidationResult validationResult = validator.validate(properties);
      if (validationResult.isValid()) {
        return ValidationResult.VALID;
      }
      failed.add(validationResult.violations());
    }
    return failed;
  }

  @Override
  public String explainValid() {
    final StringBuilder sb = new StringBuilder();
    sb.append("One of following is valid: ");
    for (final Validator validator : validators) {
      if (sb.length() > 0) {
        sb.append(" OR ");
      }
      sb.append(validator.explainValid());
    }
    return sb.toString();
  }

  @Override
  public String explainInvalid() {
    final StringBuilder sb = new StringBuilder();
    sb.append("All of following is invalid: ");
    for (final Validator validator : validators) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      sb.append(validator.explainValid());
    }
    return sb.toString();
  }

}
