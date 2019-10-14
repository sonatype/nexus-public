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

import java.util.StringJoiner;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @since 3.next
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public abstract class AbstractRepositoriesApiResource<T extends AbstractRepositoryApiRequest>
    extends ComponentSupport
    implements Resource
{
  protected final AuthorizingRepositoryManager authorizingRepositoryManager;

  protected final Provider<Validator> validatorProvider;

  protected final AbstractRepositoryApiRequestToConfigurationConverter<T> configurationAdapter;

  @Inject
  public AbstractRepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final Provider<Validator> validatorProvider,
      final AbstractRepositoryApiRequestToConfigurationConverter<T> configurationAdapter)
  {
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.validatorProvider = checkNotNull(validatorProvider);
    this.configurationAdapter = checkNotNull(configurationAdapter);
  }

  protected Response createRepository(final T request) {
    try {
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

  protected boolean updateRepository(final T request) {
    try {
      return authorizingRepositoryManager.update(configurationAdapter.convert(request));
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
}
