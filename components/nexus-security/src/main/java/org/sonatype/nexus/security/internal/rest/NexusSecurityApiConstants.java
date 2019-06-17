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

package org.sonatype.nexus.security.internal.rest;

/**
 * Holder for constants used by the REST API
 *
 * @since 3.17
 */
final class NexusSecurityApiConstants
{
  static final String USER_ID_DESCRIPTION = "The userid which is required for login. This value cannot be changed.";

  static final String FIRST_NAME_DESCRIPTION = "The first name of the user.";

  static final String LAST_NAME_DESCRIPTION = "The last name of the user.";

  static final String EMAIL_DESCRIPTION = "The email address associated with the user.";

  static final String STATUS_DESCRIPTION = "The user's status, e.g. active or disabled.";

  static final String SOURCE_DESCRIPTION =
      "The user source which is the origin of this user. This value cannot be changed.";

  static final String ROLES_DESCRIPTION = "The roles which the user has been assigned within Nexus.";

  static final String INVALID_PERMISSIONS = "Not privileged to perform operation.";

  static final String USER_NOT_FOUND = "User not found in the system.";

  static final String USER_OR_SOURCE_NOT_FOUND = "User or user source not found in the system.";

  private NexusSecurityApiConstants() {
    // pointless comment and constructor for sonar
  }
}
