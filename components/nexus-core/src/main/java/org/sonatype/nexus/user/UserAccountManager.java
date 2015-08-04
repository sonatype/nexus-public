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
package org.sonatype.nexus.user;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.security.authorization.AuthorizationException;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;

/**
 * Provides functionality to read and update basic user data.
 *
 * @since 2.1
 */
public interface UserAccountManager
{

  /**
   * Read the user account info
   */
  User readAccount(String userId)
      throws UserNotFoundException, AuthorizationException;

  /**
   * Update the account info, but do not change password
   */
  User updateAccount(User user)
      throws InvalidConfigurationException,
             UserNotFoundException,
             NoSuchUserManagerException,
             AuthorizationException;

}
