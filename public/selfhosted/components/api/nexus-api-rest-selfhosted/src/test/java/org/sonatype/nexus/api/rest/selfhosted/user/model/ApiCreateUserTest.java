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
package org.sonatype.nexus.api.rest.selfhosted.user.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.internal.rest.ApiUserStatus;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApiCreateUserTest
    extends TestSupport
{
  @Test
  public void testToUser_withNullRoles() {
    ApiCreateUser apiCreateUser = new ApiCreateUser(
        "testuser",
        "Test",
        "User",
        "test@example.com",
        "password123",
        ApiUserStatus.active,
        null);

    User user = apiCreateUser.toUser();

    assertThat(user, is(notNullValue()));
    assertThat(user.getUserId(), is("testuser"));
    assertThat(user.getFirstName(), is("Test"));
    assertThat(user.getLastName(), is("User"));
    assertThat(user.getEmailAddress(), is("test@example.com"));
    assertThat(user.getSource(), is(UserManager.DEFAULT_SOURCE));
    assertThat(user.getRoles(), is(notNullValue()));
    assertThat(user.getRoles(), is(empty()));
  }

  @Test
  public void testToUser_withEmptyRoles() {
    ApiCreateUser apiCreateUser = new ApiCreateUser(
        "testuser",
        "Test",
        "User",
        "test@example.com",
        "password123",
        ApiUserStatus.active,
        Collections.emptySet());

    User user = apiCreateUser.toUser();

    assertThat(user, is(notNullValue()));
    assertThat(user.getUserId(), is("testuser"));
    assertThat(user.getRoles(), is(notNullValue()));
    assertThat(user.getRoles(), is(empty()));
  }

  @Test
  public void testToUser_withRoles() {
    Set<String> roles = new HashSet<>();
    roles.add("nx-admin");
    roles.add("nx-developer");

    ApiCreateUser apiCreateUser = new ApiCreateUser(
        "testuser",
        "Test",
        "User",
        "test@example.com",
        "password123",
        ApiUserStatus.active,
        roles);

    User user = apiCreateUser.toUser();

    assertThat(user, is(notNullValue()));
    assertThat(user.getUserId(), is("testuser"));
    assertThat(user.getRoles(), is(notNullValue()));
    assertThat(user.getRoles(), hasSize(2));
  }

  @Test
  public void testSetRoles_withNull() {
    ApiCreateUser apiCreateUser = new ApiCreateUser(
        "testuser",
        "Test",
        "User",
        "test@example.com",
        "password123",
        ApiUserStatus.active,
        new HashSet<>());

    apiCreateUser.setRoles(null);
    User user = apiCreateUser.toUser();

    assertThat(user, is(notNullValue()));
    assertThat(user.getRoles(), is(notNullValue()));
    assertThat(user.getRoles(), is(empty()));
  }
}
