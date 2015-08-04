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
package org.sonatype.nexus.integrationtests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.RoutesMessageUtil;
import org.sonatype.nexus.test.utils.TargetMessageUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserResource;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPrivilegeTest
    extends AbstractNexusIntegrationTest
{
  protected static Logger LOG = LoggerFactory.getLogger(AbstractPrivilegeTest.class);

  public static final String TEST_USER_NAME = "test-user";

  public static final String TEST_USER_PASSWORD = "admin123";

  protected UserMessageUtil userUtil;

  protected RoleMessageUtil roleUtil;

  protected PrivilegesMessageUtil privUtil;

  protected TargetMessageUtil targetUtil;

  protected RoutesMessageUtil routeUtil;

  protected RepositoryMessageUtil repoUtil;

  protected GroupMessageUtil groupUtil;

  public AbstractPrivilegeTest(String testRepositoryId) {
    super(testRepositoryId);

    try {
      this.init();
    }
    catch (ComponentLookupException e) {
      Assert.fail(e.getMessage());
    }
  }

  public AbstractPrivilegeTest() {
    try {
      this.init();
    }
    catch (ComponentLookupException e) {
      Assert.fail(e.getMessage());
    }
  }

  @BeforeClass
  public static void enableSecurity() {
    // turn on security for the test
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  private void init()
      throws ComponentLookupException
  {
    XStream xstream = this.getXMLXStream();

    this.userUtil = new UserMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.roleUtil = new RoleMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.privUtil = new PrivilegesMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.targetUtil = new TargetMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    TestContainer.getInstance().getTestContext().setSecureTest(true);
    this.routeUtil = new RoutesMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.repoUtil = new RepositoryMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.groupUtil = new GroupMessageUtil(this, xstream, MediaType.APPLICATION_XML);
  }

  @Before
  public void resetTestUserPrivs()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    UserResource testUser = this.userUtil.getUser(TEST_USER_NAME);
    testUser.getRoles().clear();
    testUser.addRole("anonymous");
    this.userUtil.updateUser(testUser);
  }

  /**
   * Enables the "private repository" feature as described in Nexus FAQ, without disabling anonymous access.
   * <p>
   * See https://docs.sonatype.com/display/SPRTNXOSS/Nexus+FAQ#NexusFAQ-Q.
   * CanImakearepositoryprivatewithoutdisablinganonymousaccess%3F
   *
   * Written by toby (https://gist.github.com/799810a9780100296977)
   */
  protected void enablePrivateRepository(final String userId, final String repositoryId)
      throws IOException
  {
    // remove repo access from anon user
    // this should remove all target related permissions
    overwriteUserRole("anonymous", "anonymous", "1", "54", "57", "58", "70", "74");

    // look at setup your priv
    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setContentClass("maven2");
    target.setName(repositoryId + "-target");
    target.addPattern("/some-pattern");

    // create the target
    target = targetUtil.createTarget(target);

    // now add some privs
    PrivilegeResource privReq = new PrivilegeResource();
    privReq.setDescription(repositoryId + "-target repo-target privilege");
    privReq.setMethod(Arrays.asList("create", "read", "update", "delete")); // pick and choose
    privReq.setName(repositoryId + "-priv");
    privReq.setRepositoryTargetId(target.getId());
    privReq.setType(TargetPrivilegeDescriptor.TYPE);

    // create them
    List<PrivilegeStatusResource> privs = privUtil.createPrivileges(privReq);

    // grant these to your user
    for (PrivilegeStatusResource priv : privs) {
      giveUserPrivilege(userId, priv.getId());
    }

    // grant a test user access to repository
    giveUserPrivilege(userId, "repository-" + repositoryId); // view priv (not really needed for ITs)
  }

  protected void printUserPrivs(String userId)
      throws IOException
  {
    ArrayList<String> privs = getUserPrivs(userId);

    LOG.info("User: " + userId);
    for (Iterator iter = privs.iterator(); iter.hasNext(); ) {
      String privName = (String) iter.next();
      LOG.info("\t" + privName);
    }
  }

  protected ArrayList<String> getUserPrivs(String userId)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    UserResource user = this.userUtil.getUser(userId);
    ArrayList<String> privs = new ArrayList<String>();

    for (Iterator iter = user.getRoles().iterator(); iter.hasNext(); ) {
      String roleId = (String) iter.next();
      RoleResource role = this.roleUtil.getRole(roleId);

      for (Iterator roleIter = role.getPrivileges().iterator(); roleIter.hasNext(); ) {
        String privId = (String) roleIter.next();
        // PrivilegeBaseStatusResource priv = this.privUtil.getPrivilegeResource( privId );
        // privs.add( priv.getName() );
        CPrivilege priv = getSecurityConfigUtil().getCPrivilege(privId);
        if (priv != null) {
          privs.add(priv.getName());
        }
        else {
          PrivilegeStatusResource basePriv = this.privUtil.getPrivilegeResource(privId);
          privs.add(basePriv.getName());
        }

      }
    }
    return privs;
  }

  protected void giveUserPrivilege(String userId, String priv)
      throws IOException
  {
    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    RoleResource role = null;

    // first try to retrieve
    for (RoleResource roleResource : roleUtil.getList()) {
      if (roleResource.getName().equals(priv + "Role")) {
        role = roleResource;

        if (!role.getPrivileges().contains(priv)) {
          role.addPrivilege(priv);
          // update the permissions
          RoleMessageUtil.update(role);
        }
        break;
      }
    }

    if (role == null) {
      // now give create
      role = new RoleResource();
      role.setDescription(priv + " Role");
      role.setName(priv + "Role");
      role.setSessionTimeout(60);
      role.addPrivilege(priv);
      // save it
      role = this.roleUtil.createRole(role);
    }

    // add it
    this.giveUserRole(userId, role.getId());
  }

  protected void giveUserRoleByName(String userId, String roleName)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    for (RoleResource roleResource : roleUtil.getList()) {
      if (roleResource.getName().equals(roleName)) {
        UserResource testUser = this.userUtil.getUser(userId);
        testUser.addRole(roleResource.getId());
        this.userUtil.updateUser(testUser);
        break;
      }
    }
  }

  protected void giveUserRole(String userId, String roleId)
      throws IOException
  {
    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // add it
    UserResource testUser = this.userUtil.getUser(userId);
    testUser.addRole(roleId);
    this.userUtil.updateUser(testUser);
  }

  protected void overwriteUserRole(String userId, String newRoleName, String... permissions)
      throws IOException
  {
    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    RoleResource role = null;

    // first try to retrieve
    for (RoleResource roleResource : roleUtil.getList()) {
      if (roleResource.getName().equals(newRoleName)) {
        role = roleResource;
        role.getPrivileges().clear();
        for (String priv : permissions) {
          role.addPrivilege(priv);
        }
        // update the permissions
        RoleMessageUtil.update(role);
        break;
      }
    }
    // if doesn't exist, create it
    if (role == null) {
      role = new RoleResource();
      role.setDescription(newRoleName);
      role.setName(newRoleName);
      role.setSessionTimeout(60);

      for (String priv : permissions) {
        role.addPrivilege(priv);
      }
      // save it
      role = this.roleUtil.createRole(role);
    }

    // add it
    UserResource testUser = this.userUtil.getUser(userId);
    testUser.getRoles().clear();
    testUser.addRole(role.getId());
    this.userUtil.updateUser(testUser);
  }

  protected void replaceUserRole(String userId, String roleId)
      throws Exception
  {
    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // now give create
    RoleResource role = null;

    // first try to retrieve
    for (RoleResource roleResource : roleUtil.getList()) {
      if (roleResource.getId().equals(roleId)) {
        role = roleResource;
        break;
      }
    }

    if (role == null) {
      Assert.fail("Role not found: " + roleId);
    }

    // add it
    UserResource testUser = this.userUtil.getUser(userId);
    testUser.getRoles().clear();
    testUser.addRole(role.getId());
    this.userUtil.updateUser(testUser);
  }

  @Override
  @After
  public void afterTest()
      throws Exception
  {
    // reset any password
    TestContainer.getInstance().getTestContext().useAdminForRequests();
  }

  // FIXME: This is crazy: String privilege, String... privs

  protected void addPrivilege(String userId, String privilege, String... privs)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    RoleResource role = roleUtil.findRole(privilege + "-role");
    boolean create = false;
    if (role == null) {
      role = new RoleResource();
      create = true;
    }
    role.setId(privilege + "-role");
    role.setName(privilege + "-name");
    role.addPrivilege(privilege);
    for (String priv : privs) {
      role.addPrivilege(priv);
    }
    role.setDescription(privilege);
    role.setSessionTimeout(100);
    if (create) {
      this.roleUtil.createRole(role);
    }
    else {
      RoleMessageUtil.update(role);
    }

    UserResource testUser = this.userUtil.getUser(userId);
    testUser.addRole(role.getId());
    this.userUtil.updateUser(testUser);
  }

  protected void removePrivilege(String userId, String privilege)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    UserResource testUser = this.userUtil.getUser(userId);
    testUser.removeRole(privilege + "-role");
    this.userUtil.updateUser(testUser);
  }

  protected void addPriv(String userName, String privId, String type, String repoTargetId, String repositoryId,
                         String repositoryGroupId, String... methods)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    PrivilegeResource priv = new PrivilegeResource();
    priv.setName(privId);
    priv.setDescription(privId);
    priv.setType(type);
    priv.setRepositoryTargetId(repoTargetId);
    priv.setRepositoryId(repositoryId);
    priv.setRepositoryGroupId(repositoryGroupId);
    for (String method : methods) {
      priv.addMethod(method);
    }

    List<PrivilegeStatusResource> stat = privUtil.createPrivileges(priv);
    addPrivilege(userName, stat.get(0).getId());
  }

}
