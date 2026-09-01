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
package org.sonatype.nexus.onboarding.internal;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.validation.Validate;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import jakarta.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.stereotype.Component;

/**
 * @since 3.17
 */
@Component
@Path(OnboardingResource.RESOURCE_URI)
public class OnboardingResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String PASSWORD_REQUIRED = "password is a required field, and cannot be empty.";

  public static final String RESOURCE_URI = "internal/ui/onboarding";

  private final OnboardingManager onboardingManager;

  private final SecuritySystem securitySystem;

  private final AdminPasswordFileManager adminPasswordFileManager;

  private final OnboardingSessionRefresher sessionRefresher;

  @Autowired
  public OnboardingResource(
      final OnboardingManager onboardingManager,
      final SecuritySystem securitySystem,
      final AdminPasswordFileManager adminPasswordFileManager,
      final OnboardingSessionRefresher sessionRefresher)
  {
    this.onboardingManager = checkNotNull(onboardingManager);
    this.securitySystem = checkNotNull(securitySystem);
    this.adminPasswordFileManager = checkNotNull(adminPasswordFileManager);
    this.sessionRefresher = checkNotNull(sessionRefresher);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Produces(APPLICATION_JSON)
  public List<OnboardingItem> getOnboardingItems() {
    return onboardingManager.getOnboardingItems();
  }

  /**
   * Change password of the default admin user, for administrative use only.
   * Only permitted while onboarding is still in progress.
   *
   * @param password new password
   */
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Path("/change-admin-password")
  @Validate
  public void changeAdminPassword(
      @NotEmpty(message = PASSWORD_REQUIRED) final String password,
      @Context final HttpServletRequest request,
      @Context final HttpServletResponse response)
  {
    // needsOnboarding() returns false both when onboarding is complete and when it
    // is administratively disabled — block in either case to prevent unsanctioned
    // password replacement outside the onboarding flow.
    if (!onboardingManager.needsOnboarding()) {
      log.warn("Rejecting change-admin-password request: onboarding already complete");
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    try {
      securitySystem.changePassword("admin", password, true);
      adminPasswordFileManager.removeFile();
      // Keep the caller's session alive; other admin sessions remain invalidated by
      // SecuritySystem.changePassword's internal SessionInvalidator call.
      sessionRefresher.refreshIfSelfChange("admin", password, request, response);
    }
    catch (UserNotFoundException e) {
      log.error("Unable to locate 'admin' user to change password", e);
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
  }
}
