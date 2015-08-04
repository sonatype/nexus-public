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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryStatus;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResourceResponse;

import com.google.common.base.Throwables;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.beanutils.BeanUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Jersey based {@link Repository} implementation.
 *
 * @since 2.3
 */
public abstract class JerseyRepository<T extends Repository, S extends RepositoryBaseResource, U extends RepositoryStatus>
    extends SubsystemSupport<JerseyNexusClient>
    implements Repository<T, U>
{

  private final String id;

  private final S settings;

  private RepositoryStatusResource status;

  private boolean shouldCreate;

  public JerseyRepository(final JerseyNexusClient nexusClient,
                          final String id)
  {
    super(nexusClient);
    this.id = id;
    this.settings = checkNotNull(createSettings());
    this.shouldCreate = true;
    this.status = null;
    settings().setId(this.id);
  }

  public JerseyRepository(final JerseyNexusClient nexusClient,
                          final S settings)
  {
    this(nexusClient, checkNotNull(checkNotNull(settings).getId()));
    this.shouldCreate = false;
    overwriteSettingsWith(settings);
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public String name() {
    return settings().getName();
  }

  @Override
  public String contentUri() {
    return settings().getContentResourceURI();
  }

  @Override
  public T withName(final String name) {
    settings().setName(name);
    return me();
  }

  protected S settings() {
    return settings;
  }

  @Override
  public synchronized T refresh() {
    S processed = null;

    if (!shouldCreate) {
      processed = doGet();
    }

    shouldCreate = processed == null;
    status = null;

    overwriteSettingsWith(processed);

    return me();
  }

  @Override
  public synchronized T save() {
    final S processed;
    if (shouldCreate) {
      processed = doCreate();
    }
    else {
      processed = doUpdate();
    }
    overwriteSettingsWith(processed);
    shouldCreate = false;
    status = null;
    return me();
  }

  @Override
  public synchronized T remove() {
    doRemove();
    shouldCreate = true;
    status = null;
    return me();
  }

  @Override
  public U status() {
    if (status == null && !shouldCreate) {
      status = doGetStatus();
    }
    return convertStatus(status);
  }

  @Override
  public T putInService() {
    final RepositoryStatusResource newStatus = doGetStatus();
    newStatus.setLocalStatus("IN_SERVICE");
    doUpdateStatus(newStatus);
    return me();
  }

  @Override
  public T putOutOfService() {
    final RepositoryStatusResource newStatus = doGetStatus();
    newStatus.setLocalStatus("OUT_OF_SERVICE");
    doUpdateStatus(newStatus);
    return me();
  }

  protected abstract S createSettings();

  String uri() {
    return "repositories";
  }

  private void overwriteSettingsWith(final S source) {
    try {
      BeanUtils.copyProperties(settings(), source == null ? checkNotNull(createSettings()) : source);
      settings().setId(id());
    }
    catch (final Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private T me() {
    return (T) this;
  }

  S doGet() {
    try {
      return (S) getNexusClient()
          .serviceResource(uri() + "/" + id())
          .get(RepositoryResourceResponse.class)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  S doCreate() {
    final RepositoryResourceResponse request = new RepositoryResourceResponse();
    request.setData(settings());

    try {
      return (S) getNexusClient()
          .serviceResource(uri())
          .post(RepositoryResourceResponse.class, request)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  S doUpdate() {
    final RepositoryResourceResponse request = new RepositoryResourceResponse();
    request.setData(settings());

    try {
      return (S) getNexusClient()
          .serviceResource(uri() + "/" + id())
          .put(RepositoryResourceResponse.class, request)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  private void doRemove() {
    try {
      getNexusClient()
          .serviceResource(uri() + "/" + id())
          .delete();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  U convertStatus(final RepositoryStatusResource status) {
    if (status == null) {
      return (U) new RepositoryStatusImpl(false);
    }
    return (U) new RepositoryStatusImpl("IN_SERVICE".equals(status.getLocalStatus()));
  }

  RepositoryStatusResource doGetStatus() {
    try {
      return getNexusClient()
          .serviceResource("repositories/" + id() + "/status")
          .get(RepositoryStatusResourceResponse.class)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  RepositoryStatusResource doUpdateStatus(final RepositoryStatusResource status) {
    final RepositoryStatusResourceResponse request = new RepositoryStatusResourceResponse();
    request.setData(status);
    try {
      return this.status = getNexusClient()
          .serviceResource("repositories/" + id() + "/status")
          .put(RepositoryStatusResourceResponse.class, request)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  public boolean isExposed() {
    return settings().isExposed();
  }

}
