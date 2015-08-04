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
package org.sonatype.nexus.rest;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.templates.NoSuchTemplateIdException;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.plexus.rest.ReferenceFactory;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractNexusPlexusResource
    extends AbstractPlexusResource
    implements PlexusResource
{
  public static final String PASSWORD_PLACE_HOLDER = "|$|N|E|X|U|S|$|";

  public static final String IS_LOCAL_PARAMETER = "isLocal";

  public static final String IS_REMOTE_PARAMETER = "isRemote";

  public static final String AS_EXPIRED_PARAMETER = "asExpired";

  private NexusConfiguration nexusConfiguration;

  private RepositoryRegistry repositoryRegistry;

  private RepositoryRegistry defaultRepositoryRegistry;

  private ReferenceFactory referenceFactory;

  private TemplateManager templateManager;

  private RepositoryRouter repositoryRouter;

  @Inject
  public void setNexusConfiguration(final NexusConfiguration nexusConfiguration) {
    this.nexusConfiguration = checkNotNull(nexusConfiguration);
  }

  @Inject
  public void setRepositoryRegistry(final @Named("protected") RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Inject
  public void setDefaultRepositoryRegistry(final @Named("default") RepositoryRegistry repositoryRegistry) {
    this.defaultRepositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Inject
  public void setReferenceFactory(final ReferenceFactory referenceFactory) {
    this.referenceFactory = checkNotNull(referenceFactory);
  }

  @Inject
  public void setTemplateManager(final TemplateManager templateManager) {
    this.templateManager = checkNotNull(templateManager);
  }
  
  @Inject
  public void setRepositoryRouter(final RepositoryRouter repositoryRouter) {
    this.repositoryRouter = checkNotNull(repositoryRouter);
  }

  protected NexusConfiguration getNexusConfiguration() {
    return nexusConfiguration;
  }

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  protected RepositoryRegistry getUnprotectedRepositoryRegistry() {
    return defaultRepositoryRegistry;
  }
  
  protected TemplateManager getTemplateManager() {
    return templateManager;
  }
  
  protected RepositoryRouter getRepositoryRouter() {
    return repositoryRouter;
  }
  
  protected TemplateSet getRepositoryTemplates() {
    return getTemplateManager().getTemplates().getTemplates(RepositoryTemplate.class);
  }

  protected RepositoryTemplate getRepositoryTemplateById(String id)
      throws NoSuchTemplateIdException
  {
    return (RepositoryTemplate) getTemplateManager().getTemplate(RepositoryTemplate.class, id);
  }

  /**
   * Centralized, since this is the only "dependent" stuff that relies on knowledge where restlet.Application is
   * mounted (we had a /service => / move).
   */
  protected Reference getContextRoot(Request request) {
    return this.referenceFactory.getContextRoot(request);
  }

  protected boolean isLocal(Request request, String resourceStorePath) {
    // check do we need local only access
    boolean isLocal = request.getResourceRef().getQueryAsForm().getFirst(IS_LOCAL_PARAMETER) != null;

    if (resourceStorePath != null) {
      // overriding isLocal is we know it will be a collection
      isLocal = isLocal || resourceStorePath.endsWith(RepositoryItemUid.PATH_SEPARATOR);
    }

    return isLocal;
  }

  protected boolean isRemote(Request request, String resourceStorePath) {
    // check do we need remote only access
    return request.getResourceRef().getQueryAsForm().getFirst(IS_REMOTE_PARAMETER) != null;
  }

  protected boolean asExpired(Request request, String resourceStorePath) {
    // check do we need expired access
    return request.getResourceRef().getQueryAsForm().getFirst(AS_EXPIRED_PARAMETER) != null;
  }

  private Reference updateBaseRefPath(Reference reference) {
    if (reference.getBaseRef().getPath() == null) {
      reference.getBaseRef().setPath("/");
    }
    else if (!reference.getBaseRef().getPath().endsWith("/")) {
      reference.getBaseRef().setPath(reference.getBaseRef().getPath() + "/");
    }

    return reference;
  }

  protected Reference createReference(Reference base, String relPart) {
    Reference ref = new Reference(base, relPart);

    return updateBaseRefPath(ref).getTargetRef();
  }

  protected Reference createChildReference(Request request, PlexusResource resource, String childPath) {
    return this.referenceFactory.createChildReference(request, childPath);
  }

  protected Reference createRootReference(Request request, String relPart) {
    Reference ref = new Reference(getContextRoot(request), relPart);

    if (!ref.getBaseRef().getPath().endsWith("/")) {
      ref.getBaseRef().setPath(ref.getBaseRef().getPath() + "/");
    }

    return ref.getTargetRef();
  }

  protected Reference createRepositoryReference(Request request, String repoId) {
    return createReference(getContextRoot(request), "service/local/repositories/" + repoId).getTargetRef();
  }

  protected Reference createRepositoryReference(Request request, String repoId, String repoPath) {
    Reference repoRootRef = createRepositoryReference(request, repoId);

    if (repoPath.startsWith(RepositoryItemUid.PATH_SEPARATOR)) {
      repoPath = repoPath.substring(1);
    }

    repoPath = "content/" + repoPath;

    return createReference(repoRootRef, repoPath);
  }

  protected Reference createRepositoryGroupReference(Request request, String groupId) {
    return createReference(getContextRoot(request), "service/local/repo_groups/" + groupId).getTargetRef();
  }

  protected Reference createRedirectReference(Request request) {
    String uriPart =
        request.getResourceRef().getTargetRef().toString().substring(
            getContextRoot(request).getTargetRef().toString().length());

    // trim leading slash
    if (uriPart.startsWith("/")) {
      uriPart = uriPart.substring(1);
    }

    return updateBaseRefPath(new Reference(getContextRoot(request), uriPart)).getTargetRef();
  }

  // ===

  protected ErrorResponse getNexusErrorResponse(String id, String msg) {
    ErrorResponse ner = new ErrorResponse();
    ErrorMessage ne = new ErrorMessage();
    ne.setId(id);
    ne.setMsg(StringEscapeUtils.escapeHtml(msg));
    ner.addError(ne);
    return ner;
  }

  protected void handleInvalidConfigurationException(InvalidConfigurationException e)
      throws PlexusResourceException
  {
    getLogger().debug("Configuration error!", e);

    ErrorResponse nexusErrorResponse;

    ValidationResponse vr = e.getValidationResponse();

    if (vr != null && vr.getValidationErrors().size() > 0) {
      org.sonatype.configuration.validation.ValidationMessage vm = vr.getValidationErrors().get(0);
      nexusErrorResponse = getNexusErrorResponse(vm.getKey(), vm.getShortMessage());
    }
    else {
      nexusErrorResponse = getNexusErrorResponse("*", e.getMessage());
    }

    throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", nexusErrorResponse);
  }

  protected void handleConfigurationException(ConfigurationException e)
      throws PlexusResourceException
  {
    getLogger().debug("Configuration error!", e);

    ErrorResponse nexusErrorResponse;

    if (InvalidConfigurationException.class.isAssignableFrom(e.getClass())) {
      ValidationResponse vr = ((InvalidConfigurationException) e).getValidationResponse();

      if (vr != null && vr.getValidationErrors().size() > 0) {
        ValidationMessage vm = vr.getValidationErrors().get(0);
        nexusErrorResponse = getNexusErrorResponse(vm.getKey(), vm.getShortMessage());
      }
      else {
        nexusErrorResponse = getNexusErrorResponse("*", e.getMessage());
      }
    }
    else {
      nexusErrorResponse = getNexusErrorResponse("*", e.getMessage());
    }

    throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Configuration error.", nexusErrorResponse);
  }

  protected Reference createRedirectBaseRef(Request request) {
    return createReference(getContextRoot(request), "service/local/artifact/maven/redirect").getTargetRef();
  }

  protected String getValidRemoteIPAddress(Request request) {
    return RemoteIPFinder.findIP(request);
  }

  protected String getActualPassword(String newPassword, String oldPassword) {
    if (PASSWORD_PLACE_HOLDER.equals(newPassword)) {
      return oldPassword;
    }

    return newPassword;
  }
}
