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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.storage.remote.RemoteProviderHintFactory;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.rest.util.EnumUtil;
import org.sonatype.nexus.templates.repository.DefaultRepositoryTemplateProvider;
import org.sonatype.nexus.templates.repository.ManuallyConfiguredRepositoryTemplate;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * A resource list for Repository list.
 *
 * @author cstamas
 */
@Named
@Singleton
@Path(RepositoryListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public class RepositoryListPlexusResource
    extends AbstractRepositoryPlexusResource
{
  public static final String RESOURCE_URI = "/repositories";

  private final RemoteProviderHintFactory remoteProviderHintFactory;

  // UGLY HACK, SEE BELOW
  private final DefaultRepositoryTemplateProvider repositoryTemplateProvider;

  @Inject
  public RepositoryListPlexusResource(final RemoteProviderHintFactory remoteProviderHintFactory,
                                      final DefaultRepositoryTemplateProvider repositoryTemplateProvider)
  {
    this.remoteProviderHintFactory = remoteProviderHintFactory;
    this.repositoryTemplateProvider = repositoryTemplateProvider;
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new RepositoryResourceResponse();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:repositories]");
  }

  /**
   * Get the list of user managed repositories.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = RepositoryListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return listRepositories(request, false, false);
  }

  /**
   * Add a new repository to nexus.
   */
  @Override
  @POST
  @ResourceMethodSignature(input = RepositoryResourceResponse.class, output = RepositoryResourceResponse.class)
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    RepositoryResourceResponse repoRequest = (RepositoryResourceResponse) payload;
    String repoId = null;

    if (repoRequest != null) {
      RepositoryBaseResource resource = repoRequest.getData();
      repoId = resource.getId();

      try {
        CRepository config = getRepositoryAppModel(resource, null);

        // UGLY HACK
        // This is all broken here, the conversions that happens (Repo REST DTO -> CRepo DTO -> Repo creation)
        // is simply damn too stupid.
        // All this should be removed, and do not use C* config classes anymore in REST API (see NEXUS-2505).
        // For now, this is a "backdoor", using manual template when we have a CRepo object.
        ManuallyConfiguredRepositoryTemplate template =
            repositoryTemplateProvider.createManuallyTemplate(new CRepositoryCoreConfiguration(
                repositoryTemplateProvider.getApplicationConfiguration(), config, null));

        template.create();

        getNexusConfiguration().saveConfiguration();
      }
      catch (ConfigurationException e) {
        handleConfigurationException(e);
      }
      catch (IOException e) {
        getLogger().warn("Got IO Exception!", e);

        throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
      }
    }

    return getRepositoryResourceResponse(request, repoId);
  }

  // --

  /**
   * Converting REST DTO + possible App model to App model. If app model is given, "update" happens, otherwise if
   * target is null, "create".
   *
   * @return app model, merged or created
   */
  public CRepository getRepositoryAppModel(RepositoryBaseResource resource, CRepository target)
      throws ResourceException
  {
    CRepository appModel = new DefaultCRepository();

    try {
      Xpp3Dom ex = null;

      appModel.setLocalStatus(LocalStatus.IN_SERVICE.name());
      if (target != null) {
        appModel.setLocalStatus(target.getLocalStatus());

        ex = (Xpp3Dom) target.getExternalConfiguration();
      }
      else {
        ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
      }

      appModel.setId(resource.getId());

      appModel.setName(resource.getName());

      appModel.setExposed(resource.isExposed());

      appModel.setProviderRole(resource.getProviderRole());

      if (RepositoryBaseResourceConverter.REPO_TYPE_VIRTUAL.equals(resource.getRepoType())) {
        appModel.setExternalConfiguration(ex);

        // indexer is unaware of the m2 layout conversion
        appModel.setIndexable(false);

        RepositoryShadowResource repoResource = (RepositoryShadowResource) resource;

        M2LayoutedM1ShadowRepositoryConfiguration exConf = new M2LayoutedM1ShadowRepositoryConfiguration(ex);

        exConf.setMasterRepositoryId(repoResource.getShadowOf());

        exConf.setSynchronizeAtStartup(repoResource.isSyncAtStartup());

      }
      else if (!RepositoryBaseResourceConverter.REPO_TYPE_GROUP.equals(resource.getRepoType())) {
        RepositoryResource repoResource = (RepositoryResource) resource;

        // we can use the default if the value is empty
        if (isNotEmpty(repoResource.getWritePolicy())) {
          appModel.setWritePolicy(repoResource.getWritePolicy());
        }

        appModel.setBrowseable(repoResource.isBrowseable());

        appModel.setIndexable(repoResource.isIndexable());
        appModel.setSearchable(repoResource.isIndexable());

        appModel.setNotFoundCacheTTL(repoResource.getNotFoundCacheTTL());

        appModel.setExternalConfiguration(ex);

        M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);

        exConf.setRepositoryPolicy(EnumUtil.valueOf(repoResource.getRepoPolicy(), RepositoryPolicy.class));

        if (isNotEmpty(repoResource.getOverrideLocalStorageUrl())) {
          appModel.setLocalStorage(new CLocalStorage());

          appModel.getLocalStorage().setUrl(validOverrideLocalStorageUrl(repoResource.getOverrideLocalStorageUrl()));

          appModel.getLocalStorage().setProvider("file");
        }
        else {
          appModel.setLocalStorage(null);
        }

        RepositoryResourceRemoteStorage remoteStorage = repoResource.getRemoteStorage();
        if (remoteStorage != null) {
          appModel.setNotFoundCacheActive(true);

          appModel.setRemoteStorage(new CRemoteStorage());

          appModel.getRemoteStorage().setUrl(remoteStorage.getRemoteStorageUrl());

          appModel.getRemoteStorage().setProvider(
              remoteProviderHintFactory.getDefaultRoleHint(remoteStorage.getRemoteStorageUrl()));
        }
      }

      appModel.setProviderHint(resource.getProvider());

      if (RepositoryProxyResource.class.isAssignableFrom(resource.getClass())) {
        appModel = getRepositoryProxyAppModel((RepositoryProxyResource) resource, appModel);
      }
    }
    catch (InvalidConfigurationException e) {
      handleConfigurationException(e);
    }

    return appModel;
  }

  /**
   * Converting REST DTO + possible App model to App model. If app model is given, "update" happens, otherwise if
   * target is null, "create".
   *
   * @return app model, merged or created
   */
  public CRepository getRepositoryProxyAppModel(RepositoryProxyResource model, CRepository target)
      throws PlexusResourceException
  {
    M2RepositoryConfiguration exConf = new M2RepositoryConfiguration((Xpp3Dom) target.getExternalConfiguration());

    if (model.getProvider().equals("maven2") || model.getProvider().equals("maven1")) {
      exConf.setChecksumPolicy(EnumUtil.valueOf(model.getChecksumPolicy(), ChecksumPolicy.class));
    }

    exConf.setFileTypeValidation(model.isFileTypeValidation());

    exConf.setDownloadRemoteIndex(model.isDownloadRemoteIndexes());

    exConf.setArtifactMaxAge(model.getArtifactMaxAge());

    exConf.setMetadataMaxAge(model.getMetadataMaxAge());

    if (model.getItemMaxAge() != null) {
      exConf.setItemMaxAge(model.getItemMaxAge());
    }

    // set auto block
    exConf.setAutoBlockActive(model.isAutoBlockActive());

    if (model.getRemoteStorage() != null) {
      if (target.getRemoteStorage() == null) {
        target.setRemoteStorage(new CRemoteStorage());
      }

      // url
      target.getRemoteStorage().setUrl(model.getRemoteStorage().getRemoteStorageUrl());

      // remote auth
      target.getRemoteStorage().setAuthentication(
          this.convertAuthentication(model.getRemoteStorage().getAuthentication(), null));

      // connection settings
      target.getRemoteStorage().setConnectionSettings(
          this.convertRemoteConnectionSettings(model.getRemoteStorage().getConnectionSettings()));
    }

    return target;
  }

}
