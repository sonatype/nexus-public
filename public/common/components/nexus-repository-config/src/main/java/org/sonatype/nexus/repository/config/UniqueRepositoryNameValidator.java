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
package org.sonatype.nexus.repository.config;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Ensure that Repository names are unique case-insensitively.
 * Also checks for soft-deleted repositories occupying the name.
 *
 * @since 3.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UniqueRepositoryNameValidator
    extends ConstraintValidatorSupport<UniqueRepositoryName, String>
{
  private final RepositoryManager repositoryManager;

  private final ConfigurationStore configurationStore;

  @Autowired
  public UniqueRepositoryNameValidator(
      final RepositoryManager repositoryManager,
      final ConfigurationStore configurationStore)
  {
    this.repositoryManager = requireNonNull(repositoryManager);
    this.configurationStore = requireNonNull(configurationStore);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (repositoryManager.exists(value)) {
      return false;
    }
    // Repository names are unique case-insensitively, so a case-variant of a soft-deleted repository must
    // also be rejected — otherwise the soft-deleted repository could never be cleanly restored.
    if (configurationStore.existsDeletedByNameCaseInsensitive(value)) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "A deleted repository with this name exists. Restore or permanently remove it from Trash.")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
