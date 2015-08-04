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
package org.sonatype.security.usermanagement;

import org.sonatype.configuration.validation.InvalidConfigurationException;

/**
 * An abstract UserManager, that just throws exceptions for all the write methods. Any call to theses methods should be
 * checked by the <code>supportsWrite()</code> method, so this should never be called.
 *
 * @author Brian Demers
 */
public abstract class AbstractReadOnlyUserManager
    extends AbstractUserManager
{

  public boolean supportsWrite() {
    return false;
  }

  public User addUser(User user, String password)
      throws InvalidConfigurationException
  {
    this.throwException();
    return null;
  }

  public void changePassword(String userId, String newPassword)
      throws UserNotFoundException
  {
    this.throwException();
  }

  public void deleteUser(String userId)
      throws UserNotFoundException
  {
    this.throwException();
  }

  public User updateUser(User user)
      throws UserNotFoundException, InvalidConfigurationException
  {
    this.throwException();
    return null;
  }

  private void throwException() {
    throw new IllegalStateException("UserManager: '" + this.getSource() + "' does not support write operations.");
  }

}
