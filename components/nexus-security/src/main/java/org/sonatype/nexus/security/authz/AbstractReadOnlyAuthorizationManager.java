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
package org.sonatype.nexus.security.authz;

import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;

/**
 * Read-only {@link AuthorizationManager}, which just throws exceptions for all the write methods.
 *
 * Any call to theses methods should be guarded by {@code #supportsWrite}.
 */
public abstract class AbstractReadOnlyAuthorizationManager
    implements AuthorizationManager
{
  /**
   * @return Always {@code false}
   */
  @Override
  public boolean supportsWrite() {
    return false;
  }

  @Override
  public Privilege addPrivilege(final Privilege privilege) {
    throw unsupported();
  }

  @Override
  public Role addRole(final Role role) {
    throw unsupported();
  }

  @Override
  public void deletePrivilege(final String privilegeId) throws NoSuchPrivilegeException {
    throw unsupported();
  }

  @Override
  public void deleteRole(final String roleId) throws NoSuchRoleException {
    throw unsupported();
  }

  @Override
  public Privilege updatePrivilege(final Privilege privilege) throws NoSuchPrivilegeException {
    throw unsupported();
  }

  @Override
  public Role updateRole(final Role role) throws NoSuchRoleException {
    throw unsupported();
  }

  private IllegalStateException unsupported() {
    // TODO: Should probably use UnsupportedOperationException
    throw new IllegalStateException("AuthorizationManager: '" + getSource() + "' does not support write operations.");
  }
}
