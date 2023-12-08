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
package org.sonatype.nexus.coreui.internal.atlas;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.coreui.internal.atlas.SecurityDiagnosticResource.RESOURCE_URI;

/**
 * Renders security diagnostic information.
 */
@Named
@Singleton
@Path(RESOURCE_URI)
@Produces(MediaType.APPLICATION_JSON)
public class SecurityDiagnosticResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/atlas/security-diagnostic";

  private static final String USER_FIELD = "user";

  private static final String USERID_FIELD = "userId";

  private static final String NAME_FIELD = "name";

  private static final String FIRST_NAME_FIELD = "firstName";

  private static final String LAST_NAME_FIELD = "lastName";

  private static final String EMAIL_FIELD = "emailAddress";

  private static final String DESCRIPTION_FIELD = "description";

  private static final String SOURCE_FIELD = "source";

  private static final String STATUS_FIELD = "status";

  private static final String VERSION_FIELD = "version";

  private static final String ROLES_FIELD = "roles";

  private static final String PRIVILEGES_FIELD = "privileges";

  private static final String PERMISSION_FIELD = "permission";

  private static final String PROPERTIES_FIELD = "properties";

  private final SecuritySystem securitySystem;

  @Inject
  public SecurityDiagnosticResource(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  /**
   * Renders security diagnostic information for a specific user.
   */
  @GET
  @Path("user/{userId}")
  @RequiresPermissions("nexus:atlas:read")
  public Map<String, Object> userDiagnostic(final @PathParam("userId") String userId) {
    try {
      log.info("Generating security diagnostics for user: {}", userId);
      Map<String, Object> userDataMap = new HashMap<>();
      populateUserDataMap(userDataMap, securitySystem.getAuthorizationManager("default"), userId);
      return userDataMap;
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.debug("Default AuthorizationManager not found", e);
      throw new RuntimeException(e);
    }
  }

  private void populateUserDataMap(
      final Map<String, Object> userDataMap,
      final AuthorizationManager authorizationManager,
      final String id)
  {
    try {
      userDataMap.put(USER_FIELD, toMap(securitySystem.getUser(id), authorizationManager));
    }
    catch (UserNotFoundException e) {
      log.debug("User not found: {}", id, e);
      throw new NotFoundException("User not found");
    }
  }

  private void populateRoleDataMap(
      final Map<String, Object> roleDataMap,
      final AuthorizationManager authorizationManager,
      final String id)
  {
    try {
      roleDataMap.put(id, toMap(authorizationManager.getRole(id), authorizationManager));
    }
    catch (NoSuchRoleException e) {
      roleDataMap.put(id, "ERROR: Failed to resolve role: " + id + " caused by: " + e.getMessage());
    }
  }

  private void populatePrivilegeDataMap(
      final Map<String, Object> privilegeDataMap,
      final AuthorizationManager authorizationManager,
      final String id)
  {
    try {
      privilegeDataMap.put(id, toMap(authorizationManager.getPrivilege(id)));
    }
    catch (NoSuchPrivilegeException e) {
      privilegeDataMap.put(id, "ERROR: Failed to resolve privilege: " + id + " caused by: " + e.getMessage());
    }
  }

  private Map<String, Object> toMap(final User user, final AuthorizationManager authorizationManager) {
    Map<String, Object> userData = new HashMap<>();
    userData.put(USERID_FIELD, user.getUserId());
    userData.put(NAME_FIELD, user.getName());
    userData.put(FIRST_NAME_FIELD, user.getFirstName());
    userData.put(LAST_NAME_FIELD, user.getLastName());
    userData.put(EMAIL_FIELD, user.getEmailAddress());
    userData.put(SOURCE_FIELD, user.getSource());
    userData.put(STATUS_FIELD, user.getStatus());
    userData.put(VERSION_FIELD, user.getVersion());
    Map<String, Object> roleDataMap = new HashMap<>();
    userData.put(ROLES_FIELD, roleDataMap);
    user.getRoles()
        .forEach(childRole -> populateRoleDataMap(roleDataMap, authorizationManager, childRole.getRoleId()));
    return userData;
  }

  private Map<String, Object> toMap(final Role role, final AuthorizationManager authorizationManager) {
    Map<String, Object> roleData = new HashMap<>();
    roleData.put(NAME_FIELD, role.getName());
    roleData.put(SOURCE_FIELD, role.getSource());
    roleData.put(DESCRIPTION_FIELD, role.getDescription());
    roleData.put(VERSION_FIELD, role.getVersion());
    Map<String, Object> childRoleData = new HashMap<>();
    roleData.put(ROLES_FIELD, childRoleData);
    role.getRoles().forEach(childRole -> populateRoleDataMap(childRoleData, authorizationManager, childRole));
    Map<String, Object> privilegeData = new HashMap<>();
    roleData.put(PRIVILEGES_FIELD, privilegeData);
    role.getPrivileges()
        .forEach(privilege -> populatePrivilegeDataMap(privilegeData, authorizationManager, privilege));
    return roleData;
  }

  private Map<String, Object> toMap(final Privilege privilege) {
    Map<String, Object> privilegeData = new HashMap<>();
    privilegeData.put(DESCRIPTION_FIELD, privilege.getDescription());
    privilegeData.put(NAME_FIELD, privilege.getName());
    privilegeData.put(PERMISSION_FIELD, privilege.getPermission());
    privilegeData.put(PROPERTIES_FIELD, privilege.getProperties());
    privilegeData.put(VERSION_FIELD, privilege.getVersion());
    return privilegeData;
  }
}
