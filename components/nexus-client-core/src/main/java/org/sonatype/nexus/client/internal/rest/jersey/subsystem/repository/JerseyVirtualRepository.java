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

import org.sonatype.nexus.client.core.subsystem.repository.RepositoryStatus;
import org.sonatype.nexus.client.core.subsystem.repository.VirtualRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;

/**
 * Jersey based {@link VirtualRepository} implementation.
 *
 * @since 2.3
 */
public class JerseyVirtualRepository<T extends VirtualRepository>
    extends JerseyRepository<T, RepositoryShadowResource, RepositoryStatus>
    implements VirtualRepository<T>
{

  static final String REPO_TYPE = "virtual";

  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.ShadowRepository";

  public JerseyVirtualRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyVirtualRepository(final JerseyNexusClient nexusClient, final RepositoryShadowResource settings) {
    super(nexusClient, settings);
  }

  @Override
  protected RepositoryShadowResource createSettings() {
    final RepositoryShadowResource settings = new RepositoryShadowResource();

    settings.setRepoType(REPO_TYPE);
    settings.setProviderRole(PROVIDER_ROLE);
    settings.setExposed(true);

    return settings;
  }

  private T me() {
    return (T) this;
  }

  @Override
  public String virtualizedRepository() {
    return settings().getShadowOf();
  }

  @Override
  public T ofRepository(final String repositoryId) {
    settings().setShadowOf(repositoryId);
    return me();
  }

}
