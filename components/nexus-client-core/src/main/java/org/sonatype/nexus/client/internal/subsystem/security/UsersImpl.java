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

import java.util.Collection;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.sonatype.nexus.client.core.subsystem.Restlet1xClient;
import org.sonatype.nexus.client.core.subsystem.security.User;
import org.sonatype.nexus.client.core.subsystem.security.Users;
import org.sonatype.security.rest.model.UserListResourceResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;
import org.sonatype.sisu.siesta.client.ClientBuilder.Target.Factory;

import com.google.common.base.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;

/**
 * {@link Users} implementation.
 *
 * @since 2.7
 */
public class UsersImpl
    implements Users
{

  private final UsersClient usersClient;

  @Inject
  public UsersImpl(final Factory factory) {
    usersClient = checkNotNull(factory, "factory").build(UsersClient.class);
  }

  @Override
  public UserImpl create(final String id) {
    return new UserImpl(usersClient, id);
  }

  @Override
  public User get(final String id) {
    return convert(usersClient.get(id).getData());
  }

  @Override
  public Collection<User> get() {
    return transform(usersClient.get().getData(), new Function<UserResource, User>()
    {
      @Override
      public User apply(@Nullable final UserResource input) {
        return convert(input);
      }
    });
  }

  private UserImpl convert(@Nullable final UserResource resource) {
    if (resource == null) {
      return null;
    }
    final UserImpl role = new UserImpl(usersClient, resource);
    role.overwriteWith(resource);
    return role;
  }

  @Path("/service/local/users")
  public static interface UsersClient
      extends Restlet1xClient
  {

    @GET
    @Path("/{id}")
    UserResourceResponse get(@PathParam("id") String userId);

    @GET
    UserListResourceResponse get();

    @POST
    UserResourceResponse post(UserResourceRequest request);

    @PUT
    @Path("/{id}")
    UserResourceResponse put(@PathParam("id") String userId, UserResourceRequest request);

    @DELETE
    @Path("/{id}")
    void delete(@PathParam("id") String userId);
  }

}
