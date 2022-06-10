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
package org.sonatype.nexus.security.privilege.rest;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.ReadonlyPrivilegeException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

public abstract class PrivilegeApiResourceSupport
  extends ComponentSupport
{
  public static final String PRIV_NOT_FOUND = "\"Privilege '%s' not found.\"";

  public static final String PRIV_INTERNAL = "\"Privilege '%s' is internal and cannot be modified or deleted.\"";

  public static final String PRIV_UNIQUE = "\"Privilege '%s' already exists, use a unique name.\"";

  public static final String PRIV_CONFLICT = "\"The privilege name '%s' does not match the name used in the path '%s'.\"";

  private final SecuritySystem securitySystem;

  private final Map<String, PrivilegeDescriptor> privilegeDescriptors;

  public PrivilegeApiResourceSupport(final SecuritySystem securitySystem,
                                     final Map<String, PrivilegeDescriptor> privilegeDescriptors)
  {
    this.securitySystem = checkNotNull(securitySystem);
    this.privilegeDescriptors = checkNotNull(privilegeDescriptors);
  }

  protected Response doCreate(String type, ApiPrivilegeRequest apiPrivilege) {
    try {
      PrivilegeDescriptor privilegeDescriptor = privilegeDescriptors.get(type);
      privilegeDescriptor.validate(apiPrivilege);

      getDefaultAuthorizationManager().addPrivilege(apiPrivilege.asPrivilege());
      return Response.status(Status.CREATED).build();
    }
    catch (DuplicatePrivilegeException e) {
      log.debug("Attempt to create privilege '{}' failed, the name is already in use.", apiPrivilege.getName(), e);
      throw new WebApplicationMessageException(Status.BAD_REQUEST, String.format(PRIV_UNIQUE, apiPrivilege.getName()),
          MediaType.APPLICATION_JSON);
    }
  }

  protected void doUpdate(String privilegeName, String type, ApiPrivilegeRequest apiPrivilege) {
    try {
      if (!apiPrivilege.getName().equals(privilegeName)) {
        throw new WebApplicationMessageException(Status.CONFLICT,
            String.format(PRIV_CONFLICT, apiPrivilege.getName(), privilegeName), MediaType.APPLICATION_JSON);
      }

      PrivilegeDescriptor privilegeDescriptor = privilegeDescriptors.get(type);
      privilegeDescriptor.validate(apiPrivilege);

      AuthorizationManager authorizationManager = getDefaultAuthorizationManager();
      Privilege privilege = authorizationManager.getPrivilegeByName(privilegeName);
      Privilege newPrivilege = apiPrivilege.asPrivilege();
      privilege.setDescription(newPrivilege.getDescription());
      privilege.setProperties(newPrivilege.getProperties());
      authorizationManager.updatePrivilegeByName(privilege);
    }
    catch (NoSuchPrivilegeException e) {
      log.debug("Attempt to update privilege '{}' failed, as it wasn't found in the system.", privilegeName, e);
      throw new WebApplicationMessageException(Status.NOT_FOUND, String.format(PRIV_NOT_FOUND, privilegeName),
          MediaType.APPLICATION_JSON);
    }
    catch (ReadonlyPrivilegeException e) {
      log.debug("Attempt to update internal privilege '{}' failed.", privilegeName, e);
      throw new WebApplicationMessageException(Status.BAD_REQUEST, String.format(PRIV_INTERNAL, privilegeName),
          MediaType.APPLICATION_JSON);
    }
  }

  protected ApiPrivilege toApiPrivilege(Privilege privilege) {
    if (privilege == null) {
      return null;
    }

    PrivilegeDescriptor privilegeDescriptor = privilegeDescriptors.get(privilege.getType());

    if (privilegeDescriptor == null) {
      return null;
    }

    ApiPrivilege apiPrivilege = privilegeDescriptor.createApiPrivilegeImpl(privilege);

    return apiPrivilege;
  }

  protected SecuritySystem getSecuritySystem() {
    return securitySystem;
  }

  protected AuthorizationManager getDefaultAuthorizationManager() {
    try {
      return securitySystem.getAuthorizationManager(DEFAULT_SOURCE);
    }
    //this should never happen, the default source is always available
    catch (NoSuchAuthorizationManagerException e) {
      log.error("Unable to retrieve the default authorization manager", e);
      return null;
    }
  }
}
