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
package org.sonatype.nexus.security.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeCreatedEvent;
import org.sonatype.nexus.security.privilege.PrivilegeDeletedEvent;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.PrivilegeUpdatedEvent;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleCreatedEvent;
import org.sonatype.nexus.security.role.RoleDeletedEvent;
import org.sonatype.nexus.security.role.RoleUpdatedEvent;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link AuthorizationManager}.
 */
@Named("default")
@Singleton
public class AuthorizationManagerImpl
    implements AuthorizationManager
{
  public static final String SOURCE = "default";

  private final SecurityConfigurationManager configuration;

  private final EventManager eventManager;

  private final List<PrivilegeDescriptor> privilegeDescriptors;

  @Inject
  public AuthorizationManagerImpl(final SecurityConfigurationManager configuration,
                                  final EventManager eventManager,
                                  final List<PrivilegeDescriptor> privilegeDescriptors)
  {
    this.configuration = configuration;
    this.eventManager = eventManager;
    this.privilegeDescriptors = checkNotNull(privilegeDescriptors);
  }

  @Override
  public String getSource() {
    return SOURCE;
  }

  private Role convert(final CRole source) {
    Role target = new Role();
    target.setRoleId(source.getId());
    target.setVersion(source.getVersion());
    target.setName(source.getName());
    target.setSource(SOURCE);
    target.setDescription(source.getDescription());
    target.setReadOnly(source.isReadOnly());
    target.setPrivileges(Sets.newHashSet(source.getPrivileges()));
    target.setRoles(Sets.newHashSet(source.getRoles()));
    return target;
  }

  private CRole convert(final Role source) {
    CRole target = new CRole();
    target.setId(source.getRoleId());
    target.setVersion(source.getVersion());
    target.setName(source.getName());
    target.setDescription(source.getDescription());
    target.setReadOnly(source.isReadOnly());

    if (source.getPrivileges() != null) {
      target.setPrivileges(Sets.newHashSet(source.getPrivileges()));
    }
    else {
      target.setPrivileges(Sets.<String>newHashSet());
    }

    if (source.getRoles() != null) {
      target.setRoles(Sets.newHashSet(source.getRoles()));
    }
    else {
      target.setRoles(Sets.<String>newHashSet());
    }

    return target;
  }

  private CPrivilege convert(final Privilege source) {
    CPrivilege target = new CPrivilege();
    target.setId(source.getId());
    target.setVersion(source.getVersion());
    target.setName(source.getName());
    target.setDescription(source.getDescription());
    target.setReadOnly(source.isReadOnly());
    target.setType(source.getType());
    if (source.getProperties() != null) {
      target.setProperties(Maps.newHashMap(source.getProperties()));
    }

    return target;
  }

  private Privilege convert(final CPrivilege source) {
    Privilege target = new Privilege();
    target.setId(source.getId());
    target.setVersion(source.getVersion());
    target.setName(source.getName() == null ? source.getId() : source.getName());
    target.setDescription(source.getDescription());
    target.setReadOnly(source.isReadOnly());
    target.setType(source.getType());
    target.setProperties(Maps.newHashMap(source.getProperties()));

    // expose permission string representation
    PrivilegeDescriptor descriptor = descriptor(source.getType());
    if (descriptor != null) {
      target.setPermission(descriptor.createPermission(source));
    }

    return target;
  }


  @Nullable
  private PrivilegeDescriptor descriptor(final String type) {
    for (PrivilegeDescriptor descriptor : privilegeDescriptors) {
      if (type.equals(descriptor.getType())) {
        return descriptor;
      }
    }
    return null;
  }

  // //
  // ROLE CRUDS
  // //

  @Override
  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();
    List<CRole> secRoles = this.configuration.listRoles();

    for (CRole CRole : secRoles) {
      roles.add(this.convert(CRole));
    }

    return roles;
  }

  @Override
  public Role getRole(String roleId) throws NoSuchRoleException {
    return this.convert(this.configuration.readRole(roleId));
  }

  @Override
  public Role addRole(Role role) {
    // the roleId of the secRole might change, so we need to keep the reference
    final CRole secRole = this.convert(role);

    configuration.createRole(secRole);

    eventManager.post(new RoleCreatedEvent(role));

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.convert(secRole);
  }

  @Override
  public Role updateRole(Role role) throws NoSuchRoleException {
    final CRole secRole = this.convert(role);

    configuration.updateRole(secRole);

    eventManager.post(new RoleUpdatedEvent(role));

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.convert(secRole);
  }

  @Override
  public void deleteRole(final String roleId) throws NoSuchRoleException {
    Role role = getRole(roleId);
    configuration.deleteRole(roleId);

    eventManager.post(new RoleDeletedEvent(role));

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();
  }

  // //
  // PRIVILEGE CRUDS
  // //

  @Override
  public Set<Privilege> listPrivileges() {
    Set<Privilege> privileges = new HashSet<Privilege>();
    List<CPrivilege> secPrivs = this.configuration.listPrivileges();

    for (CPrivilege CPrivilege : secPrivs) {
      privileges.add(this.convert(CPrivilege));
    }

    return privileges;
  }

  @Override
  public Privilege getPrivilege(String privilegeId) throws NoSuchPrivilegeException {
    return this.convert(this.configuration.readPrivilege(privilegeId));
  }

  @Override
  public Privilege addPrivilege(Privilege privilege) {
    final CPrivilege secPriv = this.convert(privilege);
    configuration.createPrivilege(secPriv);

    eventManager.post(new PrivilegeCreatedEvent(privilege));

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.convert(secPriv);
  }

  @Override
  public Privilege updatePrivilege(Privilege privilege) throws NoSuchPrivilegeException {
    final CPrivilege secPriv = this.convert(privilege);

    configuration.updatePrivilege(secPriv);

    eventManager.post(new PrivilegeUpdatedEvent(privilege));

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.convert(secPriv);
  }

  @Override
  public void deletePrivilege(final String privilegeId) throws NoSuchPrivilegeException {
    Privilege privilege = getPrivilege(privilegeId);
    configuration.deletePrivilege(privilegeId);

    eventManager.post(new PrivilegeDeletedEvent(privilege));

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();
  }

  @Override
  public boolean supportsWrite() {
    return true;
  }

  private void fireAuthorizationChangedEvent() {
    this.eventManager.post(new AuthorizationConfigurationChanged());
  }
}
