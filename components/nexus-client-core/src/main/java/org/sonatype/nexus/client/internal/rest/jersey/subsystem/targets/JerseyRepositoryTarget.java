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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.targets;

import java.util.List;

import org.sonatype.nexus.client.core.subsystem.targets.RepositoryTarget;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.JerseyEntitySupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResourceResponse;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import static org.sonatype.nexus.client.internal.rest.jersey.subsystem.targets.JerseyRepositoryTargets.path;

/**
 * Jersey based {@link org.sonatype.nexus.client.core.subsystem.security.RepositoryTarget} implementation.
 *
 * @since 2.3
 */
public class JerseyRepositoryTarget
    extends JerseyEntitySupport<RepositoryTarget, RepositoryTargetResource>
    implements RepositoryTarget
{

  public JerseyRepositoryTarget(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyRepositoryTarget(final JerseyNexusClient nexusClient, final String id,
                                final RepositoryTargetResource settings)
  {
    super(nexusClient, id, settings);
  }

  @Override
  protected RepositoryTargetResource createSettings(final String id) {
    final RepositoryTargetResource resource = new RepositoryTargetResource();
    resource.setId(id);
    return resource;
  }

  @Override
  protected RepositoryTargetResource doGet() {
    try {
      return getNexusClient()
          .serviceResource(path(id()))
          .get(RepositoryTargetResourceResponse.class)
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
  protected RepositoryTargetResource doCreate() {
    final RepositoryTargetResourceResponse request = new RepositoryTargetResourceResponse();
    request.setData(settings());
    try {
      return getNexusClient()
          .serviceResource("repo_targets")
          .post(RepositoryTargetResourceResponse.class, request)
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
  protected RepositoryTargetResource doUpdate() {
    final RepositoryTargetResourceResponse request = new RepositoryTargetResourceResponse();
    request.setData(settings());
    try {
      return getNexusClient()
          .serviceResource(path(id()))
          .put(RepositoryTargetResourceResponse.class, request)
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
  public String contentClass() {
    return settings().getContentClass();
  }

  @Override
  public List<String> patterns() {
    return settings().getPatterns();
  }

  @Override
  public RepositoryTarget withName(final String name) {
    settings().setName(name);
    return this;
  }

  @Override
  public RepositoryTarget withContentClass(final String cls) {
    settings().setContentClass(cls);
    return this;
  }

  @Override
  public RepositoryTarget withPatterns(final String... patterns) {
    settings().setPatterns(Lists.newArrayList(patterns));
    return this;
  }

  @Override
  public RepositoryTarget addPattern(final String pattern) {
    settings().addPattern(pattern);
    return this;
  }
}
