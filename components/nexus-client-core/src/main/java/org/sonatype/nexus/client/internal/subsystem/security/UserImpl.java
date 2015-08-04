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
package org.sonatype.nexus.client.internal.subsystem.security;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.client.core.subsystem.security.User;
import org.sonatype.nexus.client.internal.subsystem.security.UsersImpl.UsersClient;
import org.sonatype.nexus.client.rest.support.EntitySupport;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;

import static com.google.common.base.Preconditions.checkState;

/**
 * {@link User} implementation.
 *
 * @since 2.7
 */
public class UserImpl
    extends EntitySupport<User, UserResource>
    implements User
{

  private UsersClient usersClient;

  public UserImpl(final UsersClient usersClient, final UserResource settings) {
    super(settings.getUserId(), settings);
    this.usersClient = usersClient;
  }

  public UserImpl(final UsersClient usersClient, final String id) {
    super(id);
    this.usersClient = usersClient;
  }

  @Override
  protected UserResource createSettings(final String id) {
    final UserResource resource = new UserResource();
    resource.setUserId(id);
    resource.setStatus("active");
    return resource;
  }

  @Override
  protected UserResource doGet() {
    return usersClient.get(id()).getData();
  }

  @Override
  protected UserResource doCreate() {
    final UserResourceRequest request = new UserResourceRequest();
    request.setData(settings());
    final UserResource resource = usersClient.post(request).getData();
    resource.setPassword(null);
    return resource;
  }

  @Override
  protected UserResource doUpdate() {
    final UserResourceRequest request = new UserResourceRequest();
    request.setData(settings());
    return usersClient.put(id(), request).getData();
  }

  @Override
  protected void doRemove() {
    usersClient.delete(id());
  }

  @Override
  public String firstName() {
    return settings().getFirstName();
  }

  @Override
  public String lastName() {
    return settings().getLastName();
  }

  @Override
  public String email() {
    return settings().getEmail();
  }

  @Override
  public boolean isActive() {
    return "active".equals(settings().getStatus());
  }

  @Override
  public List<String> roles() {
    return Collections.unmodifiableList(settings().getRoles());
  }

  @Override
  public User withPassword(final String value) {
    checkState(shouldCreate(), "Password can only be set when user is created");
    settings().setPassword(value);
    return this;
  }

  @Override
  public User withFirstName(final String value) {
    settings().setFirstName(value);
    return this;
  }

  @Override
  public User withLastName(final String value) {
    settings().setLastName(value);
    return this;
  }

  @Override
  public User withEmail(final String value) {
    settings().setEmail(value);
    return this;
  }

  @Override
  public User enableAccess() {
    settings().setStatus("active");
    return this;
  }

  @Override
  public User disableAccess() {
    settings().setStatus("disabled");
    return this;
  }

  @Override
  public User withRole(final String value) {
    settings().addRole(value);
    return this;
  }

  @Override
  public User removeRole(final String value) {
    settings().removeRole(value);
    return this;
  }

}
