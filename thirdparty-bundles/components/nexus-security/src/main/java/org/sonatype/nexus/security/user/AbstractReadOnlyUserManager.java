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

/**
 * Read-only {@link UserManager}, which just throws exceptions for all the write methods.
 *
 * Any call to theses methods should be guarded by {@code #supportsWrite}.
 */
public abstract class AbstractReadOnlyUserManager
    extends AbstractUserManager
{
  /**
   * @return Always {@code false}
   */
  @Override
  public boolean supportsWrite() {
    return false;
  }

  @Override
  public User addUser(final User user, final String password) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void changePassword(final String userId, final String newPassword) throws UserNotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteUser(final String userId) throws UserNotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override
  public User updateUser(final User user) throws UserNotFoundException {
    throw new UnsupportedOperationException();
  }
}
