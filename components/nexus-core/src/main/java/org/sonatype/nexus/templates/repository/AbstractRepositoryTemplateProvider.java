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
package org.sonatype.nexus.templates.repository;

import java.io.IOException;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.remote.RemoteProviderHintFactory;
import org.sonatype.nexus.templates.AbstractTemplateProvider;
import org.sonatype.nexus.templates.TemplateSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract class for template providers that provides templates for Repositories.
 *
 * @author cstamas
 */
public abstract class AbstractRepositoryTemplateProvider
    extends AbstractTemplateProvider<RepositoryTemplate>
{

  private RepositoryTypeRegistry repositoryTypeRegistry;

  private NexusConfiguration nexusConfiguration;

  private RemoteProviderHintFactory remoteProviderHintFactory;

  @Inject
  public void setNexusConfiguration(final NexusConfiguration nexusConfiguration) {
    this.nexusConfiguration = checkNotNull(nexusConfiguration);
  }

  @Inject
  public void setRemoteProviderHintFactory(final RemoteProviderHintFactory remoteProviderHintFactory) {
    this.remoteProviderHintFactory = checkNotNull(remoteProviderHintFactory);
  }

  @Inject
  public void setRepositoryTypeRegistry(final RepositoryTypeRegistry repositoryTypeRegistry) {
    this.repositoryTypeRegistry = checkNotNull(repositoryTypeRegistry);
  }

  protected Repository createRepository(CRepository repository)
      throws ConfigurationException, IOException
  {
    return nexusConfiguration.createRepository(repository);
  }

  public RemoteProviderHintFactory getRemoteProviderHintFactory() {
    return remoteProviderHintFactory;
  }

  public TemplateSet getTemplates(Object filter) {
    return getTemplates().getTemplates(filter);
  }

  public TemplateSet getTemplates(Object... filters) {
    return getTemplates().getTemplates(filters);
  }

  public ManuallyConfiguredRepositoryTemplate createManuallyTemplate(CRepositoryCoreConfiguration configuration)
      throws ConfigurationException
  {
    final CRepository repoConfig = configuration.getConfiguration(false);

    RepositoryTypeDescriptor rtd =
        repositoryTypeRegistry.getRepositoryTypeDescriptor(repoConfig.getProviderRole(),
            repoConfig.getProviderHint());

    if (rtd == null) {
      final String msg =
          String.format(
              "Repository being created \"%s\" (repoId=%s) has corresponding type that is not registered in Core: Repository type %s:%s is unknown to Nexus Core. It is probably contributed by an old Nexus plugin. Please contact plugin developers to upgrade the plugin, and register the new repository type(s) properly!",
              repoConfig.getName(), repoConfig.getId(), repoConfig.getProviderRole(),
              repoConfig.getProviderHint());

      throw new ConfigurationException(msg);
    }

    ContentClass contentClass = repositoryTypeRegistry.getRepositoryContentClass(rtd.getRole(), rtd.getHint());

    return new ManuallyConfiguredRepositoryTemplate(this, "manual", "Manually created template", contentClass,
        null, configuration);
  }

}
