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

import java.util.Map;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.AuthenticationInfoConverter;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.storage.remote.RemoteProviderHintFactory;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractProxyRepositoryConfigurator
    extends AbstractRepositoryConfigurator
{
  private AuthenticationInfoConverter authenticationInfoConverter;

  private GlobalRemoteConnectionSettings globalRemoteConnectionSettings;

  private RemoteProviderHintFactory remoteProviderHintFactory;

  private Map<String, RemoteRepositoryStorage> remoteRepositoryStorages;

  @Inject
  public void populateAbstractProxyRepositoryConfigurator(AuthenticationInfoConverter authenticationInfoConverter,
                                             GlobalRemoteConnectionSettings globalRemoteConnectionSettings,
                                             RemoteProviderHintFactory remoteProviderHintFactory,
                                             Map<String, RemoteRepositoryStorage> remoteRepositoryStorages)
  {
    this.authenticationInfoConverter = checkNotNull(authenticationInfoConverter);
    this.globalRemoteConnectionSettings = checkNotNull(globalRemoteConnectionSettings);
    this.remoteProviderHintFactory = checkNotNull(remoteProviderHintFactory);
    this.remoteRepositoryStorages = checkNotNull(remoteRepositoryStorages);
  }

  @Override
  public void doApplyConfiguration(Repository repository, ApplicationConfiguration configuration,
                                   CRepositoryCoreConfiguration coreConfig)
      throws ConfigurationException
  {
    super.doApplyConfiguration(repository, configuration, coreConfig);

    // proxy stuff, but is optional!

    // FIXME: hm, we are called when we are dirty, so....
    CRepository repo = coreConfig.getConfiguration(true);

    if (repo.getRemoteStorage() != null) {
      // NOTE: we are intentionally _casting_ it, not calling adaptToFacet(), since repo implementation
      // still does not know that is should be a proxy repo!
      ProxyRepository prepository = (ProxyRepository) repository;

      try {
        if (repo.getRemoteStorage() != null) {
          RemoteRepositoryStorage oldRemoteStorage = prepository.getRemoteStorage();

          RemoteRepositoryStorage configRemoteStorage =
              getRemoteRepositoryStorage(repo.getId(), repo.getRemoteStorage().getUrl(),
                  repo.getRemoteStorage().getProvider());

          // detect do we really need to set remote storage
          if (oldRemoteStorage == null || oldRemoteStorage != configRemoteStorage) {
            // validate the remoteUrl with new remote storage
            configRemoteStorage.validateStorageUrl(repo.getRemoteStorage().getUrl());

            // set the chosen remote storage
            prepository.setRemoteStorage(configRemoteStorage);
            // mark remote storage context dirty, if applicable
            final RemoteStorageContext ctx = prepository.getRemoteStorageContext();
            if (ctx != null) {
              ctx.incrementGeneration();
            }
          }
          else {
            // just validate
            oldRemoteStorage.validateStorageUrl(repo.getRemoteStorage().getUrl());
          }

          if (repo.getRemoteStorage().getAuthentication() != null) {
            prepository.setRemoteAuthenticationSettings(
                authenticationInfoConverter.convertAndValidateFromModel(repo.getRemoteStorage().getAuthentication()));
          }

          if (repo.getRemoteStorage().getConnectionSettings() != null) {
            prepository.setRemoteConnectionSettings(globalRemoteConnectionSettings
                .convertAndValidateFromModel(repo.getRemoteStorage().getConnectionSettings()));
          }
        }
        else {
          prepository.setRemoteStorage(null);
        }
      }
      catch (RemoteStorageException e) {
        ValidationResponse response = new ApplicationValidationResponse();

        ValidationMessage error = new ValidationMessage("remoteStorageUrl", e.getMessage(), e.getMessage());

        response.addValidationError(error);

        throw new InvalidConfigurationException(response);
      }
    }
  }

  @Override
  protected void doPrepareForSave(Repository repository, ApplicationConfiguration configuration,
                                  CRepositoryCoreConfiguration coreConfiguration)
  {
    super.doPrepareForSave(repository, configuration, coreConfiguration);

    if (repository instanceof ProxyRepository) {
      // real cast needed here, adapt would return null!
      ProxyRepository prepository = (ProxyRepository) repository;

      // FIXME: hm, we are called when we are dirty, so....
      CRepository repoConfig = coreConfiguration.getConfiguration(true);

      if (repoConfig.getRemoteStorage() != null) {
        RemoteStorageContext rsc = prepository.getRemoteStorageContext();

        // NEXUS-5258 Do not persist storage provider hint if it's the default one
        if (remoteProviderHintFactory.getDefaultHttpRoleHint().equals(
            prepository.getRemoteStorage().getProviderId())) {
          repoConfig.getRemoteStorage().setProvider(null);
        }

        if (rsc.hasRemoteAuthenticationSettings()) {
          repoConfig.getRemoteStorage().setAuthentication(
              authenticationInfoConverter.convertToModel(rsc.getRemoteAuthenticationSettings()));
        }
        else {
          repoConfig.getRemoteStorage().setAuthentication(null);
        }

        if (rsc.hasRemoteConnectionSettings()) {
          repoConfig.getRemoteStorage().setConnectionSettings(
              globalRemoteConnectionSettings.convertToModel(rsc.getRemoteConnectionSettings()));
        }
        else {
          repoConfig.getRemoteStorage().setConnectionSettings(null);
        }
      }
    }
  }

  protected RemoteRepositoryStorage getRemoteRepositoryStorage(final String repoId, 
                                                               final String remoteUrl,
                                                               final String provider)
      throws InvalidConfigurationException
  {
    try {
      final String mungledHint = remoteProviderHintFactory.getRoleHint(remoteUrl, provider);
      final RemoteRepositoryStorage result = remoteRepositoryStorages.get(mungledHint);
      if (result != null) {
        return result;
      }
      throw new InvalidConfigurationException("Repository " + repoId
          + " have remote storage with unsupported provider: " + provider);
    }
    catch (IllegalArgumentException e) {
      throw new InvalidConfigurationException("Repository " + repoId
          + " have remote storage with unsupported provider: " + provider, e);
    }
  }
}
