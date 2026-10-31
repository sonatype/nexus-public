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
package org.sonatype.nexus.rapture.internal.security;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for anonymous-aware behaviour in {@link SecurityComponent}. Anonymous subjects must have their permissions
 * computed by {@link SecurityComponent#getPermissions()} so the UI renders menus consistent with any elevated roles
 * assigned to the anonymous user (NEXUS-47113). {@link SecurityComponent#getUser()} continues to return {@code null}
 * for anonymous so the sign-in / sign-out UI flow keeps working — returning a populated user there would hide the
 * sign-in button and trap the UI in an anonymous session after logout.
 */
@ExtendWith(MockitoExtension.class)
class SecurityComponentTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private AuthTicketService authTickets;

  private SecurityComponent securityComponent;

  @BeforeEach
  void setUp() {
    securityComponent = new SecurityComponent(securitySystem, anonymousManager, authTickets);
  }

  @Test
  void getUserReturnsNullForAnonymousSubject() {
    // Anonymous must NOT be reported as the current user: the ExtJS UI uses getUser() == null as
    // its signal to show the Sign In button. Returning a populated UserXO here hides Sign In and
    // strands the UI in an anonymous session after logout (NEXUS-47113 follow-up).
    Subject anonymous = mockAnonymousSubject();
    when(securitySystem.getSubject()).thenReturn(anonymous);

    UserXO user = securityComponent.getUser();

    assertNull(user, "Anonymous must not appear as a logged-in user — the sign-in flow depends on this");
  }

  @Test
  void getUserReturnsNullForUnauthenticatedNonAnonymousSubject() {
    Subject subject = mockUnauthenticatedNonAnonymousSubject();
    when(securitySystem.getSubject()).thenReturn(subject);

    UserXO user = securityComponent.getUser();

    assertNull(user, "Unauthenticated non-anonymous subjects must not be returned as users");
  }

  @Test
  void getPermissionsReturnsComputedListForAnonymousSubject() {
    Subject anonymous = mockAnonymousSubject();
    when(securitySystem.getSubject()).thenReturn(anonymous);
    when(securitySystem.listPrivileges()).thenReturn(Collections.emptySet());

    List<PermissionXO> permissions = securityComponent.getPermissions();

    assertNotNull(permissions,
        "Anonymous subject must receive a computed permission list, not null (NEXUS-47113)");
  }

  @Test
  void getPermissionsReturnsNullForUnauthenticatedNonAnonymousSubject() {
    Subject subject = mockUnauthenticatedNonAnonymousSubject();
    when(securitySystem.getSubject()).thenReturn(subject);

    List<PermissionXO> permissions = securityComponent.getPermissions();

    assertNull(permissions);
  }

  private Subject mockAnonymousSubject() {
    Subject subject = mock(Subject.class);
    AnonymousPrincipalCollection principals = new AnonymousPrincipalCollection("anonymous", "NexusAuthorizingRealm");
    lenient().when(subject.getPrincipals()).thenReturn(principals);
    lenient().when(subject.getPrincipal()).thenReturn("anonymous");
    lenient().when(subject.isAuthenticated()).thenReturn(false);
    lenient().when(subject.isRemembered()).thenReturn(false);
    lenient().when(subject.isPermitted(anyList())).thenReturn(new boolean[0]);
    return subject;
  }

  private Subject mockUnauthenticatedNonAnonymousSubject() {
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipals()).thenReturn(new SimplePrincipalCollection("someone", "realm"));
    lenient().when(subject.isAuthenticated()).thenReturn(false);
    lenient().when(subject.isRemembered()).thenReturn(false);
    return subject;
  }
}
