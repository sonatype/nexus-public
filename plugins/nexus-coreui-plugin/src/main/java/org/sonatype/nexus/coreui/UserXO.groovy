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
package org.sonatype.nexus.coreui

import javax.validation.constraints.NotNull

import org.sonatype.nexus.security.role.RolesExist
import org.sonatype.nexus.security.user.UniqueUserId
import org.sonatype.nexus.security.user.UserStatus
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import groovy.transform.ToString
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.NotEmpty

/**
 * User exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class UserXO
{
  @NotBlank
  @UniqueUserId(groups = Create)
  String userId

  @NotBlank(groups = Update)
  String version

  // Null on create
  String realm

  @NotBlank
  String firstName

  @NotBlank
  String lastName

  @NotBlank
  @Email
  String email

  @NotNull
  UserStatus status

  @NotBlank(groups = Create.class)
  String password

  @NotEmpty
  @RolesExist(groups = [Create, Update])
  Set<String> roles

  Boolean external

  // FIXME: Sort out what this is used for
  Set<String> externalRoles
}
