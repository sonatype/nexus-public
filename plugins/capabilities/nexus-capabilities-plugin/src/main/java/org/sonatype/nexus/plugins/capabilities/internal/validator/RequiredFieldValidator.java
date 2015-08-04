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
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.capability.support.ValidatorSupport;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Validator} that ensures that a required field corresponding property is not null or empty.
 *
 * @since capabilities 2.0
 */
@Named
public class RequiredFieldValidator
    extends ValidatorSupport
    implements Validator
{

  private final String key;

  private final String label;

  @Inject
  RequiredFieldValidator(final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                         final @Assisted CapabilityType type,
                         final @Assisted String key)
  {
    super(capabilityDescriptorRegistryProvider, type);
    this.key = checkNotNull(key);
    label = propertyName(key);
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    if (properties != null) {
      final String value = properties.get(key);
      if (value == null || value.trim().length() == 0) {
        return new DefaultValidationResult().add(key, label + " is required");
      }
    }
    return ValidationResult.VALID;
  }

  @Override
  public String explainValid() {
    return label + " is not null or empty";
  }

  @Override
  public String explainInvalid() {
    return label + " is null or empty";
  }

}
