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
package org.sonatype.nexus.repository.rest.api.validation;

import java.util.Arrays;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistribution;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

/**
 * {@link NotEmptyOnDistribution} validator.
 * <p>
 * The distribution is fixed by packaging for the life of the JVM, so it is resolved once when the constraint is
 * initialized rather than on every validation. Tests driving {@link NexusDistribution#setTestOverride} must therefore
 * set the override before the validator is constructed.
 */
public class NotEmptyOnDistributionValidator
    extends ConstraintValidatorSupport<NotEmptyOnDistribution, CharSequence>
{
  private boolean required;

  @Override
  public void initialize(final NotEmptyOnDistribution annotation) {
    NexusDistributionType distribution = NexusDistribution.detect(getClass().getClassLoader());
    // An un-evaluable gate must not silently relax a constraint, so enforce it.
    required = distribution == null || Arrays.asList(annotation.value()).contains(distribution);
  }

  @Override
  public boolean isValid(final CharSequence value, final ConstraintValidatorContext context) {
    if (!required) {
      return true;
    }
    return value != null && !value.toString().trim().isEmpty();
  }
}
