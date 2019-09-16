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
public final class NexusSecurityApiConstants
{
  static final String USER_ID_DESCRIPTION = "The userid which is required for login. This value cannot be changed.";

  static final String FIRST_NAME_DESCRIPTION = "The first name of the user.";

  static final String LAST_NAME_DESCRIPTION = "The last name of the user.";

  static final String EMAIL_DESCRIPTION = "The email address associated with the user.";

  static final String STATUS_DESCRIPTION = "The user's status, e.g. active or disabled.";

  static final String SOURCE_DESCRIPTION =
      "The user source which is the origin of this user. This value cannot be changed.";

  static final String ROLES_DESCRIPTION = "The roles which the user has been assigned within Nexus.";

  public static final String INVALID_PERMISSIONS = "The user does not have permission to perform the operation.";

  static final String USER_NOT_FOUND = "User not found in the system.";

  static final String USER_OR_SOURCE_NOT_FOUND = "User or user source not found in the system.";

  public static final String PRIVILEGE_MISCONFIGURED = "Privilege object not configured properly.";

  public static final String PRIVILEGE_NOT_FOUND = "Privilege not found in the system.";

  public static final String PRIVILEGE_READ_ONLY = "The privilege is internal and may not be altered.";

  public static final String PRIVILEGE_TYPE_DESCRIPTION = "The type of privilege, each type covers different portions of the system. External values supplied to this will be ignored by the system.";

  public static final String PRIVILEGE_NAME_DESCRIPTION = "The name of the privilege.  This value cannot be changed.";

  public static final String PRIVILEGE_READONLY_DESCRIPTION = "Indicates whether the privilege can be changed. External values supplied to this will be ignored by the system.";

  public static final String PRIVILEGE_DOMAIN_DESCRIPTION = "The domain (i.e. 'blobstores', 'capabilities' or even '*' for all) that this privilege is granting access to."
      + "  Note that creating new privileges with a domain is only necessary when using plugins that define their own domain(s).";

  public static final String PRIVILEGE_REPOSITORY_FORMAT_DESCRIPTION = "The repository format (i.e 'nuget', 'npm') this privilege will grant access to (or * for all).";

  public static final String PRIVILEGE_REPOSITORY_DESCRIPTION = "The name of the repository this privilege will grant access to (or * for all).";

  public static final String PRIVILEGE_CONTENT_SELECTOR_DESCRIPTION = "The name of a content selector that will be used to grant access to content via this privilege.";

  public static final String PRIVILEGE_SCRIPT_DESCRIPTION = "The name of a script to give access to.";

  public static final String PRIVILEGE_PATTERN_DESCRIPTION = "A colon separated list of parts that create a permission string.";

  public static final String PRIVILEGE_ACTION_DESCRIPTION = "A collection of actions to associate with the privilege, using BREAD syntax (browse,read,edit,add,delete,all) as well as 'run' for script privileges.";

  public static final String ROLE_SOURCE_DESCRIPTION = "The user source which is the origin of this role.";

  public static final String ROLE_ID_DESCRIPTION = "The id of the role.";

  public static final String ROLE_NAME_DESCRIPTION = "The name of the role.";

  public static final String ROLE_DESCRIPTION_DESCRIPTION = "The description of this role.";

  public static final String ROLE_PRIVILEGES_DESCRIPTION = "The list of privileges assigned to this role.";

  public static final String ROLE_ROLES_DESCRIPTION = "The list of roles assigned to this role.";

  private NexusSecurityApiConstants() {
    // pointless comment and constructor for sonar
  }
}
