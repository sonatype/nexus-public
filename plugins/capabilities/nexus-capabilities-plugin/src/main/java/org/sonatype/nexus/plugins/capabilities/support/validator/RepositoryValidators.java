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
package org.sonatype.nexus.plugins.capabilities.support.validator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.internal.validator.ValidatorFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory of {@link Validator}s related to repositories.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class RepositoryValidators
{

  private final ValidatorFactory validatorFactory;

  @Inject
  public RepositoryValidators(final ValidatorFactory validatorFactory) {
    this.validatorFactory = checkNotNull(validatorFactory);
  }

  /**
   * Creates a new validator that checks that a repository referenced by specified property key is of specified type.
   *
   * @param type        capability type
   * @param propertyKey key of property that contains teh repository id
   * @param facet       type of repository
   * @return created validator
   */
  public Validator repositoryOfType(final CapabilityType type,
                                    final String propertyKey,
                                    final Class<?> facet)
  {
    return validatorFactory.repositoryOfType(type, propertyKey, facet);
  }

  /**
   * Creates a new validator that checks that a repository referenced by specified property key exists.
   *
   * @param type        capability type
   * @param propertyKey key of property that contains the repository id
   * @return created validator
   * @since capabilities 2.3
   */
  public Validator repositoryExists(final CapabilityType type,
                                    final String propertyKey)
  {
    return validatorFactory.repositoryExists(type, propertyKey);
  }

}
