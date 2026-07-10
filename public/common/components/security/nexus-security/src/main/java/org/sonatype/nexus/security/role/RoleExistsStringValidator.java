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
package org.sonatype.nexus.security.role;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class RoleExistsStringValidator
    extends ConstraintValidatorSupport<RoleExistsString, String>
{
  private final AuthorizationManager authorizationManager;

  @Autowired
  public RoleExistsStringValidator(final AuthorizationManager authorizationManager) {
    this.authorizationManager = authorizationManager;
  }

  @Override
  public boolean isValid(final String role, final ConstraintValidatorContext constraintValidatorContext) {
    try {
      authorizationManager.getRole(role);
      return true;
    }
    catch (NoSuchRoleException e) {
      return false;
    }
  }
}
