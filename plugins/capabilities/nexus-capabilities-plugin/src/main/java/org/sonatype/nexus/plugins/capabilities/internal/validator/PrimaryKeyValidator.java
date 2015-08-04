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

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.capability.support.ValidatorSupport;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.CapabilityReferenceFilterBuilder;
import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;

import com.google.common.base.Predicate;
import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.capabilities.support.CapabilityReferenceFilterBuilder.capabilities;

/**
 * A {@link Validator} that ensures that only one capability of specified type and set of properties can be created.
 *
 * @since capabilities 2.0
 */
@Named
public class PrimaryKeyValidator
    extends ValidatorSupport
    implements Validator
{

  private final CapabilityRegistry capabilityRegistry;

  private final CapabilityIdentity excludeId;

  private final String[] propertyKeys;

  @Inject
  PrimaryKeyValidator(final CapabilityRegistry capabilityRegistry,
                      final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                      final @Assisted CapabilityType type,
                      final @Assisted String... propertyKeys)
  {
    super(capabilityDescriptorRegistryProvider, type);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.excludeId = null;
    this.propertyKeys = propertyKeys;
  }

  PrimaryKeyValidator(final CapabilityRegistry capabilityRegistry,
                      final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                      final CapabilityIdentity excludeId,
                      final CapabilityType type,
                      final String... propertyKeys)
  {
    super(capabilityDescriptorRegistryProvider, type);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.excludeId = checkNotNull(excludeId);
    this.propertyKeys = propertyKeys;
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    final Collection<? extends CapabilityReference> references = capabilityRegistry.get(
        buildFilter(properties)
    );
    if (references == null
        || references.isEmpty()
        || (references.size() == 1 && references.iterator().next().context().id().equals(excludeId))) {
      return ValidationResult.VALID;
    }
    return new DefaultValidationResult().add(buildMessage(properties));
  }

  @Override
  public String explainValid() {
    final StringBuilder message = new StringBuilder()
        .append("Only one capability with type '").append(typeName()).append("'");

    if (propertyKeys != null) {
      for (final String key : propertyKeys) {
        message.append(", same ").append(propertyName(key).toLowerCase());
      }
    }
    message.append(" exists");

    return message.toString();
  }

  @Override
  public String explainInvalid() {
    final StringBuilder message = new StringBuilder()
        .append("More then one capability with type '").append(typeName()).append("'");

    if (propertyKeys != null) {
      for (final String key : propertyKeys) {
        message.append(", same ").append(propertyName(key).toLowerCase());
      }
    }
    message.append(" exists");

    return message.toString();
  }

  private String buildMessage(final Map<String, String> properties) {
    final StringBuilder message = new StringBuilder()
        .append("Only one capability of type '").append(typeName()).append("'");

    if (properties != null) {
      for (final String key : propertyKeys) {
        message.append(", ").append(propertyName(key).toLowerCase()).append(" '")
            .append(properties.get(key)).append("'");
      }
    }
    message.append(" can be created");

    return message.toString();
  }

  private Predicate<CapabilityReference> buildFilter(final Map<String, String> properties) {
    final CapabilityReferenceFilterBuilder.CapabilityReferenceFilter filter = capabilities().withType(
        capabilityType()
    );
    if (propertyKeys != null) {
      for (final String key : propertyKeys) {
        filter.withProperty(key, properties.get(key));
      }
    }
    return filter;
  }

}
