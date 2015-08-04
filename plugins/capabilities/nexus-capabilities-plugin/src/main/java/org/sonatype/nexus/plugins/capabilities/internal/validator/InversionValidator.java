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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Logical NOT ona a {@link Validator}.
 *
 * @since capabilities 2.0
 */
public class InversionValidator
    implements Validator
{

  private final Validator validator;

  @Inject
  public InversionValidator(final Validator validator) {
    this.validator = checkNotNull(validator);
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    final ValidationResult validationResult = validator.validate(properties);
    if (!validationResult.isValid()) {
      return ValidationResult.VALID;
    }
    return new DefaultValidationResult().add(explainInvalid());
  }

  @Override
  public String explainValid() {
    return validator.explainInvalid();
  }

  @Override
  public String explainInvalid() {
    return validator.explainValid();
  }

}
