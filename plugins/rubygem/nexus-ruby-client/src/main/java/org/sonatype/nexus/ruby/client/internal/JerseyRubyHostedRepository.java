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
package org.sonatype.nexus.ruby.client.internal;

import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyHostedRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.ruby.client.RubyHostedRepository;

public class JerseyRubyHostedRepository
    extends JerseyHostedRepository<RubyHostedRepository>
    implements RubyHostedRepository
{
  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.Repository";

  static final String PROVIDER = "rubygems-hosted";

  public JerseyRubyHostedRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyRubyHostedRepository(final JerseyNexusClient nexusClient,
                                    final RepositoryResource settings)
  {
    super(nexusClient, settings);
  }

  @Override
  protected RepositoryResource createSettings() {
    final RepositoryResource settings = super.createSettings();

    settings.setProviderRole(JerseyRubyHostedRepository.PROVIDER_ROLE);
    settings.setProvider(JerseyRubyHostedRepository.PROVIDER);
    settings.setRepoPolicy("RELEASE");
    settings.setIndexable(false);

    return settings;
  }
}
