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
package org.sonatype.nexus.security.realm;

import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * {@link RealmExists} validator.
 *
 * @since 3.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RealmExistsValidator
    extends ConstraintValidatorSupport<RealmExists, String>
{
  private final RealmSecurityManager realmSecurityManager;

  @Autowired
  public RealmExistsValidator(final RealmSecurityManager realmSecurityManager) {
    this.realmSecurityManager = checkNotNull(realmSecurityManager);
  }

  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    log.trace("Validating realm exists: {}", value);
    for (Realm realm : realmSecurityManager.getRealms()) {
      if (value.equals(realm.getName())) {
        return true;
      }
    }
    return false;
  }
}
