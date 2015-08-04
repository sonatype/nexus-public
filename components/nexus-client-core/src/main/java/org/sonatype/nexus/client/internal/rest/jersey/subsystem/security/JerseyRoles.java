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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.security;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.security.Role;
import org.sonatype.nexus.client.core.subsystem.security.Roles;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.security.rest.model.RoleListResourceResponse;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.RoleResourceResponse;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Jersey based {@link Roles} implementation.
 *
 * @since 2.3
 */
public class JerseyRoles
    extends SubsystemSupport<JerseyNexusClient>
    implements Roles
{

  public JerseyRoles(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public JerseyRole create(final String id) {
    return new JerseyRole(getNexusClient(), id);
  }

  @Override
  public Role get(final String id) {
    try {
      return convert(
          getNexusClient()
              .serviceResource(path(id))
              .get(RoleResourceResponse.class)
              .getData()
      );
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public Collection<Role> get() {
    final RoleListResourceResponse roles;
    try {
      roles = getNexusClient()
          .serviceResource("roles")
          .get(RoleListResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }

    return Collections2.transform(roles.getData(), new Function<RoleResource, Role>()
    {
      @Override
      public Role apply(@Nullable final RoleResource input) {
        return convert(input);
      }
    });
  }

  private JerseyRole convert(@Nullable final RoleResource resource) {
    if (resource == null) {
      return null;
    }
    final JerseyRole role = new JerseyRole(getNexusClient(), resource.getId(), resource);
    role.overwriteWith(resource);
    return role;
  }

  static String path(final String id) {
    try {
      return "roles/" + URLEncoder.encode(id, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

}
