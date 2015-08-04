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
package org.sonatype.security.ldap;

/**
 * Common constants shared across three LDAP related modules/plugins.
 *
 * @since 2.9
 */
public final class LdapConstants
{
  private LdapConstants() {
    // no instance
  }

  /**
   * The LDAP realm name. Shiro will consider this string as only and solely to perform authorization
   * using LDAP realm, so any principal should use this string when constructing principals.
   */
  public static final String REALM_NAME = "LdapAuthenticatingRealm";

  /**
   * String marking the source of users when looked up thru user manager that uses LDAP.
   */
  public static final String USER_SOURCE = "LDAP";
}
