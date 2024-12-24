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
package org.sonatype.nexus.coreui;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * Role {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_Role")
public class RoleComponent
    extends DirectComponentSupport
{
  private final SecuritySystem securitySystem;

  private final List<AuthorizationManager> authorizationManagers;

  @Inject
  public RoleComponent(final SecuritySystem securitySystem, final List<AuthorizationManager> authorizationManagers) {
    this.securitySystem = checkNotNull(securitySystem);
    this.authorizationManagers = checkNotNull(authorizationManagers);
  }

  /**
   * Retrieves roles from all available {@link AuthorizationManager}s.
   *
   * @return a list of roles
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:roles:read")
  public List<RoleXO> read() throws NoSuchAuthorizationManagerException {
    return securitySystem.listRoles(DEFAULT_SOURCE)
        .stream()
        .map(this::convert)
        .collect(Collectors.toList()); // NOSONAR
  }

  /**
   * Retrieves role references from all available {@link AuthorizationManager}s.
   *
   * @return a list of role references
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:roles:read")
  public List<ReferenceXO> readReferences() throws NoSuchAuthorizationManagerException {
    return securitySystem.listRoles(DEFAULT_SOURCE)
        .stream()
        .map(input -> new ReferenceXO(input.getRoleId(), input.getName()))
        .collect(Collectors.toList()); // NOSONAR
  }

  /**
   * Retrieves available role sources.
   *
   * @return list of sources
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<ReferenceXO> readSources() {
    return authorizationManagers.stream()
        .filter(manager -> !DEFAULT_SOURCE.equals(manager.getSource()))
        .map(manager -> new ReferenceXO(manager.getSource(), manager.getSource()))
        .collect(Collectors.toList()); // NOSONAR
  }

  /**
   * Retrieves roles from specified source.
   *
   * @param source to retrieve roles from
   * @return a list of roles
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:roles:read")
  @Validate
  public List<RoleXO> readFromSource(@NotEmpty final String source) throws NoSuchAuthorizationManagerException {
    return securitySystem.listRoles(source)
        .stream()
        .map(this::convert)
        .collect(Collectors.toList()); // NOSONAR
  }

  /**
   * Creates a role.
   *
   * @param roleXO to be created
   * @return created role
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:create")
  @Validate(groups = {Create.class, Default.class})
  public RoleXO create(@NotNull @Valid final RoleXO roleXO) throws NoSuchAuthorizationManagerException {
    // HACK: Temporary validation for external role IDs to support editable text entry in combo box (LDAP only)
    if ("LDAP".equals(roleXO.getSource())) {
      securitySystem.getAuthorizationManager(roleXO.getSource()).getRole(roleXO.getId());
    }
    return convert(securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
        .addRole(
            new Role(
                roleXO.getId(),
                roleXO.getName(),
                roleXO.getDescription(),
                roleXO.getSource(),
                false,
                roleXO.getRoles(),
                roleXO.getPrivileges())));
  }

  /**
   * Updates a role.
   *
   * @param roleXO to be updated
   * @return updated role
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:update")
  @Validate(groups = {Update.class, Default.class})
  public RoleXO update(@NotNull @Valid final RoleXO roleXO) throws NoSuchAuthorizationManagerException {
    Role roleToUpdate = new Role();
    roleToUpdate.setRoleId(roleXO.getId());
    roleToUpdate.setName(roleXO.getName());
    roleToUpdate.setDescription(roleXO.getDescription());
    roleToUpdate.setSource(roleXO.getSource());
    roleToUpdate.setReadOnly(false);
    roleToUpdate.setRoles(roleXO.getRoles());
    roleToUpdate.setPrivileges(roleXO.getPrivileges());
    roleToUpdate.setVersion(Integer.parseInt(roleXO.getVersion()));
    return convert(securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
        .updateRole(roleToUpdate));
  }

  /**
   * Deletes a role.
   *
   * @param id of role to be deleted
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:delete")
  @Validate
  public void remove(@NotEmpty final String id) throws NoSuchAuthorizationManagerException {
    securitySystem.getAuthorizationManager(DEFAULT_SOURCE).deleteRole(id);
  }

  /**
   * Convert role to XO.
   */
  private RoleXO convert(final Role input) {
    RoleXO roleXO = new RoleXO();
    roleXO.setId(input.getRoleId());
    roleXO.setVersion(String.valueOf(input.getVersion()));
    roleXO.setSource((DEFAULT_SOURCE.equals(input.getSource()) ||
        Strings2.isBlank(input.getSource())) ? "Nexus" : input.getSource());
    roleXO.setName(Strings2.isBlank(input.getName()) ? input.getRoleId() : input.getName());
    roleXO.setDescription(Strings2.isBlank(input.getDescription()) ? input.getRoleId() : input.getDescription());
    roleXO.setReadOnly(input.isReadOnly());
    roleXO.setPrivileges(input.getPrivileges());
    roleXO.setRoles(input.getRoles());
    return roleXO;
  }
}
