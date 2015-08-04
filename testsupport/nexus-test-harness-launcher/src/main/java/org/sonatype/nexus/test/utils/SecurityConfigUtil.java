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
package org.sonatype.nexus.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUser;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Reader;
import org.sonatype.security.realms.tools.DynamicSecurityResource;
import org.sonatype.security.realms.tools.StaticSecurityResource;
import org.sonatype.security.rest.model.PrivilegeProperty;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserResource;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;

public class SecurityConfigUtil
    extends ITUtil
{
  public SecurityConfigUtil(AbstractNexusIntegrationTest test) {
    super(test);
  }

  public void verifyRole(RoleResource role)
      throws IOException
  {
    List<RoleResource> roles = new ArrayList<RoleResource>();
    roles.add(role);
    verifyRolesExistInCore(roles);
  }

  /**
   * Verify the list of roles contains all roles configured in security.xml
   */
  public void verifyRolesComplete(List<RoleResource> roles)
      throws IOException
  {
    for (CRole cRole : getSecurityConfig().getRoles()) {
      RoleResource roleResource = getRoleResource(cRole.getId(), roles);

      if (cRole.getId().endsWith("-view")) {
        // view roles privileges are added at runtime by listening to repository events. Which it is not
        // possible to do here
        continue;
      }

      Assert.assertNotNull("Role '" + cRole.getId() + "' should be contained!", roleResource);

      CRole role = RoleConverter.toCRole(roleResource);

      assertRoleEquals(cRole, role);
    }
  }

  public void assertRoleEquals(CRole roleA, CRole roleB) {
    // sort them, to not fail on same set but different order
    Collections.sort(roleA.getPrivileges());
    Collections.sort(roleB.getPrivileges());

    XStream xStream = new XStream();
    String roleStringA = xStream.toXML(roleA);
    String roleStringB = xStream.toXML(roleB);

    Assert.assertTrue("Role A:\n" + roleStringB + "\nRole B:\n"
        + roleStringA, new RoleComparator().compare(roleA, roleB) == 0);
  }

  private RoleResource getRoleResource(String id, List<RoleResource> roles) {
    for (RoleResource role : roles) {
      if (id.equals(role.getId())) {
        return role;
      }
    }

    return null;
  }

  public void verifyRolesExistInCore(List<RoleResource> roles)
      throws IOException
  {
    for (RoleResource roleResource : roles) {
      CRole secRole = getCRole(roleResource.getId());
      Assert.assertNotNull(secRole);
      CRole role = RoleConverter.toCRole(roleResource);

      assertRoleEquals(secRole, role);
    }
  }

  public void verifyUser(UserResource user)
      throws IOException
  {
    List<UserResource> users = new ArrayList<UserResource>();
    users.add(user);
    verifyUsers(users);
  }

  public void verifyUsers(List<UserResource> users)
      throws IOException
  {

    for (Iterator<UserResource> outterIter = users.iterator(); outterIter.hasNext(); ) {
      UserResource userResource = outterIter.next();

      CUser secUser = getCUser(userResource.getUserId());

      Assert.assertNotNull("Cannot find user: " + userResource.getUserId(), secUser);

      CUser user = UserConverter.toCUser(userResource);

      Assert.assertTrue(new UserComparator().compare(user, secUser) == 0);

    }
  }

  public String getPrivilegeProperty(PrivilegeStatusResource priv, String key) {
    for (PrivilegeProperty prop : priv.getProperties()) {
      if (prop.getKey().equals(key)) {
        return prop.getValue();
      }
    }

    return null;
  }

  public void verifyPrivileges(List<PrivilegeStatusResource> privs)
      throws IOException
  {
    for (Iterator<PrivilegeStatusResource> iter = privs.iterator(); iter.hasNext(); ) {
      PrivilegeStatusResource privResource = iter.next();

      CPrivilege secPriv = getCPrivilege(privResource.getId());

      Assert.assertNotNull(secPriv);

      Assert.assertEquals(secPriv.getId(), privResource.getId());
      Assert.assertEquals(secPriv.getName(), privResource.getName());
      Assert.assertEquals(secPriv.getDescription(), privResource.getDescription());

      for (CProperty prop : secPriv.getProperties()) {
        Assert.assertEquals(prop.getValue(), getPrivilegeProperty(privResource, prop.getKey()));
      }
    }
  }

  public CRole getCRole(String roleId)
      throws IOException
  {
    Configuration securityConfig = getSecurityConfig();
    List<CRole> secRoles = securityConfig.getRoles();

    for (Iterator<CRole> iter = secRoles.iterator(); iter.hasNext(); ) {
      CRole cRole = iter.next();

      if (roleId.equals(cRole.getId())) {
        return cRole;
      }
    }
    return null;
  }

  public CPrivilege getCPrivilege(String privilegeId)
      throws IOException
  {
    Configuration securityConfig = getSecurityConfig();
    List<CPrivilege> secPrivs = securityConfig.getPrivileges();

    for (Iterator<CPrivilege> iter = secPrivs.iterator(); iter.hasNext(); ) {
      CPrivilege cPriv = iter.next();

      if (privilegeId.equals(cPriv.getId())) {
        return cPriv;
      }
    }
    return null;
  }

  public CPrivilege getCPrivilegeByName(String privilegeName)
      throws IOException
  {
    Configuration securityConfig = getSecurityConfig();
    List<CPrivilege> secPrivs = securityConfig.getPrivileges();

    for (Iterator<CPrivilege> iter = secPrivs.iterator(); iter.hasNext(); ) {
      CPrivilege cPriv = iter.next();

      if (privilegeName.equals(cPriv.getName())) {
        return cPriv;
      }
    }
    return null;
  }

  public CUser getCUser(String userId)
      throws IOException
  {
    Configuration securityConfig = getSecurityConfig();
    List<CUser> secUsers = securityConfig.getUsers();

    for (Iterator<CUser> iter = secUsers.iterator(); iter.hasNext(); ) {
      CUser cUser = iter.next();

      if (userId.equals(cUser.getId())) {
        return cUser;
      }
    }
    return null;
  }

  public Configuration getSecurityConfig()
      throws IOException
  {
    File secConfigFile = new File(AbstractNexusIntegrationTest.WORK_CONF_DIR, "security.xml");

    Reader fr = null;
    Configuration configuration = null;

    try {
      SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();

      fr = new InputStreamReader(new FileInputStream(secConfigFile));

      // read again with interpolation
      try {
        configuration = reader.read(fr);
      }
      finally {
        fr.close();
      }

      Configuration staticConfiguration = null;

      fr =
          new InputStreamReader(
              SecurityConfigUtil.class.getResourceAsStream("/META-INF/nexus/static-security.xml"));

      try {
        staticConfiguration = reader.read(fr);
      }
      finally {
        fr.close();
      }

      for (CUser user : staticConfiguration.getUsers()) {
        configuration.addUser(user);
      }
      for (CRole role : staticConfiguration.getRoles()) {
        configuration.addRole(role);
      }
      for (CPrivilege priv : staticConfiguration.getPrivileges()) {
        configuration.addPrivilege(priv);
      }

      List<StaticSecurityResource> resources =
          getTest().getITPlexusContainer().lookupList(StaticSecurityResource.class);
      for (StaticSecurityResource resource : resources) {
        addStaticSecurity(configuration, resource.getConfiguration());
      }

      /**
       * This is really really hacky, as we are manually joining together roles here. I don't like it but it makes
       * the IT pass. TODO: Come up with some other means to do this. Need guice to handle this properly
       */
      addStaticSecurity(configuration, reader, "/META-INF/nexus-indexer-lucene-static-security.xml");
      addStaticSecurity(configuration, reader, "/META-INF/nexus-archive-browser-plugin-security.xml");
      addStaticSecurity(configuration, reader, "/META-INF/nexus-rrb-plugin-security.xml");

      List<DynamicSecurityResource> dynamic =
          getTest().getITPlexusContainer().lookupList(DynamicSecurityResource.class);
      for (DynamicSecurityResource resource : dynamic) {
        addStaticSecurity(configuration, resource.getConfiguration());
      }
    }
    catch (IOException e) {
      throw e;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      IOException ioe = new IOException("could not parse security.xml");
      ioe.initCause(e);
      throw ioe;
    }
    return configuration;
  }

  private void addStaticSecurity(Configuration configuration, SecurityConfigurationXpp3Reader reader,
                                 String securityFile)
      throws IOException, XmlPullParserException
  {

    final InputStream input = SecurityConfigUtil.class.getResourceAsStream(securityFile);
    if (input == null) {
      // probably a pro XML.
      return;
    }
    InputStreamReader fr = new InputStreamReader(input);

    Configuration staticConfiguration;
    try {
      staticConfiguration = reader.read(fr);
    }
    finally {
      fr.close();
    }

    addStaticSecurity(configuration, staticConfiguration);
  }

  private void addStaticSecurity(Configuration configuration, Configuration staticConfiguration) {
    for (CPrivilege priv : staticConfiguration.getPrivileges()) {
      CPrivilege p = getPrivilege(priv.getId(), configuration.getPrivileges());
      if (p == null) {
        configuration.addPrivilege(priv);
      }
    }

    for (CRole role : staticConfiguration.getRoles()) {
      CRole existingRole = getRole(role.getId(), configuration.getRoles());

      if (existingRole != null) {

        for (String containedRole : role.getRoles()) {
          if (!existingRole.getRoles().contains(containedRole)) {
            existingRole.addRole(containedRole);
          }
        }

        for (String containedPriv : role.getPrivileges()) {
          if (!existingRole.getPrivileges().contains(containedPriv)) {
            existingRole.addPrivilege(containedPriv);
          }
        }
      }
      else {
        configuration.addRole(role);
      }
    }
  }

  private CRole getRole(String id, List<CRole> roles) {
    for (CRole role : roles) {
      if (role.getId().equals(id)) {
        return role;
      }
    }

    return null;
  }

  private CPrivilege getPrivilege(String id, List<CPrivilege> privs) {
    for (CPrivilege priv : privs) {
      if (priv.getId().equals(id)) {
        return priv;
      }
    }

    return null;
  }

}
