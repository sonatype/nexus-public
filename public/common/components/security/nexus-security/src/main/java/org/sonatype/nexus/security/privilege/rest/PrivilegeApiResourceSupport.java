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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CPrivilegeBuilder;
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.ReadonlyPrivilegeException;

import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

public abstract class PrivilegeApiResourceSupport
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String PRIV_NOT_FOUND = "\"Privilege '%s' not found.\"";

  public static final String PRIV_INTERNAL = "\"Privilege '%s' is internal and cannot be modified or deleted.\"";

  public static final String PRIV_UNIQUE = "\"Privilege '%s' already exists, use a unique name.\"";

  public static final String PRIV_CONFLICT =
      "\"The privilege name '%s' does not match the name used in the path '%s'.\"";

  public static final String PRIV_FORBIDDEN =
      "\"The current user is not permitted to grant the effective permission of privilege '%s'.\"";

  public static final String PRIV_TYPE_MISMATCH =
      "\"Privilege '%s' is of type '%s' and cannot be updated via the '%s' endpoint.\"";

  private final SecuritySystem securitySystem;

  private final SecurityHelper securityHelper;

  private final Map<String, PrivilegeDescriptor> privilegeDescriptors;

  public PrivilegeApiResourceSupport(
      final SecuritySystem securitySystem,
      final SecurityHelper securityHelper,
      final List<PrivilegeDescriptor> privilegeDescriptorsList)
  {
    this.securitySystem = checkNotNull(securitySystem);
    this.securityHelper = checkNotNull(securityHelper);
    this.privilegeDescriptors = QualifierUtil.buildQualifierBeanMap(checkNotNull(privilegeDescriptorsList));
  }

  protected Response doCreate(String type, ApiPrivilegeRequest apiPrivilege) {
    try {
      PrivilegeDescriptor privilegeDescriptor = privilegeDescriptors.get(type);
      privilegeDescriptor.validate(apiPrivilege);

      Privilege privilege = apiPrivilege.asPrivilege();
      checkGrantRights(privilegeDescriptor, privilege);

      getDefaultAuthorizationManager().addPrivilege(privilege);
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

      AuthorizationManager authorizationManager = getDefaultAuthorizationManager();
      Privilege privilege = authorizationManager.getPrivilegeByName(privilegeName);
      if (!type.equals(privilege.getType())) {
        throw new WebApplicationMessageException(Status.CONFLICT,
            String.format(PRIV_TYPE_MISMATCH, privilegeName, privilege.getType(), type),
            MediaType.APPLICATION_JSON);
      }
      PrivilegeDescriptor privilegeDescriptor = privilegeDescriptors.get(type);
      privilegeDescriptor.validate(apiPrivilege);

      Privilege newPrivilege = apiPrivilege.asPrivilege();
      checkGrantRights(privilegeDescriptor, newPrivilege);

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

  private void checkGrantRights(final PrivilegeDescriptor privilegeDescriptor, final Privilege privilege) {
    Permission resolved = privilegeDescriptor.createPermission(toCPrivilege(privilege));
    checkNotNull(resolved, "PrivilegeDescriptor for type '%s' returned a null permission", privilege.getType());
    if (!securityHelper.allPermitted(resolved)) {
      log.debug(
          "Rejected privilege '{}' mutation: caller lacks the effective permission '{}' the privilege would grant.",
          privilege.getName(), resolved);
      throw new WebApplicationMessageException(Status.FORBIDDEN, String.format(PRIV_FORBIDDEN, privilege.getName()),
          MediaType.APPLICATION_JSON);
    }
  }

  private static CPrivilege toCPrivilege(final Privilege source) {
    CPrivilegeBuilder builder = new CPrivilegeBuilder()
        .type(source.getType())
        .id(source.getId())
        .name(source.getName())
        .description(source.getDescription() == null ? "" : source.getDescription())
        .readOnly(source.isReadOnly());
    if (source.getProperties() != null) {
      for (Map.Entry<String, String> entry : source.getProperties().entrySet()) {
        builder.property(entry.getKey(), entry.getValue());
      }
    }
    return builder.create();
  }

  protected ApiPrivilege toApiPrivilege(Privilege privilege) {
    if (privilege == null) {
      return null;
    }

    PrivilegeDescriptor privilegeDescriptor = privilegeDescriptors.get(privilege.getType());

    if (privilegeDescriptor == null) {
      log.warn("Skipping privilege '{}' with unknown or unsupported type '{}'", privilege.getName(),
          privilege.getType());
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
    // this should never happen, the default source is always available
    catch (NoSuchAuthorizationManagerException e) {
      log.error("Unable to retrieve the default authorization manager", e);
      return null;
    }
  }
}
