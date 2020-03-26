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
package org.sonatype.nexus.cleanup.storage.config;

import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.cleanup.storage.config.RegexCriteriaValidator.InvalidExpressionException;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static java.util.Objects.nonNull;
import static org.sonatype.nexus.cleanup.storage.config.RegexCriteriaValidator.validate;

/**
 *
 * @since 3.19
 */
@Named
public class CleanupPolicyAssetNamePatternValidator
    extends ConstraintValidatorSupport<CleanupPolicyAssetNamePattern, String>
{
  public CleanupPolicyAssetNamePatternValidator() {
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    // we don't require it
    if (nonNull(value)) {

      try {
        // but if it is provided we require it to be valid
        validate(value);
      }
      catch (InvalidExpressionException e) { // NOSONAR
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(getEscapeHelper().stripJavaEl(e.getMessage())).addConstraintViolation();
        return false;
      }
    }

    return true;
  }
}
