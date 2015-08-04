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

import org.sonatype.nexus.client.core.subsystem.repository.HostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryStatus;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryResource;

/**
 * Jersey based {@link HostedRepository} implementation.
 *
 * @since 2.3
 */
public class JerseyHostedRepository<T extends HostedRepository>
    extends JerseyRepository<T, RepositoryResource, RepositoryStatus>
    implements HostedRepository<T>
{

  static final String REPO_TYPE = "hosted";

  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.Repository";

  public JerseyHostedRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyHostedRepository(final JerseyNexusClient nexusClient, final RepositoryResource resource) {
    super(nexusClient, resource);
  }

  @Override
  protected RepositoryResource createSettings() {
    final RepositoryResource settings = new RepositoryResource();

    settings.setRepoType(REPO_TYPE);
    settings.setProviderRole(PROVIDER_ROLE);
    settings.setExposed(true);
    settings.setWritePolicy("ALLOW_WRITE_ONCE");
    settings.setBrowseable(true);
    settings.setIndexable(false);
    settings.setRepoPolicy("MIXED");

    return settings;
  }

  private T me() {
    return (T) this;
  }

  @Override
  public T withRepoPolicy(final String policy) {
    settings().setRepoPolicy(policy);
    return me();
  }

  @Override
  public T readOnly() {
    settings().setWritePolicy("READ_ONLY");
    return me();
  }

  @Override
  public T allowRedeploy() {
    settings().setWritePolicy("ALLOW_WRITE");
    return me();
  }

  @Override
  public T disableRedeploy() {
    settings().setWritePolicy("ALLOW_WRITE_ONCE");
    return me();
  }

  @Override
  public T enableBrowsing() {
    settings().setBrowseable(true);
    return me();
  }

  @Override
  public T disableBrowsing() {
    settings().setBrowseable(false);
    return me();
  }

  @Override
  public boolean isBrowsable() {
    return settings().isBrowseable();
  }
}
