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
package org.sonatype.nexus.rest.repositories;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.inject.Inject;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.AuthenticationInfoConverter;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.NexusCompat;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.nexus.rest.RepositoryURLBuilder;
import org.sonatype.nexus.rest.global.AbstractGlobalConfigurationPlexusResource;
import org.sonatype.nexus.rest.model.AuthenticationSettings;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public abstract class AbstractRepositoryPlexusResource
    extends AbstractNexusPlexusResource
{
  /**
   * Key to store Repo with which we work against.
   */
  public static final String REPOSITORY_ID_KEY = "repositoryId";

  private AuthenticationInfoConverter authenticationInfoConverter;

  private GlobalRemoteConnectionSettings globalRemoteConnectionSettings;

  private ApplicationConfiguration applicationConfiguration;

  private RepositoryURLBuilder repositoryURLBuilder;

  @Inject
  public void setAuthenticationInfoConverter(final AuthenticationInfoConverter authenticationInfoConverter) {
    this.authenticationInfoConverter = authenticationInfoConverter;
  }

  @Inject
  public void setGlobalRemoteConnectionSettings(final GlobalRemoteConnectionSettings globalRemoteConnectionSettings) {
    this.globalRemoteConnectionSettings = globalRemoteConnectionSettings;
  }

  @Inject
  public void setApplicationConfiguration(final ApplicationConfiguration applicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration;
  }

  @Inject
  public void setRepositoryURLBuilder(final RepositoryURLBuilder repositoryURLBuilder) {
    this.repositoryURLBuilder = repositoryURLBuilder;
  }

  protected AuthenticationInfoConverter getAuthenticationInfoConverter() {
    return authenticationInfoConverter;
  }

  protected GlobalRemoteConnectionSettings getGlobalRemoteConnectionSettings() {
    return globalRemoteConnectionSettings;
  }

  protected ApplicationConfiguration getApplicationConfiguration() {
    return applicationConfiguration;
  }

  /**
   * Pull the repository Id out of the Request.
   */
  protected String getRepositoryId(Request request) {
    return request.getAttributes().get(REPOSITORY_ID_KEY).toString();
  }

  // CLEAN
  public String getRestRepoRemoteStatus(ProxyRepository repository, Request request, Response response)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();

    boolean forceCheck = form.getFirst("forceCheck") != null;

    RemoteStatus rs =
        repository.getRemoteStatus(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT), forceCheck);

    if (RemoteStatus.UNKNOWN.equals(rs)) {
      // set status to ACCEPTED, since we have incomplete info
      response.setStatus(Status.SUCCESS_ACCEPTED);
    }

    return rs == null ? null : rs.toString() + (rs.getReason() == null ? "" : ":" + rs.getReason());
  }

  // clean
  public String getRestRepoType(Repository repository) {
    if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      return RepositoryBaseResourceConverter.REPO_TYPE_PROXIED;
    }
    else if (repository.getRepositoryKind().isFacetAvailable(HostedRepository.class)) {
      return RepositoryBaseResourceConverter.REPO_TYPE_HOSTED;
    }
    else if (repository.getRepositoryKind().isFacetAvailable(ShadowRepository.class)) {
      return RepositoryBaseResourceConverter.REPO_TYPE_VIRTUAL;
    }
    else if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      return RepositoryBaseResourceConverter.REPO_TYPE_GROUP;
    }
    else {
      throw new IllegalArgumentException("The passed model with class" + repository.getClass().getName()
          + " is not recognized!");
    }
  }

  protected RepositoryListResourceResponse listRepositories(Request request, boolean allReposes)
      throws ResourceException
  {
    return listRepositories(request, allReposes, true);
  }

  // clean
  protected RepositoryListResourceResponse listRepositories(Request request, boolean allReposes,
                                                            boolean includeGroups)
      throws ResourceException
  {
    RepositoryListResourceResponse result = new RepositoryListResourceResponse();

    RepositoryListResource repoRes;

    Collection<Repository> repositories = getRepositoryRegistry().getRepositories();

    for (Repository repository : repositories) {
      // To save UI changes at the moment, not including groups in repo call
      if ((allReposes || repository.isUserManaged())
          && (includeGroups || !repository.getRepositoryKind().isFacetAvailable(GroupRepository.class))) {
        repoRes = new RepositoryListResource();

        repoRes.setResourceURI(createRepositoryReference(request, repository.getId()).toString());

        repoRes.setContentResourceURI(repositoryURLBuilder.getExposedRepositoryContentUrl(repository));

        repoRes.setRepoType(getRestRepoType(repository));

        repoRes.setProvider(NexusCompat.getRepositoryProviderHint(repository));

        repoRes.setProviderRole(NexusCompat.getRepositoryProviderRole(repository));

        repoRes.setFormat(repository.getRepositoryContentClass().getId());

        repoRes.setId(repository.getId());

        repoRes.setName(repository.getName());

        repoRes.setUserManaged(repository.isUserManaged());

        repoRes.setExposed(repository.isExposed());

        repoRes.setEffectiveLocalStorageUrl(repository.getLocalUrl());

        if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
          repoRes.setRepoPolicy(repository.adaptToFacet(MavenRepository.class).getRepositoryPolicy()
              .toString());
        }

        if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
          repoRes.setRemoteUri(repository.adaptToFacet(ProxyRepository.class).getRemoteUrl());
        }

        result.addData(repoRes);
      }
    }

    return result;
  }

  // clean
  protected RepositoryResourceResponse getRepositoryResourceResponse(Request request, String repoId)
      throws ResourceException
  {
    RepositoryResourceResponse result = new RepositoryResourceResponse();

    try {
      RepositoryBaseResource resource = null;

      Repository repository = getRepositoryRegistry().getRepository(repoId);

      if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
        // it is a group, not a repo
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Repository Not Found");
      }

      resource = getRepositoryRestModel(request, repository);

      result.setData(resource);
    }
    catch (NoSuchRepositoryAccessException e) {
      getLogger().warn("Repository access denied, id=" + repoId);

      throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "Access Denied to Repository");
    }
    catch (NoSuchRepositoryException e) {
      getLogger().warn("Repository not found, id=" + repoId);

      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Repository Not Found");
    }
    return result;
  }

  /**
   * Converting App model to REST DTO.
   */
  public RepositoryBaseResource getRepositoryRestModel(Request request, Repository repository) {
    RepositoryResource resource = null;

    if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      resource = getRepositoryProxyRestModel(repository.adaptToFacet(ProxyRepository.class));
    }
    else if (repository.getRepositoryKind().isFacetAvailable(ShadowRepository.class)) {
      return getRepositoryShadowRestModel(request, repository.adaptToFacet(ShadowRepository.class));
    }
    else {
      resource = new RepositoryResource();
    }

    resource.setContentResourceURI(repositoryURLBuilder.getExposedRepositoryContentUrl(repository));

    resource.setProvider(NexusCompat.getRepositoryProviderHint(repository));

    resource.setProviderRole(NexusCompat.getRepositoryProviderRole(repository));

    resource.setFormat(repository.getRepositoryContentClass().getId());

    resource.setRepoType(getRestRepoType(repository));

    resource.setId(repository.getId());

    resource.setName(repository.getName());

    resource.setWritePolicy(repository.getWritePolicy().name());

    resource.setBrowseable(repository.isBrowseable());

    resource.setIndexable(repository.isSearchable());

    resource.setExposed(repository.isExposed());

    resource.setNotFoundCacheTTL(repository.getNotFoundCacheTimeToLive());

    // TODO: remove the default local storage, this is a work around for NEXUS-1994
    // the new 1.4 API doesn't store the default URL, well, it is part of the CRepo, but it is not exposed.
    // so we can figure it out again, I think the default local Storage should be removed from the REST message
    // which is part of the reason for not exposing it. The other part is it is not used anywhere except to set
    // the localUrl if not already set.

    // apples to apples here, man i hate this section of code!!!!
    // always set to default (see AbstractRepositoryConfigurator)
    String defaultLocalStorageUrl =
        ((AbstractRepository)repository).getCurrentCoreConfiguration()
            .getConfiguration(false).defaultLocalStorageUrl;
    resource.setDefaultLocalStorageUrl(defaultLocalStorageUrl);

    // if not user set (but using default), this is null, otherwise it contains user-set value
    String overrideLocalStorageUrl =
        ((AbstractRepository)repository).getCurrentCoreConfiguration().getConfiguration(false)
            .getLocalStorage().getUrl();
    if (StringUtils.isNotBlank(overrideLocalStorageUrl)) {
      resource.setOverrideLocalStorageUrl(overrideLocalStorageUrl);
    }

    if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
      resource.setRepoPolicy(repository.adaptToFacet(MavenRepository.class).getRepositoryPolicy().toString());

      if (repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class)) {
        resource.setChecksumPolicy(repository.adaptToFacet(MavenProxyRepository.class).getChecksumPolicy()
            .toString());

        resource.setDownloadRemoteIndexes(repository.adaptToFacet(MavenProxyRepository.class)
            .isDownloadRemoteIndexes());
      }
    }
    // as this is a required field on ui, we need this to be set for non-maven type repos
    else {
      resource.setRepoPolicy(RepositoryPolicy.MIXED.name());
      resource.setChecksumPolicy(ChecksumPolicy.IGNORE.name());
      resource.setDownloadRemoteIndexes(false);
    }

    return resource;

  }

  /**
   * Converting App model to REST DTO.
   */
  public RepositoryProxyResource getRepositoryProxyRestModel(ProxyRepository repository) {
    RepositoryProxyResource resource = new RepositoryProxyResource();

    resource.setRemoteStorage(new RepositoryResourceRemoteStorage());

    resource.getRemoteStorage().setRemoteStorageUrl(repository.getRemoteUrl());

    resource.getRemoteStorage().setAuthentication(
        AbstractGlobalConfigurationPlexusResource.convert(NexusCompat.getRepositoryRawConfiguration(repository)
            .getRemoteStorage().getAuthentication()));

    resource.getRemoteStorage().setConnectionSettings(
        AbstractGlobalConfigurationPlexusResource.convert(NexusCompat.getRepositoryRawConfiguration(repository)
            .getRemoteStorage().getConnectionSettings()));

    // set auto block
    resource.setAutoBlockActive(repository.isAutoBlockActive());

    // set content validation
    resource.setFileTypeValidation(repository.isFileTypeValidation());

    if (repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class)) {
      resource.setArtifactMaxAge(repository.adaptToFacet(MavenProxyRepository.class).getArtifactMaxAge());

      resource.setMetadataMaxAge(repository.adaptToFacet(MavenProxyRepository.class).getMetadataMaxAge());

      resource.setItemMaxAge(repository.adaptToFacet(MavenProxyRepository.class).getItemMaxAge());
    }
    else {
      // This is a total hack to be able to retrieve this data from a non core repo if available
      try {

        // NXCM-5131 Ask for itemMaxAge first, because it's already introduced in AbstractProxyRepository and
        // may be a superclass for non-maven repositories (e.g. NuGet)
        Method itemMethod = repository.getClass().getMethod("getItemMaxAge", new Class<?>[0]);
        if (itemMethod != null) {
          resource.setItemMaxAge((Integer) itemMethod.invoke(repository, new Object[0]));
        }

        Method artifactMethod = repository.getClass().getMethod("getArtifactMaxAge", new Class<?>[0]);
        if (artifactMethod != null) {
          resource.setArtifactMaxAge((Integer) artifactMethod.invoke(repository, new Object[0]));
        }

        Method metadataMethod = repository.getClass().getMethod("getMetadataMaxAge", new Class<?>[0]);
        if (metadataMethod != null) {
          resource.setMetadataMaxAge((Integer) metadataMethod.invoke(repository, new Object[0]));
        }
      }
      catch (Exception e) {
        // nothing to do here, doesn't support artifactmax age
      }
    }

    return resource;
  }

  public RepositoryShadowResource getRepositoryShadowRestModel(Request request, ShadowRepository shadow) {
    RepositoryShadowResource resource = new RepositoryShadowResource();

    resource.setId(shadow.getId());

    resource.setName(shadow.getName());

    resource.setContentResourceURI(repositoryURLBuilder.getExposedRepositoryContentUrl(shadow));

    resource.setProvider(NexusCompat.getRepositoryProviderHint(shadow));

    resource.setRepoType(RepositoryBaseResourceConverter.REPO_TYPE_VIRTUAL);

    resource.setFormat(shadow.getRepositoryContentClass().getId());

    resource.setShadowOf(shadow.getMasterRepository().getId());

    resource.setSyncAtStartup(shadow.isSynchronizeAtStartup());

    resource.setExposed(shadow.isExposed());

    return resource;
  }

  protected CRemoteAuthentication convertAuthentication(AuthenticationSettings authentication, String oldPassword) {
    if (authentication == null) {
      return null;
    }

    CRemoteAuthentication appModelSettings = new CRemoteAuthentication();
    appModelSettings.setUsername(authentication.getUsername());
    appModelSettings.setPassword(this.getActualPassword(authentication.getPassword(), oldPassword));
    appModelSettings.setNtlmDomain(authentication.getNtlmDomain());
    appModelSettings.setNtlmHost(authentication.getNtlmHost());

    return appModelSettings;
  }

  protected CRemoteConnectionSettings convertRemoteConnectionSettings(
      RemoteConnectionSettings remoteConnectionSettings)
  {
    if (remoteConnectionSettings == null) {
      return null;
    }

    CRemoteConnectionSettings cRemoteConnectionSettings = new CRemoteConnectionSettings();

    cRemoteConnectionSettings.setConnectionTimeout(remoteConnectionSettings.getConnectionTimeout() * 1000);

    cRemoteConnectionSettings.setQueryString(remoteConnectionSettings.getQueryString());

    cRemoteConnectionSettings.setRetrievalRetryCount(remoteConnectionSettings.getRetrievalRetryCount());

    cRemoteConnectionSettings.setUserAgentCustomizationString(remoteConnectionSettings.getUserAgentString());

    return cRemoteConnectionSettings;
  }
}