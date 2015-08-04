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
package org.sonatype.nexus.repository.obr.client.internal;

import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyProxyRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.repository.obr.client.ObrProxyRepository;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;

/**
 * @since obr 2.1
 */
public class JerseyObrProxyRepository
    extends JerseyProxyRepository<ObrProxyRepository>
    implements ObrProxyRepository
{

  static final String PROVIDER = "obr-proxy";

  public JerseyObrProxyRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyObrProxyRepository(final JerseyNexusClient nexusClient, final RepositoryProxyResource resource) {
    super(nexusClient, resource);
  }

  @Override
  protected RepositoryProxyResource createSettings() {
    final RepositoryProxyResource settings = super.createSettings();

    settings.setProvider(JerseyObrProxyRepository.PROVIDER);
    settings.setRepoPolicy("RELEASE");
    settings.setIndexable(false);

    return settings;
  }

}
