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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.targets.RepositoryTarget;
import org.sonatype.nexus.client.core.subsystem.targets.RepositoryTargets;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResourceResponse;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Jersey based {@link org.sonatype.nexus.client.core.subsystem.targets.RepositoryTargets} implementation.
 *
 * @since 2.3
 */
public class JerseyRepositoryTargets
    extends SubsystemSupport<JerseyNexusClient>
    implements RepositoryTargets
{

  public JerseyRepositoryTargets(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public JerseyRepositoryTarget create(final String id) {
    return new JerseyRepositoryTarget(getNexusClient(), id);
  }

  @Override
  public RepositoryTarget get(final String id) {
    try {
      return convert(
          getNexusClient()
              .serviceResource(path(id))
              .get(RepositoryTargetResourceResponse.class)
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
  public Collection<RepositoryTarget> get() {
    final RepositoryTargetListResourceResponse privileges;
    try {
      privileges = getNexusClient()
          .serviceResource("repo_targets")
          .get(RepositoryTargetListResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }

    return Collections2.transform(privileges.getData(), new Function<RepositoryTargetListResource, RepositoryTarget>()
    {
      @Override
      public RepositoryTarget apply(@Nullable final RepositoryTargetListResource input) {
        return convert(input);
      }
    });
  }

  private JerseyRepositoryTarget convert(@Nullable RepositoryTargetListResource resource) {
    if (resource == null) {
      return null;
    }

    return convert(otherDTO(resource));
  }

  private JerseyRepositoryTarget convert(@Nullable final RepositoryTargetResource resource) {
    if (resource == null) {
      return null;
    }

    final JerseyRepositoryTarget privilege = new JerseyRepositoryTarget(getNexusClient(), resource.getId(), resource);
    privilege.overwriteWith(resource);
    return privilege;
  }

  private RepositoryTargetResource otherDTO(final RepositoryTargetListResource resource) {
    final RepositoryTargetResource targetResource = new RepositoryTargetResource();

    try {
      BeanUtils.copyProperties(targetResource, resource);
    }
    catch (Exception e) {
      Throwables.propagate(e);
    }
    return targetResource;
  }

  static String path(final String id) {
    try {
      return "repo_targets/" + URLEncoder.encode(id, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

}
