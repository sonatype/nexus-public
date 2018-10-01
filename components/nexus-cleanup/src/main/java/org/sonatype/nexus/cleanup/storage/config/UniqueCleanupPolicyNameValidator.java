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
package org.sonatype.nexus.cleanup.storage.config;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.NONE_POLICY;

/**
 * Ensure that Cleanup Policy names are unique case-insensitively.
 *
 * @since 3.14
 */
@Named
public class UniqueCleanupPolicyNameValidator
    extends ConstraintValidatorSupport<UniqueCleanupPolicyName, String>
{
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Inject
  public UniqueCleanupPolicyNameValidator(final CleanupPolicyStorage cleanupPolicyStorage) {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return !NONE_POLICY.getName().equalsIgnoreCase(value) && !cleanupPolicyStorage.exists(value);
  }
}
