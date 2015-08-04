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
import org.sonatype.nexus.client.core.subsystem.security.Privilege;
import org.sonatype.nexus.client.core.subsystem.security.Privileges;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.security.rest.model.PrivilegeListResourceResponse;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.PrivilegeStatusResourceResponse;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Jersey based {@link org.sonatype.nexus.client.core.subsystem.security.Privileges} implementation.
 *
 * @since 2.3
 */
public class JerseyPrivileges
    extends SubsystemSupport<JerseyNexusClient>
    implements Privileges
{

  public JerseyPrivileges(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public JerseyPrivilege create(final String id) {
    return new JerseyPrivilege(getNexusClient(), id);
  }

  @Override
  public Privilege create() {
    return create(null);
  }

  @Override
  public Privilege get(final String id) {
    try {
      return convert(
          getNexusClient()
              .serviceResource(path(id))
              .get(PrivilegeStatusResourceResponse.class)
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
  public Collection<Privilege> get() {
    final PrivilegeListResourceResponse privileges;
    try {
      privileges = getNexusClient()
          .serviceResource("privileges")
          .get(PrivilegeListResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }

    return Collections2.transform(privileges.getData(), new Function<PrivilegeStatusResource, Privilege>()
    {
      @Override
      public Privilege apply(@Nullable final PrivilegeStatusResource input) {
        return convert(input);
      }
    });
  }

  private JerseyPrivilege convert(@Nullable PrivilegeStatusResource resource) {
    if (resource == null) {
      return null;
    }

    final JerseyPrivilege privilege = new JerseyPrivilege(getNexusClient(), resource.getId(), resource);
    privilege.overwriteWith(resource);
    return privilege;
  }

  static String path(final String id) {
    try {
      return "privileges/" + URLEncoder.encode(id, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

}
