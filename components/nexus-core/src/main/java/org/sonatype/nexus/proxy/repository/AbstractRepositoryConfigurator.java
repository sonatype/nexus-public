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
package org.sonatype.nexus.proxy.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.plugins.RepositoryCustomizer;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractRepositoryConfigurator
    implements Configurator<Repository, CRepositoryCoreConfiguration>
{
  private RepositoryRegistry repositoryRegistry;

  private Map<String, LocalRepositoryStorage> localRepositoryStorages;

  private Map<String, RepositoryCustomizer> pluginRepositoryConfigurators;

  @Inject
  public void populateAbstractRepositoryConfigurator(final RepositoryRegistry repositoryRegistry,
                                                     final Map<String, LocalRepositoryStorage> localRepositoryStorages,
                                                     final Map<String, RepositoryCustomizer> pluginRepositoryConfigurators)
  {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.localRepositoryStorages = checkNotNull(localRepositoryStorages);
    this.pluginRepositoryConfigurators = checkNotNull(pluginRepositoryConfigurators);
  }

  @Override
  public final void applyConfiguration(final Repository target,
                                       final ApplicationConfiguration configuration,
                                       final CRepositoryCoreConfiguration config)
      throws ConfigurationException
  {
    doApplyConfiguration(target, configuration, config);

    // config done, apply customizations if needed
    if (pluginRepositoryConfigurators != null) {
      for (RepositoryCustomizer configurator : pluginRepositoryConfigurators.values()) {
        if (configurator.isHandledRepository(target)) {
          configurator.configureRepository(target);
        }
      }
    }
  }

  public final void prepareForSave(Repository target, ApplicationConfiguration configuration,
                                   CRepositoryCoreConfiguration config)
  {
    // in 1st round, i intentionally choosed to make our lives bitter, and handle plexus config manually
    // later we will see about it
    doPrepareForSave(target, configuration, config);
  }

  protected void doApplyConfiguration(Repository repository, ApplicationConfiguration configuration,
                                      CRepositoryCoreConfiguration coreConfiguration)
      throws ConfigurationException
  {
    // Setting common things on a repository

    // FIXME: hm, we are called when we are dirty, so....
    CRepository repo = coreConfiguration.getConfiguration(true);

    // NX-198: filling up the default variable to store the "default" local URL
    File defaultStorageFile = new File(new File(configuration.getWorkingDirectory(), "storage"), repo.getId());

    try {
      repo.defaultLocalStorageUrl = defaultStorageFile.toURI().toURL().toString();
    }
    catch (MalformedURLException e) {
      // will not happen, not user settable
      throw new InvalidConfigurationException("Malformed URL for LocalRepositoryStorage!", e);
    }

    String localUrl;
    boolean usingDefaultLocalUrl;

    if (repo.getLocalStorage() != null && !Strings.isNullOrEmpty(repo.getLocalStorage().getUrl())) {
      localUrl = repo.getLocalStorage().getUrl();
      usingDefaultLocalUrl = false;
    }
    else {
      localUrl = repo.defaultLocalStorageUrl;
      usingDefaultLocalUrl = true;
    }

    if (repo.getLocalStorage() == null) {
      repo.setLocalStorage(new CLocalStorage());

      repo.getLocalStorage().setProvider("file");
    }

    LocalRepositoryStorage ls = getLocalRepositoryStorage(repo.getId(), repo.getLocalStorage().getProvider());

    try {
      ls.validateStorageUrl(localUrl);

      if (!usingDefaultLocalUrl) {
        repo.getLocalStorage().setUrl(localUrl);
      }

      repository.setLocalStorage(ls);
      // mark local storage context dirty, if applicable
      final LocalStorageContext ctx = repository.getLocalStorageContext();
      if (ctx != null) {
        ctx.incrementGeneration();
      }
    }
    catch (LocalStorageException e) {
      ValidationResponse response = new ApplicationValidationResponse();

      ValidationMessage error =
          new ValidationMessage("overrideLocalStorageUrl", "Repository has an invalid local storage URL '"
              + localUrl, "Invalid file location");

      response.addValidationError(error);

      throw new InvalidConfigurationException(response);
    }

    // clear the NotFoundCache
    if (repository.getNotFoundCache() != null) {
      repository.getNotFoundCache().purge();
    }
  }

  protected void doPrepareForSave(Repository repository, ApplicationConfiguration configuration,
                                  CRepositoryCoreConfiguration coreConfiguration)
  {
    // Setting common things on a repository
  }

  // ==

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  protected LocalRepositoryStorage getLocalRepositoryStorage(String repoId, String provider)
      throws InvalidConfigurationException
  {
    final LocalRepositoryStorage result = localRepositoryStorages.get(provider);
    if (result != null) {
      return result;
    }
    throw new InvalidConfigurationException("Repository " + repoId
        + " have local storage with unsupported provider: " + provider);
  }

}
