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

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.GroupRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.validation.ConstraintViolationFactory;
import org.sonatype.nexus.validation.Validate;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

/**
 * @since 3.20
 */
public abstract class AbstractGroupRepositoriesApiResource<T extends GroupRepositoryApiRequest>
    extends AbstractRepositoriesApiResource<T>
{
  private final ConstraintViolationFactory constraintViolationFactory;

  private final RepositoryManager repositoryManager;

  @Inject
  public AbstractGroupRepositoriesApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final GroupRepositoryApiRequestToConfigurationConverter<T> configurationAdapter,
      final ConstraintViolationFactory constraintViolationFactory,
      final RepositoryManager repositoryManager)
  {
    super(authorizingRepositoryManager, configurationAdapter);
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @POST
  @RequiresAuthentication
  @Validate
  public Response createRepository(final T request) {
    validateGroupMembers(request);
    return super.createRepository(request);
  }

  @PUT
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  public Response updateRepository(
      final T request,
      @PathParam("repositoryName") final String repositoryName)
  {
    validateGroupMembers(request);
    return super.updateRepository(request, repositoryName);
  }

  private void validateGroupMembers(T request) {
    String groupFormat = request.getFormat();
    Set<ConstraintViolation<?>> violations = Sets.newHashSet();
    Collection<String> memberNames = request.getGroup().getMemberNames();
    for (String repositoryName : memberNames) {
      Repository repository = repositoryManager.get(repositoryName);
      if (nonNull(repository)) {
        String memberFormat = repository.getFormat().getValue();
        if (!memberFormat.equals(groupFormat)) {
          violations.add(constraintViolationFactory.createViolation("memberNames",
              "Member repository format does not match group repository format: " + repositoryName));
        }
      }
      else {
        violations.add(constraintViolationFactory.createViolation("memberNames",
            "Member repository does not exist: " + repositoryName));
      }
    }
    maybePropagate(violations, log);
  }
}
