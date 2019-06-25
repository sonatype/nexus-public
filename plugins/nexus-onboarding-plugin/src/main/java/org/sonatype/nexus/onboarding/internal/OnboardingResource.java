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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.onboarding.OnboardingManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.validation.Validate;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.17
 */
@Singleton
@Named
@Path(OnboardingResource.RESOURCE_URI)
public class OnboardingResource
    extends ComponentSupport
    implements Resource
{
  public static final String PASSWORD_REQUIRED = "password is a required field, and cannot be empty.";
  public static final String RESOURCE_URI = "internal/ui/onboarding";

  private final OnboardingManager onboardingManager;

  private final SecuritySystem securitySystem;

  private final AdminPasswordFileManager adminPasswordFileManager;

  @Inject
  public OnboardingResource(final OnboardingManager onboardingManager,
                            final SecuritySystem securitySystem,
                            final AdminPasswordFileManager adminPasswordFileManager)
  {
    this.onboardingManager = checkNotNull(onboardingManager);
    this.securitySystem = checkNotNull(securitySystem);
    this.adminPasswordFileManager = checkNotNull(adminPasswordFileManager);
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
   *
   * @param password new password
   */
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Path("/change-admin-password")
  @Validate
  public void changeAdminPassword(@NotEmpty(message = PASSWORD_REQUIRED) final String password) {
    try {
      securitySystem.changePassword("admin", password, false);
      adminPasswordFileManager.removeFile();
    }
    catch (UserNotFoundException e) {
      log.error("Unable to locate 'admin' user to change password", e);
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
  }
}
