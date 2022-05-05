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

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;
import org.sonatype.nexus.security.internal.DefaultRealmConstants;
import org.sonatype.nexus.security.user.UserManager;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import static java.util.stream.Collectors.toList;

/**
 * Anonymous helpers.
 *
 * @since 3.0
 */
public class AnonymousHelper
{
  private AnonymousHelper() {
    // empty
  }

  /**
   * Check given given subject is anonymous.
   */
  public static boolean isAnonymous(@Nullable final Subject subject) {
    return subject != null && subject.getPrincipals() instanceof AnonymousPrincipalCollection;
  }

  /**
   * Check given principals represent anonymous.
   *
   * @since 3.22
   */
  public static boolean isAnonymous(@Nullable final PrincipalCollection principals) {
    return principals instanceof AnonymousPrincipalCollection;
  }

  /**
   * Get all authentication realms.
   */
  public static List<String> getAuthenticationRealms(final List<UserManager> userManagers) {
    return userManagers.stream()
        .map(UserManager::getAuthenticationRealmName)
        .filter(Objects::nonNull)
        .map(realm -> realm.equals(DefaultRealmConstants.DEFAULT_REALM_NAME) ?
            AuthorizingRealmImpl.NAME : realm)
        .collect(toList());
  }
}
