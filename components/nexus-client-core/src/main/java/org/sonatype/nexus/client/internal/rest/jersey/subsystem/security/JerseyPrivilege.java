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

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.client.core.subsystem.security.Privilege;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.JerseyEntitySupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.PrivilegeResourceRequest;
import org.sonatype.security.rest.model.PrivilegeListResourceResponse;
import org.sonatype.security.rest.model.PrivilegeProperty;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.PrivilegeStatusResourceResponse;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.beanutils.BeanUtils;

import static org.sonatype.nexus.client.internal.rest.jersey.subsystem.security.JerseyPrivileges.path;

/**
 * Jersey based {@link org.sonatype.nexus.client.core.subsystem.security.Privilege} implementation.
 *
 * @since 2.3
 */
public class JerseyPrivilege
    extends JerseyEntitySupport<Privilege, PrivilegeStatusResource>
    implements Privilege
{

  public JerseyPrivilege(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyPrivilege(final JerseyNexusClient nexusClient, final String id,
                         final PrivilegeStatusResource settings)
  {
    super(nexusClient, id, settings);
  }

  @Override
  protected PrivilegeStatusResource createSettings(final String id) {
    final PrivilegeStatusResource resource = new PrivilegeStatusResource();
    resource.setId(id);
    resource.setUserManaged(true);
    resource.setType("target");
    return resource;
  }

  @Override
  protected PrivilegeStatusResource doGet() {
    try {
      return getNexusClient()
          .serviceResource(path(id()))
          .get(PrivilegeStatusResourceResponse.class)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public String id() {
    return settings().getId();
  }

  /**
   * Privileges are immutable, you cannot update a privilege.
   * Creating "one" privilege will return many privileges depending on the methods set,
   * so this method has to break the "fluent contract" and return null.
   *
   * @return always null.
   * @see #create()
   * @deprecated use #create() instead.
   */
  @Override
  public synchronized Privilege save() {
    super.save();
    return null;
  }

  /**
   * Create privileges based on this instance.
   *
   * @return the privileges created for this instance.
   */
  public synchronized Collection<Privilege> create() {
    if (shouldCreate()) {
      final PrivilegeResourceRequest request = new PrivilegeResourceRequest();
      request.setData(convert());
      try {
        final List<PrivilegeStatusResource> resources = getNexusClient()
            .serviceResource("privileges_target")
            .post(PrivilegeListResourceResponse.class, request)
            .getData();

        return Collections2.transform(resources, new Function<PrivilegeStatusResource, Privilege>()
        {
          @Nullable
          @Override
          public Privilege apply(@Nullable final PrivilegeStatusResource resource) {
            return new JerseyPrivilege(getNexusClient(), resource.getId(), resource);
          }
        });
      }
      catch (UniformInterfaceException e) {
        throw getNexusClient().convert(e);
      }
      catch (ClientHandlerException e) {
        throw getNexusClient().convert(e);
      }
    }

    throw new IllegalStateException("This privilege was already loaded from Nexus.");
  }

  /**
   * Creating "one" privilege will return many privileges depending on the methods set.
   *
   * @return always null
   */
  @Override
  protected PrivilegeStatusResource doCreate() {
    final PrivilegeResourceRequest request = new PrivilegeResourceRequest();
    request.setData(convert());
    try {
      getNexusClient()
          .serviceResource("privileges_target")
          .post(PrivilegeListResourceResponse.class, request);
      return null;
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  private PrivilegeResource convert() {
    final PrivilegeResource resource = new PrivilegeResource();
    try {
      BeanUtils.copyProperties(resource, settings());
    }
    catch (Exception e) {
      Throwables.propagate(e);
    }

    resource.setMethod(methods() != null ? Lists.newArrayList(methods()) : null);
    resource.setRepositoryGroupId(repositoryGroupId());
    resource.setRepositoryId(repositoryId());
    resource.setRepositoryTargetId(targetId());
    resource.setType(type());

    return resource;
  }

  @Override
  protected PrivilegeStatusResource doUpdate() {
    throw new UnsupportedOperationException("A privilege is immutable.");
  }

  @Override
  protected void doRemove() {
    try {
      getNexusClient()
          .serviceResource(path(id()))
          .delete();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public String name() {
    return settings().getName();
  }

  @Override
  public String description() {
    return settings().getDescription();
  }

  @Override
  public Privilege withDescription(final String value) {
    settings().setDescription(value);
    return this;
  }

  @Override
  public String type() {
    return settings().getType();
  }

  private void setProperty(String key, String value) {
    final List<PrivilegeProperty> properties = settings().getProperties();
    for (PrivilegeProperty property : properties) {
      if (property.getKey().equals(key)) {
        property.setValue(value);
        return;
      }
    }

    final PrivilegeProperty property = new PrivilegeProperty();
    property.setKey(key);
    property.setValue(value);
    properties.add(property);
  }

  private String getProperty(String key) {
    final List<PrivilegeProperty> properties = settings().getProperties();
    for (PrivilegeProperty property : properties) {
      if (property.getKey().equals(key)) {
        return property.getValue();
      }
    }
    return null;
  }

  @Override
  public String repositoryId() {
    return getProperty("repositoryId");
  }

  @Override
  public Privilege withRepositoryId(final String repositoryId) {
    setProperty("repositoryId", repositoryId);
    return this;
  }

  @Override
  public List<String> methods() {
    final String methods = getProperty("method");
    if (methods == null) {
      return null;
    }
    return Lists.newArrayList(methods.split(","));
  }

  @Override
  public Privilege withMethods(final String... methods) {
    setProperty("method", Joiner.on(",").join(methods));
    return this;
  }

  @Override
  public String targetId() {
    return getProperty("repositoryTargetId");
  }

  @Override
  public Privilege withTargetId(final String targetId) {
    setProperty("repositoryTargetId", targetId);
    return this;
  }

  @Override
  public String repositoryGroupId() {
    return getProperty("repositoryGroupId");
  }

  @Override
  public Privilege withRepositoryGroupId(String groupId) {
    setProperty("repositoryGroupId", groupId);
    return this;
  }

  @Override
  public Privilege withName(final String value) {
    settings().setName(value);
    return this;
  }

}
