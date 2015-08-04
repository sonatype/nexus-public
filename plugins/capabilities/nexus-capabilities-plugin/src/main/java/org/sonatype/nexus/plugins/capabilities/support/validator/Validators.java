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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Central access point for built-in {@link org.sonatype.nexus.plugins.capabilities.Validator}s.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class Validators
{

  private final CapabilityValidators capabilityValidators;

  private final LogicalValidators logicalValidators;

  private final RepositoryValidators repositoryValidators;

  private final ValueValidators valueValidators;

  @Inject
  Validators(final CapabilityValidators capabilityValidators,
             final LogicalValidators logicalValidators,
             final RepositoryValidators repositoryValidators,
             final ValueValidators valueValidators)
  {
    this.capabilityValidators = checkNotNull(capabilityValidators);
    this.logicalValidators = checkNotNull(logicalValidators);
    this.repositoryValidators = checkNotNull(repositoryValidators);
    this.valueValidators = checkNotNull(valueValidators);
  }

  /**
   * Access to capability specific validators.
   *
   * @return capability specific validators factory
   */
  public CapabilityValidators capability() {
    return capabilityValidators;
  }

  /**
   * Access to logical validators.
   *
   * @return logical validators factory
   */
  public LogicalValidators logical() {
    return logicalValidators;
  }

  /**
   * Access to repository specific validators.
   *
   * @return repository specific validators factory
   */
  public RepositoryValidators repository() {
    return repositoryValidators;
  }

  /**
   * Access to value specific validators.
   *
   * @return value specific validators factory
   * @since 2.7
   */
  public ValueValidators value() {
    return valueValidators;
  }


}
