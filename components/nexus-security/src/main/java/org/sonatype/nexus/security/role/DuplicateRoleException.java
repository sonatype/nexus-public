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

/**
 * @since 3.19
 */
public class DuplicateRoleException
    extends RoleException
{
  private static final String ERROR_TEXT = "Role %s already exists.";

  public DuplicateRoleException(final String roleId, Throwable cause) {
    super(ERROR_TEXT, roleId, cause);
  }

  public DuplicateRoleException(final String role) {
    this(role, null);
  }
}
