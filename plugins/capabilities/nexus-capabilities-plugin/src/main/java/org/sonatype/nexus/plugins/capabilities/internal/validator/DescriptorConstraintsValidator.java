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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.capability.support.ValidatorSupport;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.Validators;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Validator} that ensures that constraints expressed by descriptor fields are satisfied.
 *
 * @since capabilities 2.0
 */
@Named
public class DescriptorConstraintsValidator
    extends ValidatorSupport
    implements Validator
{

  private final Validator validator;

  @Inject
  DescriptorConstraintsValidator(final Validators validators,
                                 final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                                 final @Assisted CapabilityType type)
  {
    super(capabilityDescriptorRegistryProvider, type);
    checkNotNull(validators);
    Validator descriptorValidator = validators.capability().alwaysValid();
    final List<FormField> formFields = capabilityDescriptor().formFields();
    if (formFields != null) {
      final List<Validator> fieldValidators = Lists.newArrayList();
      for (final FormField formField : formFields) {
        if (formField.isRequired()) {
          fieldValidators.add(validators.capability().required(type, formField.getId()));
        }
        final String regexp = formField.getRegexValidation();
        if (regexp != null && regexp.trim().length() > 0) {
          fieldValidators.add(validators.capability().matches(type, formField.getId(), regexp));
        }
      }
      if (!fieldValidators.isEmpty()) {
        descriptorValidator = validators.logical().and(
            fieldValidators.toArray(new Validator[fieldValidators.size()])
        );
      }
    }
    validator = descriptorValidator;
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    return validator.validate(properties);
  }

  @Override
  public String explainValid() {
    return validator.explainValid();
  }

  @Override
  public String explainInvalid() {
    return validator.explainInvalid();
  }

}
