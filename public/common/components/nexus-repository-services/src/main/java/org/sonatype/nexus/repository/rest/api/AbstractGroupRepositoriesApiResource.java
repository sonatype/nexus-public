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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.rest.GroupRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupDeployAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.repository.security.GroupMemberAuthorizationChecker;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

/**
 * @since 3.20
 */
public abstract class AbstractGroupRepositoriesApiResource<T extends GroupRepositoryApiRequest>
    extends AbstractRepositoriesApiResource<T>
{
  private static final String WRITABLE_MEMBER = "writableMember";

  private ConstraintViolationFactory constraintViolationFactory;

  private ApplicationVersion applicationVersion;

  private GroupMemberAuthorizationChecker groupMemberAuthorizationChecker;

  protected GroupRepositoryApiRequestToConfigurationConverter<T> configurationConverter;

  @Autowired
  public void setConstraintViolationFactory(final ConstraintViolationFactory constraintViolationFactory) {
    this.constraintViolationFactory = requireNonNull(constraintViolationFactory);
  }

  @Autowired
  public void setGroupMemberAuthorizationChecker(
      final GroupMemberAuthorizationChecker groupMemberAuthorizationChecker)
  {
    this.groupMemberAuthorizationChecker = requireNonNull(groupMemberAuthorizationChecker);
  }

  @Autowired
  public void setApplicationVersion(final ApplicationVersion applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  @Autowired
  public void setConfigurationConverter(
      final GroupRepositoryApiRequestToConfigurationConverter<T> configurationConverter)
  {
    this.configurationConverter = configurationConverter;
  }

  @Override
  public Configuration adapt(final T request) {
    return configurationConverter.convert(request);
  }

  @POST
  @Override
  public Response createRepository(final T request) {
    return super.createRepository(request);
  }

  @PUT
  @Path("/{repositoryName}")
  @Override
  public Response updateRepository(
      final T request,
      @PathParam("repositoryName") final String repositoryName)
  {
    return super.updateRepository(request, repositoryName);
  }

  @Override
  protected void validateRequest(final T request) {
    super.validateRequest(request);

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    validateMembers(request, violations);
    validateGroupDeploy(request, violations);
    maybePropagate(violations, log);
  }

  /**
   * Validates each requested group member: it must exist, match the group's format, and - for members not already
   * stored on the group - be readable or browsable by the caller. Members already present are grandfathered so that
   * existing group configurations remain editable. All failures are collected onto {@code memberNames}. The
   * authorization portion is delegated to {@link GroupMemberAuthorizationChecker}, shared with the UI write paths.
   */
  private void validateMembers(final T request, final Set<ConstraintViolation<?>> violations) {
    GroupAttributes groupAttributes = request.getGroup();
    if (groupAttributes == null || groupAttributes.getMemberNames() == null) {
      return;
    }
    String groupFormat = request.getFormat();
    List<String> existingFormatMatchedMembers = new ArrayList<>();
    for (String memberName : groupAttributes.getMemberNames()) {
      if (memberExistsWithMatchingFormat(memberName, groupFormat, violations)) {
        existingFormatMatchedMembers.add(memberName);
      }
    }
    // Grandfathering uses request.getName() to identify the group's already-stored members. On update this is the
    // request BODY name, not yet checked against the {repositoryName} path param at this point - the match is enforced
    // later by AbstractRepositoriesApiResource.ensureRepositoryNameMatches, which rejects any mismatch BEFORE the
    // configuration is persisted. So a body name pointing at a different group cannot persist a grandfathering bypass.
    // If that ordering is ever changed (name validated after persistence, or an early-return that skips it), this
    // becomes exploitable - keep the name check ahead of the write.
    //
    // A single generic violation rather than one naming each member: distinct per-member authorization messages would
    // let an unprivileged caller read which repositories they cannot access off the 400 body (information oracle,
    // NEXUS-53747). Existence and format mismatches above are pre-existing named messages and unchanged.
    if (!groupMemberAuthorizationChecker.findUnauthorizedNewMembers(request.getName(), existingFormatMatchedMembers)
        .isEmpty()) {
      violations.add(memberViolation("Not authorized to add one or more of the requested member repositories."));
    }
  }

  private boolean memberExistsWithMatchingFormat(
      final String memberName,
      final String groupFormat,
      final Set<ConstraintViolation<?>> violations)
  {
    Repository member = repositoryManager.get(memberName);
    if (member == null) {
      violations.add(memberViolation("Member repository does not exist: " + memberName));
      return false;
    }
    if (!member.getFormat().getValue().equals(groupFormat)) {
      violations.add(memberViolation("Member repository format does not match group repository format: " + memberName));
      return false;
    }
    return true;
  }

  private ConstraintViolation<?> memberViolation(final String message) {
    return constraintViolationFactory.createViolation("memberNames", message);
  }

  private void validateGroupDeploy(final T request, final Set<ConstraintViolation<?>> violations) {
    GroupAttributes group = request.getGroup();
    if (group instanceof GroupDeployAttributes) {
      GroupDeployAttributes groupDeployAttributes = (GroupDeployAttributes) group;
      final String writableMember = groupDeployAttributes.getWritableMember();
      if (writableMember == null || writableMember.isEmpty()) {
        return;
      }
      Repository writableMemberRepository = repositoryManager.get(writableMember);
      if (!applicationVersion.isAnyProEdition()) {
        violations.add(constraintViolationFactory.createViolation(
            WRITABLE_MEMBER,
            GroupHandler.INSUFFICIENT_LICENSE));
      }
      else if (writableMemberRepository == null) {
        violations.add(constraintViolationFactory.createViolation(
            WRITABLE_MEMBER,
            "Writable member repository does not exist"));
      }
      else if (!writableMemberRepository.getType().getValue().equals(HostedType.NAME)) {
        violations.add(constraintViolationFactory.createViolation(
            WRITABLE_MEMBER,
            "Writable member must be a hosted repository"));
      }
      else if (!writableMemberRepository.getFormat().getValue().equals(request.getFormat())) {
        violations.add(constraintViolationFactory.createViolation(
            WRITABLE_MEMBER,
            "Writable member repository format does not match group repository format: " + writableMember));
      }
      else if (!request.getGroup().getMemberNames().contains(writableMember)) {
        violations.add(constraintViolationFactory.createViolation(
            WRITABLE_MEMBER,
            "Writable member must be a member of the group"));
      }
    }
  }

  @GET
  @Path("/{repositoryName}")
  @Operation(summary = "Get repository")
  @Override
  public AbstractApiRepository getRepository(
      @Parameter(hidden = true) @BeanParam final FormatAndType formatAndType,
      @PathParam("repositoryName") final String repositoryName)
  {
    return super.getRepository(formatAndType, repositoryName);
  }
}
