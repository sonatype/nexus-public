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
import javax.inject.Provider;

import org.sonatype.nexus.capability.support.ValidatorSupport;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

import com.google.inject.assistedinject.Assisted;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Validates capability configuration field is valid URI.
 *
 * @since 2.7
 */
public class IntegerValidator
    extends ValidatorSupport
    implements Validator
{
  private static interface Messages
      extends MessageBundle
  {
    @DefaultMessage("%s is an integer value")
    String valid(String fieldLabel);

    @DefaultMessage("%s is not an integer value")
    String invalid(String fieldLabel);

    @DefaultMessage("%s is not an integer value (%s)")
    String invalidReason(String fieldLabel, String failure);
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final String key;

  private final String label;

  @Inject
  public IntegerValidator(final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                          final @Assisted CapabilityType type,
                          final @Assisted String key)
  {
    super(capabilityDescriptorRegistryProvider, type);
    this.key = checkNotNull(key);
    this.label = propertyName(key);
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    String value = properties.get(key);
    if (StringUtils.isNotEmpty(value)) {
      try {
        Integer.valueOf(value);
      }
      catch (NumberFormatException e) {
        return new DefaultValidationResult().add(key, messages.invalidReason(label, e.getMessage()));
      }
    }
    return ValidationResult.VALID;
  }

  @Override
  public String explainValid() {
    return messages.valid(label);
  }

  @Override
  public String explainInvalid() {
    return messages.invalid(label);
  }

}
