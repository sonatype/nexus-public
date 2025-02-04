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
package org.sonatype.nexus.repository.rest.api;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.HighAvailabilitySupportChecker;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.validation.Validate;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.sonatype.nexus.rest.ApiDocConstants.BLOBSTORE_CHANGE_NOT_ALLOWED;
import static org.sonatype.nexus.rest.ApiDocConstants.BLOBSTORE_NOT_FOUND;

/**
 * @since 3.20
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public abstract class AbstractRepositoriesApiResource<T extends AbstractRepositoryApiRequest>
    extends ComponentSupport
    implements Resource
{
  private AuthorizingRepositoryManager authorizingRepositoryManager;

  protected RepositoryManager repositoryManager;

  protected BlobStoreManager blobStoreManager;

  private AbstractRepositoryApiRequestToConfigurationConverter<T> configurationAdapter;

  private Map<String, ApiRepositoryAdapter> convertersByFormat;

  private ApiRepositoryAdapter defaultAdapter;

  protected HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  private Map<String, Recipe> recipesByFormat;

  @Inject
  public void setHighAvailabilitySupportChecker(final HighAvailabilitySupportChecker highAvailabilitySupportChecker) {
    this.highAvailabilitySupportChecker = highAvailabilitySupportChecker;
  }

  @Inject
  public void setAuthorizingRepositoryManager(final AuthorizingRepositoryManager authorizingRepositoryManager) {
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
  }

  @Inject
  public void setRepositoryManager(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Inject
  public void setBlobStoreManager(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = blobStoreManager;
  }

  @Inject
  public void setConfigurationAdapter(
      final AbstractRepositoryApiRequestToConfigurationConverter<T> configurationAdapter)
  {
    this.configurationAdapter = checkNotNull(configurationAdapter);
  }

  @Inject
  public void setConvertersByFormat(final Map<String, ApiRepositoryAdapter> convertersByFormat) {
    this.convertersByFormat = checkNotNull(convertersByFormat);
  }

  @Inject
  public void setDefaultAdapter(final ApiRepositoryAdapter defaultAdapter) {
    this.defaultAdapter = checkNotNull(defaultAdapter);
  }

  @Inject
  public void setRecipesByFormat(final Map<String, Recipe> recipesByFormat) {
    this.recipesByFormat = checkNotNull(recipesByFormat);
  }

  @POST
  @RequiresAuthentication
  @Validate
  public Response createRepository(@NotNull @Valid final T request) {
    verifyAPIEnabled(request.getFormat());
    try {
      Configuration configuration = configurationAdapter.convert(request);
      validateRequest(configuration, configuration.getRepositoryName());
      authorizingRepositoryManager.create(configurationAdapter.convert(request));
      return Response.status(Status.CREATED).build();
    }
    catch (AuthorizationException | AuthenticationException | ConstraintViolationException e) {
      throw e;
    }
    catch (Exception e) {
      StringJoiner stringJoiner = new StringJoiner("\n", "\"", "\"");
      stringJoiner.add(e.getMessage());
      for (Throwable t : e.getSuppressed()) {
        stringJoiner.add(t.getMessage());
      }
      String message = stringJoiner.toString();
      log.debug("Failed to create a new repository via REST: {}", message, e);
      throw new WebApplicationMessageException(BAD_REQUEST, message, APPLICATION_JSON);
    }
  }

  @PUT
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  public Response updateRepository(
      @NotNull @Valid final T request,
      @PathParam("repositoryName") final String repositoryName)
  {
    try {
      Configuration newConfiguration = configurationAdapter.convert(request);
      validateRequest(newConfiguration, repositoryName);
      boolean updated = authorizingRepositoryManager.update(newConfiguration);

      Status status = updated ? Status.NO_CONTENT : Status.NOT_FOUND;
      return Response.status(status).build();
    }
    catch (AuthorizationException | AuthenticationException | ConstraintViolationException e) {
      throw e;
    }
    catch (Exception e) {
      StringJoiner stringJoiner = new StringJoiner("\n", "\"", "\"");
      stringJoiner.add(e.getMessage());
      for (Throwable t : e.getSuppressed()) {
        stringJoiner.add(t.getMessage());
      }
      String message = stringJoiner.toString();
      log.debug("Failed to edit a repository via REST: {}", message, e);

      throw new WebApplicationMessageException(BAD_REQUEST, message, APPLICATION_JSON);
    }
  }

  @GET
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  @ApiOperation("Get repository")
  public AbstractApiRepository getRepository(
      @ApiParam(hidden = true) @BeanParam final FormatAndType formatAndType,
      @PathParam("repositoryName") final String repositoryName)
  {
    return authorizingRepositoryManager.getRepositoryWithAdmin(repositoryName)
        .filter(r -> r.getType().getValue().equals(formatAndType.type()) &&
            r.getFormat().getValue().equals(formatAndType.format()))
        .map(r -> convertersByFormat.getOrDefault(r.getFormat().toString(), defaultAdapter).adapt(r))
        .orElseThrow(() -> new WebApplicationMessageException(NOT_FOUND, "\"Repository not found\"", APPLICATION_JSON));
  }

  /**
   * By default, the API is enabled in High Availability, otherwise it should be overridden by a format.
   * 
   * @return {@code true} in case of API is enabled or {@code false} otherwise.
   */
  public boolean isApiEnabled() {
    return true;
  }

  private void validateRequest(final Configuration newConfig, final String repositoryName) {
    validateFormatEnabled(newConfig.getRecipeName());
    ensureRepositoryNameMatches(newConfig, repositoryName);
    validateBlobStoreName(newConfig, repositoryName);
  }

  private void ensureRepositoryNameMatches(final Configuration newConfig, final String repositoryName) {
    if (!repositoryName.equals(newConfig.getRepositoryName())) {
      throw new ValidationErrorsException("name", "Renaming a repository is not supported");
    }
  }

  private void validateBlobStoreName(final Configuration newConfiguration, final String repositoryName) {
    String blobStoreName = (String) newConfiguration.getAttributes().get("storage").get("blobStoreName");
    if (!blobStoreManager.exists(blobStoreName)) {
      throw new ValidationErrorsException("BlobStoreName", BLOBSTORE_NOT_FOUND);
    }

    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      return;
    }

    Function<Map<String, Map<String, Object>>, String> getBlobStorageFromAttributes =
        storageAttr -> (String) storageAttr.get("storage").get("blobStoreName");

    String newBlobStoreName = Optional.ofNullable(newConfiguration.getAttributes())
        .map(getBlobStorageFromAttributes)
        .orElse("");
    String currentBlobStore = getBlobStorageFromAttributes.apply(repository.getConfiguration().getAttributes());

    if (!newBlobStoreName.equals(currentBlobStore)) {
      throw new ValidationErrorsException("BlobStoreName", BLOBSTORE_CHANGE_NOT_ALLOWED);
    }
  }

  private void verifyAPIEnabled(final String format) {
    if (!isApiEnabled()) {
      String message = String.format("Format %s is disabled in High Availability", format);
      throw new WebApplicationMessageException(METHOD_NOT_ALLOWED, message, APPLICATION_JSON);
    }
  }

  private void validateFormatEnabled(final String recipeName) {
    Recipe recipe = recipesByFormat.get(recipeName);
    if (recipe != null && !recipe.isFeatureEnabled()) {
      throw new ValidationErrorsException("This format is not currently enabled");
    }
  }
}
