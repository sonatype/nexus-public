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
package org.sonatype.security.realms.validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUser;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.realms.privileges.PrivilegeDescriptor;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.util.StringUtils;

@Singleton
@Typed(SecurityConfigurationValidator.class)
@Named("default")
public class DefaultConfigurationValidator
    extends ComponentSupport
    implements SecurityConfigurationValidator
{
  private static String DEFAULT_SOURCE = "default";

  private final ConfigurationIdGenerator idGenerator;

  private final List<PrivilegeDescriptor> privilegeDescriptors;

  @Inject
  public DefaultConfigurationValidator(List<PrivilegeDescriptor> privilegeDescriptors,
                                       ConfigurationIdGenerator idGenerator)
  {
    this.privilegeDescriptors = privilegeDescriptors;
    this.idGenerator = idGenerator;
  }

  public ValidationResponse validateModel(ValidationRequest<Configuration> request) {
    ValidationResponse response = new ValidationResponse();
    response.setContext(new SecurityValidationContext());

    Configuration model = (Configuration) request.getConfiguration();

    SecurityValidationContext context = (SecurityValidationContext) response.getContext();

    List<CPrivilege> privs = model.getPrivileges();

    if (privs != null) {
      for (CPrivilege priv : privs) {
        response.append(validatePrivilege(context, priv, false));
      }
    }

    List<CRole> roles = model.getRoles();

    if (roles != null) {
      for (CRole role : roles) {
        response.append(validateRole(context, role, false));
      }
    }

    response.append(validateRoleContainment(context));

    List<CUser> users = model.getUsers();

    if (users != null) {
      for (CUser user : users) {
        Set<String> roleIds = new HashSet<String>();
        for (CUserRoleMapping userRoleMapping : (List<CUserRoleMapping>) model.getUserRoleMappings()) {
          if (userRoleMapping.getUserId() != null && userRoleMapping.getUserId().equals(user.getId())
              && (DEFAULT_SOURCE.equals(userRoleMapping.getSource()))) {
            roleIds.addAll(userRoleMapping.getRoles());
          }
        }

        response.append(validateUser(context, user, roleIds, false));
      }
    }

    List<CUserRoleMapping> userRoleMappings = model.getUserRoleMappings();
    if (userRoleMappings != null) {
      for (CUserRoleMapping userRoleMapping : userRoleMappings) {
        response.append(this.validateUserRoleMapping(context, userRoleMapping, false));
      }
    }

    // summary
    if (response.getValidationErrors().size() > 0 || response.getValidationWarnings().size() > 0) {
      log.error("* * * * * * * * * * * * * * * * * * * * * * * * * *");

      log.error("Security configuration has validation errors/warnings");

      log.error("* * * * * * * * * * * * * * * * * * * * * * * * * *");

      if (response.getValidationErrors().size() > 0) {
        log.error("The ERRORS:");

        for (ValidationMessage msg : response.getValidationErrors()) {
          log.error(msg.toString());
        }
      }

      if (response.getValidationWarnings().size() > 0) {
        log.error("The WARNINGS:");

        for (ValidationMessage msg : response.getValidationWarnings()) {
          log.error(msg.toString());
        }
      }

      log.error("* * * * * * * * * * * * * * * * * * * * *");
    }
    else {
      log.info("Security configuration validated succesfully.");
    }

    return response;
  }

  public ValidationResponse validatePrivilege(SecurityValidationContext ctx, CPrivilege privilege, boolean update) {
    ValidationResponse response = new ValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    for (PrivilegeDescriptor descriptor : privilegeDescriptors) {
      ValidationResponse resp = descriptor.validatePrivilege(privilege, ctx, update);

      if (resp != null) {
        response.append(resp);
      }
    }

    ctx.getExistingPrivilegeIds().add(privilege.getId());

    return response;
  }

  public ValidationResponse validateRoleContainment(SecurityValidationContext ctx) {
    ValidationResponse response = new ValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    SecurityValidationContext context = (SecurityValidationContext) response.getContext();

    if (context.getExistingRoleIds() != null) {
      for (String roleId : context.getExistingRoleIds()) {
        response.append(isRecursive(roleId, roleId, ctx));
      }
    }

    return response;
  }

  private boolean isRoleNameAlreadyInUse(Map<String, String> existingRoleNameMap, CRole role) {
    for (String roleId : existingRoleNameMap.keySet()) {
      if (roleId.equals(role.getId())) {
        continue;
      }
      if (existingRoleNameMap.get(roleId).equals(role.getName())) {
        return true;
      }
    }
    return false;
  }

  private String getRoleTextForDisplay(String roleId, SecurityValidationContext ctx) {
    String name = ctx.getExistingRoleNameMap().get(roleId);

    if (StringUtils.isEmpty(name)) {
      return roleId;
    }

    return name;
  }

  private ValidationResponse isRecursive(String baseRoleId, String roleId, SecurityValidationContext ctx) {
    ValidationResponse response = new ValidationResponse();

    List<String> containedRoles = ctx.getRoleContainmentMap().get(roleId);

    for (String containedRoleId : containedRoles) {
      // Only need to do this on the first level
      if (baseRoleId.equals(roleId)) {
        if (!ctx.getExistingRoleIds().contains(roleId)) {
          ValidationMessage message =
              new ValidationMessage("roles", "Role '" + getRoleTextForDisplay(baseRoleId, ctx)
                  + "' contains an invalid role", "Role cannot contain invalid role '"
                  + getRoleTextForDisplay(roleId, ctx) + "'.");

          response.addValidationWarning(message);
        }
      }

      if (containedRoleId.equals(baseRoleId)) {
        ValidationMessage message =
            new ValidationMessage("roles", "Role '" + getRoleTextForDisplay(baseRoleId, ctx)
                + "' contains itself through Role '" + getRoleTextForDisplay(roleId, ctx)
                + "'.  This is not valid.", "Role cannot contain itself recursively (via role '"
                + getRoleTextForDisplay(roleId, ctx) + "').");

        response.addValidationError(message);

        break;
      }

      if (ctx.getExistingRoleIds().contains(containedRoleId)) {
        response.append(isRecursive(baseRoleId, containedRoleId, ctx));
      }
      // Only need to do this on the first level
      else if (baseRoleId.equals(roleId)) {
        ValidationMessage message =
            new ValidationMessage("roles", "Role '" + getRoleTextForDisplay(roleId, ctx)
                + "' contains an invalid role '" + getRoleTextForDisplay(containedRoleId, ctx) + "'.",
                "Role cannot contain invalid role '"
                    + getRoleTextForDisplay(containedRoleId, ctx) + "'.");

        response.addValidationWarning(message);
      }
    }

    return response;
  }

  public ValidationResponse validateRole(SecurityValidationContext ctx, CRole role, boolean update) {
    ValidationResponse response = new ValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    SecurityValidationContext context = (SecurityValidationContext) response.getContext();

    List<String> existingIds = context.getExistingRoleIds();

    if (existingIds == null) {
      context.addExistingRoleIds();

      existingIds = context.getExistingRoleIds();
    }

    if (!update && existingIds.contains(role.getId())) {
      ValidationMessage message = new ValidationMessage("id", "Role ID must be unique.");
      response.addValidationError(message);
    }

    if (update && !existingIds.contains(role.getId())) {
      ValidationMessage message = new ValidationMessage("id", "Role ID cannot be changed.");
      response.addValidationError(message);
    }

    if (!update && (StringUtils.isEmpty(role.getId()) || "0".equals(role.getId()))) {
      String newId = idGenerator.generateId();

      response.addValidationWarning("Fixed wrong role ID from '" + role.getId() + "' to '" + newId + "'");

      role.setId(newId);

      response.setModified(true);
    }

    Map<String, String> existingRoleNameMap = context.getExistingRoleNameMap();

    if (StringUtils.isEmpty(role.getName())) {
      ValidationMessage message =
          new ValidationMessage("name", "Role ID '" + role.getId() + "' requires a name.", "Name is required.");
      response.addValidationError(message);
    }
    else if (isRoleNameAlreadyInUse(existingRoleNameMap, role)) {
      ValidationMessage message =
          new ValidationMessage("name", "Role ID '" + role.getId() + "' can't use the name '" + role.getName()
              + "'.", "Name is already in use.");
      response.addValidationError(message);
    }
    else {
      existingRoleNameMap.put(role.getId(), role.getName());
    }

    if (context.getExistingPrivilegeIds() != null) {
      List<String> privIds = role.getPrivileges();

      for (String privId : privIds) {
        if (!context.getExistingPrivilegeIds().contains(privId)) {
          ValidationMessage message =
              new ValidationMessage("privileges", "Role ID '" + role.getId() + "' Invalid privilege id '"
                  + privId + "' found.", "Role cannot contain invalid privilege ID '" + privId + "'.");
          response.addValidationWarning(message);
        }
      }
    }

    List<String> roleIds = role.getRoles();

    List<String> containedRoles = context.getRoleContainmentMap().get(role.getId());

    if (containedRoles == null) {
      containedRoles = new ArrayList<String>();
      context.getRoleContainmentMap().put(role.getId(), containedRoles);
    }

    for (String roleId : roleIds) {
      if (roleId.equals(role.getId())) {
        ValidationMessage message =
            new ValidationMessage("roles", "Role ID '" + role.getId() + "' cannot contain itself.",
                "Role cannot contain itself.");
        response.addValidationError(message);
      }
      else if (context.getRoleContainmentMap() != null) {
        containedRoles.add(roleId);
      }
    }

    // It is expected that a full context is built upon update
    if (update) {
      response.append(isRecursive(role.getId(), role.getId(), context));
    }

    existingIds.add(role.getId());

    return response;
  }

  public ValidationResponse validateUser(SecurityValidationContext ctx, CUser user, Set<String> roles, boolean update) {
    ValidationResponse response = new ValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    SecurityValidationContext context = (SecurityValidationContext) response.getContext();

    List<String> existingIds = context.getExistingUserIds();

    if (existingIds == null) {
      context.addExistingUserIds();

      existingIds = context.getExistingUserIds();
    }

    if (!update && StringUtils.isEmpty(user.getId())) {
      ValidationMessage message =
          new ValidationMessage("userId", "User ID is required.", "User ID is required.");

      response.addValidationError(message);
    }

    if (!update && StringUtils.isNotEmpty(user.getId()) && existingIds.contains(user.getId())) {
      ValidationMessage message =
          new ValidationMessage("userId", "User ID '" + user.getId() + "' is already in use.", "User ID '"
              + user.getId() + "' is already in use.");

      response.addValidationError(message);
    }

    if (StringUtils.isNotEmpty(user.getId()) && user.getId().contains(" ")) {
      ValidationMessage message =
          new ValidationMessage("userId", "User ID '" + user.getId() + "' cannot contain spaces.", "User ID '"
              + user.getId() + "' cannot contain spaces.");

      response.addValidationError(message);
    }

    if (StringUtils.isNotEmpty(user.getFirstName())) {
      user.setFirstName(user.getFirstName());
    }

    if (StringUtils.isNotEmpty(user.getLastName())) {
      user.setLastName(user.getLastName());
    }

    if (StringUtils.isEmpty(user.getPassword())) {
      ValidationMessage message =
          new ValidationMessage("password", "User ID '" + user.getId()
              + "' has no password.  This is a required field.", "Password is required.");
      response.addValidationError(message);
    }

    if (StringUtils.isEmpty(user.getEmail())) {
      ValidationMessage message =
          new ValidationMessage("email", "User ID '" + user.getId() + "' has no email address",
              "Email address is required.");
      response.addValidationError(message);
    }
    else {
      try {
        if (!user.getEmail().matches(".+@.+")) {
          ValidationMessage message =
              new ValidationMessage("email", "User ID '" + user.getId() + "' has an invalid email address.",
                  "Email address is invalid.");
          response.addValidationError(message);
        }
      }
      catch (PatternSyntaxException e) {
        throw new IllegalStateException("Regex did not compile: " + e.getMessage(), e);
      }

    }

    if (!CUser.STATUS_ACTIVE.equals(user.getStatus()) && !CUser.STATUS_DISABLED.equals(user.getStatus())) {
      ValidationMessage message =
          new ValidationMessage("status", "User ID '" + user.getId() + "' has invalid status '"
              + user.getStatus() + "'.  (Allowed values are: " + CUser.STATUS_ACTIVE + " and "
              + CUser.STATUS_DISABLED + ")", "Invalid Status selected.");
      response.addValidationError(message);
    }

    if (context.getExistingRoleIds() != null && context.getExistingUserRoleMap() != null) {

      if (roles != null && roles.size() > 0) {
        for (String roleId : roles) {
          if (!context.getExistingRoleIds().contains(roleId)) {
            ValidationMessage message =
                new ValidationMessage("roles", "User ID '" + user.getId() + "' Invalid role id '" + roleId
                    + "' found.", "User cannot contain invalid role ID '" + roleId + "'.");
            response.addValidationError(message);
          }
        }
      }
    }

    if (!StringUtils.isEmpty(user.getId())) {
      existingIds.add(user.getId());
    }

    return response;
  }

  public ValidationResponse validateUserRoleMapping(SecurityValidationContext context,
                                                    CUserRoleMapping userRoleMapping, boolean update)
  {
    ValidationResponse response = new ValidationResponse();

    // ID must be not empty
    if (StringUtils.isEmpty(userRoleMapping.getUserId())) {
      ValidationMessage message =
          new ValidationMessage("userId", "UserRoleMapping has no userId." + "  This is a required field.",
              "UserId is required.");
      response.addValidationError(message);
    }

    // source must be not empty
    if (StringUtils.isEmpty(userRoleMapping.getSource())) {
      ValidationMessage message =
          new ValidationMessage("source", "User Role Mapping for user '" + userRoleMapping.getUserId()
              + "' has no source.  This is a required field.", "UserId is required.");
      response.addValidationError(message);
    }

    List<String> roles = userRoleMapping.getRoles();
    // all roles must be real
    if (context.getExistingRoleIds() != null && context.getExistingUserRoleMap() != null) {

      if (roles != null && roles.size() > 0) {
        for (String roleId : roles) {
          if (!context.getExistingRoleIds().contains(roleId)) {
            ValidationMessage message =
                new ValidationMessage("roles", "User Role Mapping for user '"
                    + userRoleMapping.getUserId() + "' Invalid role id '" + roleId + "' found.",
                    "User cannot contain invalid role ID '" + roleId + "'.");
            response.addValidationError(message);
          }
        }
      }
    }

    return response;
  }
}
