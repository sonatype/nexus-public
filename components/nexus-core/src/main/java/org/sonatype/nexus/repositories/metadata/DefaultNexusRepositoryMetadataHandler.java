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
package org.sonatype.nexus.repositories.metadata;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.repository.metadata.MetadataHandlerException;
import org.sonatype.nexus.repository.metadata.RepositoryMetadataHandler;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.sisu.goodies.common.ComponentSupport;

@Named
@Singleton
public class DefaultNexusRepositoryMetadataHandler
    extends ComponentSupport
    implements NexusRepositoryMetadataHandler
{
  private final RepositoryRegistry repositoryRegistry;

  private final RepositoryMetadataHandler repositoryMetadataHandler;

  private final Hc4Provider hc4Provider;

  @Inject
  public DefaultNexusRepositoryMetadataHandler(final RepositoryRegistry repositoryRegistry,
                                               final RepositoryMetadataHandler repositoryMetadataHandler,
                                               final Hc4Provider hc4Provider)
  {
    this.repositoryRegistry = repositoryRegistry;
    this.repositoryMetadataHandler = repositoryMetadataHandler;
    this.hc4Provider = hc4Provider;
  }

  public RepositoryMetadata readRemoteRepositoryMetadata(final String url)
      throws MetadataHandlerException,
             IOException
  {
    final Hc4RawTransport hc4RawTransport = new Hc4RawTransport(hc4Provider.createHttpClient(), url);
    return repositoryMetadataHandler.readRepositoryMetadata(hc4RawTransport);
  }

  public RepositoryMetadata readRepositoryMetadata(final String repositoryId)
      throws NoSuchRepositoryException,
             MetadataHandlerException,
             IOException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    final NexusRawTransport nrt = new NexusRawTransport(repository, false, true);
    return repositoryMetadataHandler.readRepositoryMetadata(nrt);
  }

  public void writeRepositoryMetadata(final String repositoryId, final RepositoryMetadata repositoryMetadata)
      throws NoSuchRepositoryException,
             MetadataHandlerException,
             IOException
  {
    final Repository repository = repositoryRegistry.getRepository(repositoryId);
    final NexusRawTransport nrt = new NexusRawTransport(repository, true, false);
    repositoryMetadataHandler.writeRepositoryMetadata(repositoryMetadata, nrt);
  }

}
