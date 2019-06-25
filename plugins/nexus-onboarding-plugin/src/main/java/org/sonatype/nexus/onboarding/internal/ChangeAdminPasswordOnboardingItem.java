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
package org.sonatype.nexus.onboarding.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.internal.UserManagerImpl;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.17
 */
@Named
@Singleton
public class ChangeAdminPasswordOnboardingItem
  extends ComponentSupport
  implements OnboardingItem
{
  private final SecuritySystem securitySystem;

  @Inject
  public ChangeAdminPasswordOnboardingItem(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  @Override
  public String getType() {
    return "ChangeAdminPassword";
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public boolean applies() {
    try {
      User user = securitySystem.getUser("admin", UserManagerImpl.DEFAULT_SOURCE);
      return UserStatus.changepassword.equals(user.getStatus());
    }
    catch (UserNotFoundException e) {
      log.trace("admin user not found in system, marking onboarding item as not applicable.", e);
    }
    catch (NoSuchUserManagerException e) {
      log.trace("default UserManager not found in system, marking onboarding item as not applicable.", e);
    }

    return false;
  }
}
