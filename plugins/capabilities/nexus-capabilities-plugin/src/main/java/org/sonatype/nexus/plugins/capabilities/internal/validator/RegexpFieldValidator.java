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
import java.util.regex.Pattern;

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
 * A {@link Validator} that ensures that a required field corresponding property matches a specified regex pattern.
 *
 * @since capabilities 2.0
 */
@Named
public class RegexpFieldValidator
    extends ValidatorSupport
    implements Validator
{

  private final String key;

  private final Pattern pattern;

  private final String label;

  @Inject
  RegexpFieldValidator(final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                       final @Assisted CapabilityType type,
                       final @Assisted("key") String key,
                       final @Assisted("regexp") String regexp)
  {
    super(capabilityDescriptorRegistryProvider, type);
    this.key = checkNotNull(key);
    this.pattern = Pattern.compile(checkNotNull(regexp));
    label = propertyName(key);
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    if (properties != null) {
      final String value = properties.get(key);
      if (value != null && !pattern.matcher(value).matches()) {
        return new DefaultValidationResult().add(key, label + " does not match '" + pattern.pattern() + "'");
      }
    }
    return ValidationResult.VALID;
  }

  @Override
  public String explainValid() {
    return label + " matches '" + pattern.pattern() + "'";
  }

  @Override
  public String explainInvalid() {
    return label + " does not match '" + pattern.pattern() + "'";
  }

}
