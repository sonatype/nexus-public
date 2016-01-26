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
package org.sonatype.nexus.security;

/**
 * Holder for Nexus role related constants.
 *
 * @since 3.0
 */
public final class Roles
{
  /**
   * Role ID used for NX Administrator role, used in some places to detect is user admin or not.
   */
  public static final String ADMIN_ROLE_ID = "nx-admin";

  /**
   * Role ID used for NX Anonymous rule, used in some places to detect is used anonymous or not.
   */
  public static final String ANONYMOUS_ROLE_ID = "nx-anonymous";

  private Roles() {
    // nop
  }
}
