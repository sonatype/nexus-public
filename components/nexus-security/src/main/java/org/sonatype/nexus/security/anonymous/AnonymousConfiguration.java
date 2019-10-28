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
package org.sonatype.nexus.security.anonymous;

import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;

/**
 * Anonymous configuration.
 *
 * @since 3.0
 */
public interface AnonymousConfiguration //NOSONAR
    extends Cloneable
{
  /**
   * @since 3.1
   */
  String DEFAULT_USER_ID = "anonymous";

  /**
   * @since 3.1
   */
  String DEFAULT_REALM_NAME = AuthorizingRealmImpl.NAME;

  /**
   * Obtain a copy of this configuration.
   */
  AnonymousConfiguration copy();

  /**
   * Get the realm in which the UserID associated with the configuration is located.
   */
  String getRealmName();

  /**
   * Get the UserID which is used as the template for permissions.
   */
  String getUserId();

  /**
   * Indicates whether anonymous access is enabled in this configuration.
   *
   * @return
   */
  boolean isEnabled();

  /**
   * Set whether anonymous access is enabled in this configuration.
   */
  void setEnabled(final boolean enabled);

  /**
   * Set the realm in which the UserID associated with the configuration is located.
   */
  void setRealmName(final String realmName);

  /**
   * Set the UserID which is used as the template for permissions.
   */
  void setUserId(final String userId);
}
