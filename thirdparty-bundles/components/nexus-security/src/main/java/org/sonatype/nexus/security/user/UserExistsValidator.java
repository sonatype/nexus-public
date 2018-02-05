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
package org.sonatype.nexus.security.user;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link UserExists} validator.
 *
 * @since 3.0
 */
@Named
public class UserExistsValidator
    extends ConstraintValidatorSupport<UserExists, String>
{
  private final SecuritySystem securitySystem;

  @Inject
  public UserExistsValidator(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    log.trace("Validating user exists: {}", value);
    try {
      User user = securitySystem.getUser(value);
      return true;
    }
    catch (UserNotFoundException e) {
      return false;
    }
  }
}
