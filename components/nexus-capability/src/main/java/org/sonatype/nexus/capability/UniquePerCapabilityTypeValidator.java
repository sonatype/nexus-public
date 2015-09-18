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
package org.sonatype.nexus.capability;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.capabilities;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * {@link UniquePerCapabilityType} validator.
 *
 * @since 3.0
 */
@Named
public class UniquePerCapabilityTypeValidator
    extends ConstraintValidatorSupport<UniquePerCapabilityType, Object>
{
  private final CapabilityRegistry capabilityRegistry;

  private CapabilityType type;

  @Inject
  public UniquePerCapabilityTypeValidator(final CapabilityRegistry capabilityRegistry)
  {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  @Override
  public void initialize(final UniquePerCapabilityType constraintAnnotation) {
    type = capabilityType(constraintAnnotation.value());
  }

  @Override
  public boolean isValid(final Object value, final ConstraintValidatorContext context) {
    if (value != null) {
      log.trace("Validating only one capability of type {} can be created", value);
      CapabilityReferenceFilter filter = capabilities().withType(type);
      Collection<? extends CapabilityReference> references = capabilityRegistry.get(filter);
      return references.isEmpty();
    }
    return true;
  }
}
