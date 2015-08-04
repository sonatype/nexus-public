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
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Validator} that ensures that capability repository property references a repository of specified kind(s).
 *
 * @since capabilities 2.0
 */
@Named
public class RepositoryTypeValidator
    extends ValidatorSupport
    implements Validator
{

  private final RepositoryRegistry repositoryRegistry;

  private final String propertyKey;

  private final Class<?> facet;

  @Inject
  RepositoryTypeValidator(final RepositoryRegistry repositoryRegistry,
                          final Provider<CapabilityDescriptorRegistry> capabilityDescriptorRegistryProvider,
                          final @Assisted CapabilityType type,
                          final @Assisted String propertyKey,
                          final @Assisted Class<?> facet)
  {
    super(capabilityDescriptorRegistryProvider, type);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.propertyKey = checkNotNull(propertyKey);
    this.facet = checkNotNull(facet);
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    String repositoryId = properties.get(propertyKey);
    if (repositoryId != null) {
      try {
        final Repository repository = repositoryRegistry.getRepository(repositoryId);
        if (!repository.getRepositoryKind().isFacetAvailable(facet)) {
          return new DefaultValidationResult().add(propertyKey, buildMessage(repository));
        }
      }
      catch (NoSuchRepositoryException ignore) {
        // ignore
      }
    }
    return ValidationResult.VALID;
  }

  @Override
  public String explainValid() {
    final StringBuilder message = new StringBuilder();
    message.append(propertyName(propertyKey)).append(" is a ").append(facetName()).append(" repository");
    return message.toString();
  }

  @Override
  public String explainInvalid() {
    final StringBuilder message = new StringBuilder();
    message.append(propertyName(propertyKey)).append(" is not a ").append(facetName())
        .append(" repository");
    return message.toString();

  }

  private String buildMessage(final Repository repository) {
    final StringBuilder message = new StringBuilder();
    message.append("Selected ").append(propertyName(propertyKey).toLowerCase())
        .append(" '").append(repository.getName())
        .append("' must be a ").append(facetName()).append(" repository");
    return message.toString();
  }

  private Object facetName() {
    return facet.getSimpleName().toLowerCase().replace("repository", "");
  }

}
