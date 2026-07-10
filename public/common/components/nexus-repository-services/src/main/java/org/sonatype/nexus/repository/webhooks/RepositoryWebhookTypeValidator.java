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
package org.sonatype.nexus.repository.webhooks;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class RepositoryWebhookTypeValidator
    extends ConstraintValidatorSupport<RepositoryWebhookType, List<String>>
{
  private final List<RepositoryWebhook> repositoryWebhooks;

  @Autowired
  public RepositoryWebhookTypeValidator(final List<RepositoryWebhook> repositoryWebhooks) {
    this.repositoryWebhooks = repositoryWebhooks;
  }

  @Override
  public boolean isValid(final List<String> names, final ConstraintValidatorContext constraintValidatorContext) {
    if (names == null || names.isEmpty()) {
      return true; // empty list is valid
    }

    Set<String> webhookNames =
        repositoryWebhooks.stream().map(RepositoryWebhook::getName).collect(Collectors.toUnmodifiableSet());
    // Check if all names in the provided list are valid webhook names
    return webhookNames.containsAll(names);
  }
}
