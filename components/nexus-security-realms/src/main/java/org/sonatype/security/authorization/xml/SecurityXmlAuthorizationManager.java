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
package org.sonatype.security.authorization.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.events.AuthorizationConfigurationChanged;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.model.CRole;
import org.sonatype.security.realms.tools.ConfigurationManager;
import org.sonatype.security.realms.tools.ConfigurationManagerAction;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Throwables;

/**
 * AuthorizationManager that wraps roles from security-xml-realm.
 */
@Singleton
@Typed(AuthorizationManager.class)
@Named("default")
public class SecurityXmlAuthorizationManager
    implements AuthorizationManager
{
  public static final String SOURCE = "default";

  private final ConfigurationManager configuration;

  private final PrivilegeInheritanceManager privInheritance;

  private final EventBus eventBus;

  @Inject
  public SecurityXmlAuthorizationManager(@Named("default") ConfigurationManager configuration,
                                         PrivilegeInheritanceManager privInheritance,
                                         EventBus eventBus)
  {
    this.configuration = configuration;
    this.privInheritance = privInheritance;
    this.eventBus = eventBus;
  }

  public String getSource() {
    return SOURCE;
  }

  protected Role toRole(CRole secRole) {
    Role role = new Role();

    role.setRoleId(secRole.getId());
    role.setName(secRole.getName());
    role.setSource(SOURCE);
    role.setDescription(secRole.getDescription());
    role.setReadOnly(secRole.isReadOnly());
    role.setPrivileges(new HashSet<String>(secRole.getPrivileges()));
    role.setRoles(new HashSet<String>(secRole.getRoles()));

    return role;
  }

  protected CRole toRole(Role role) {
    CRole secRole = new CRole();

    secRole.setId(role.getRoleId());
    secRole.setName(role.getName());
    secRole.setDescription(role.getDescription());
    secRole.setReadOnly(role.isReadOnly());
    // null check
    if (role.getPrivileges() != null) {
      secRole.setPrivileges(new ArrayList<String>(role.getPrivileges()));
    }
    else {
      secRole.setPrivileges(new ArrayList<String>());
    }

    // null check
    if (role.getRoles() != null) {
      secRole.setRoles(new ArrayList<String>(role.getRoles()));
    }
    else {
      secRole.setRoles(new ArrayList<String>());
    }

    return secRole;
  }

  protected CPrivilege toPrivilege(Privilege privilege) {
    CPrivilege secPriv = new CPrivilege();
    secPriv.setId(privilege.getId());
    secPriv.setName(privilege.getName());
    secPriv.setDescription(privilege.getDescription());
    secPriv.setReadOnly(privilege.isReadOnly());
    secPriv.setType(privilege.getType());

    if (privilege.getProperties() != null && privilege.getProperties().entrySet() != null) {
      for (Entry<String, String> entry : privilege.getProperties().entrySet()) {
        CProperty prop = new CProperty();
        prop.setKey(entry.getKey());
        prop.setValue(entry.getValue());
        secPriv.addProperty(prop);
      }
    }

    return secPriv;
  }

  protected Privilege toPrivilege(CPrivilege secPriv) {
    Privilege privilege = new Privilege();
    privilege.setId(secPriv.getId());
    privilege.setName(secPriv.getName());
    privilege.setDescription(secPriv.getDescription());
    privilege.setReadOnly(secPriv.isReadOnly());
    privilege.setType(secPriv.getType());

    if (secPriv.getProperties() != null) {
      for (CProperty prop : (List<CProperty>) secPriv.getProperties()) {
        privilege.addProperty(prop.getKey(), prop.getValue());
      }
    }

    return privilege;
  }

  // //
  // ROLE CRUDS
  // //

  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();
    List<CRole> secRoles = this.configuration.listRoles();

    for (CRole CRole : secRoles) {
      roles.add(this.toRole(CRole));
    }

    return roles;
  }

  public Role getRole(String roleId)
      throws NoSuchRoleException
  {
    return this.toRole(this.configuration.readRole(roleId));
  }

  public Role addRole(Role role)
      throws InvalidConfigurationException
  {
    // the roleId of the secRole might change, so we need to keep the reference
    final CRole secRole = this.toRole(role);

    try {
      this.configuration.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run() throws Exception {
          configuration.createRole(secRole);
          configuration.save();
        }
      });
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e, InvalidConfigurationException.class);
      throw Throwables.propagate(e);
    }

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.toRole(secRole);
  }

  public Role updateRole(Role role)
      throws NoSuchRoleException, InvalidConfigurationException
  {
    final CRole secRole = this.toRole(role);

    try {
      this.configuration.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run() throws Exception {
          configuration.updateRole(secRole);
          configuration.save();
        }
      });
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e, NoSuchRoleException.class, InvalidConfigurationException.class);
      throw Throwables.propagate(e);
    }

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.toRole(secRole);
  }

  public void deleteRole(final String roleId)
      throws NoSuchRoleException
  {
    try {
      this.configuration.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run() throws Exception {
          configuration.deleteRole(roleId);
          configuration.save();
        }
      });
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e, NoSuchRoleException.class);
      throw Throwables.propagate(e);
    }

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();
  }

  // //
  // PRIVILEGE CRUDS
  // //

  public Set<Privilege> listPrivileges() {
    Set<Privilege> privileges = new HashSet<Privilege>();
    List<CPrivilege> secPrivs = this.configuration.listPrivileges();

    for (CPrivilege CPrivilege : secPrivs) {
      privileges.add(this.toPrivilege(CPrivilege));
    }

    return privileges;
  }

  public Privilege getPrivilege(String privilegeId)
      throws NoSuchPrivilegeException
  {
    return this.toPrivilege(this.configuration.readPrivilege(privilegeId));
  }

  public Privilege addPrivilege(Privilege privilege)
      throws InvalidConfigurationException
  {
    final CPrivilege secPriv = this.toPrivilege(privilege);
    // create implies read, so we need to add logic for that
    addInheritedPrivileges(secPriv);

    try {
      this.configuration.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run() throws Exception {
          configuration.createPrivilege(secPriv);
          configuration.save();
        }
      });
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e, InvalidConfigurationException.class);
      throw Throwables.propagate(e);
    }

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.toPrivilege(secPriv);
  }

  public Privilege updatePrivilege(Privilege privilege)
      throws NoSuchPrivilegeException, InvalidConfigurationException
  {
    final CPrivilege secPriv = this.toPrivilege(privilege);

    try {
      this.configuration.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run() throws Exception {
          configuration.updatePrivilege(secPriv);
          configuration.save();
        }
      });
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e, NoSuchPrivilegeException.class, InvalidConfigurationException.class);
      throw Throwables.propagate(e);
    }

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();

    return this.toPrivilege(secPriv);
  }

  public void deletePrivilege(final String privilegeId)
      throws NoSuchPrivilegeException
  {
    try {
      this.configuration.runWrite(new ConfigurationManagerAction()
      {
        @Override
        public void run() throws Exception {
          configuration.deletePrivilege(privilegeId);
          configuration.save();
        }
      });
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e, NoSuchPrivilegeException.class);
      throw Throwables.propagate(e);
    }

    // notify any listeners that the config changed
    this.fireAuthorizationChangedEvent();
  }

  public boolean supportsWrite() {
    return true;
  }

  private void addInheritedPrivileges(CPrivilege privilege) {
    CProperty methodProperty = null;

    for (CProperty property : (List<CProperty>) privilege.getProperties()) {
      if (property.getKey().equals("method")) {
        methodProperty = property;
        break;
      }
    }

    if (methodProperty != null) {
      List<String> inheritedMethods = privInheritance.getInheritedMethods(methodProperty.getValue());

      StringBuffer buf = new StringBuffer();

      for (String method : inheritedMethods) {
        buf.append(method);
        buf.append(",");
      }

      if (buf.length() > 0) {
        buf.setLength(buf.length() - 1);

        methodProperty.setValue(buf.toString());
      }
    }
  }

  private void fireAuthorizationChangedEvent() {
    this.eventBus.post(new AuthorizationConfigurationChanged());
  }

}
