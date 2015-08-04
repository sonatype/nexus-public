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
package org.sonatype.nexus.testsuite.config.nexus1696;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.index.tasks.descriptors.UpdateIndexTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.model.RestApiSettings;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.ContentListMessageUtil;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.RoutesMessageUtil;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.sonatype.nexus.test.utils.TargetMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserResource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;

public class Nexus1696ValidateBaseUrlIT
    extends AbstractNexusIntegrationTest
{

  private String baseUrl;

  @Before
  public void init()
      throws Exception
  {
    baseUrl = nexusBaseUrl.replace("nexus", "nexus1696").replace("http", "https");

    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
    RestApiSettings restApiSettings = new RestApiSettings();
    restApiSettings.setForceBaseUrl(true);
    restApiSettings.setBaseUrl(baseUrl);
    settings.setGlobalRestApiSettings(restApiSettings);

    SettingsMessageUtil.save(settings);
  }

  @Test
  public void checkGroups()
      throws Exception
  {
    GroupMessageUtil groupUtil =
        new GroupMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);
    ContentListMessageUtil contentUtil =
        new ContentListMessageUtil(this.getXMLXStream(), MediaType.APPLICATION_XML);

    List<RepositoryGroupListResource> groups = groupUtil.getList();
    Assert.assertFalse("No itens to be tested", groups.isEmpty());

    for (RepositoryGroupListResource group : groups) {
      Assert.assertTrue("Repository '" + group.getId()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + group.getResourceURI(),
          group.getResourceURI().startsWith(baseUrl));

      List<ContentListResource> contents = contentUtil.getContentListResource(group.getId(), "/", true);

      for (ContentListResource content : contents) {
        Assert
            .assertTrue("Group content '" + content.getText() + "' uri do not start with baseUrl.  Expected: " + baseUrl
                + ", but got: " + content.getResourceURI(),
                content.getResourceURI().startsWith(baseUrl));
      }
    }
  }

  @Test
  public void checkRepositories()
      throws Exception
  {
    RepositoryMessageUtil repoUtil =
        new RepositoryMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);
    ContentListMessageUtil contentUtil =
        new ContentListMessageUtil(this.getXMLXStream(), MediaType.APPLICATION_XML);

    List<RepositoryListResource> repositories = repoUtil.getList();
    Assert.assertFalse("No itens to be tested", repositories.isEmpty());

    for (RepositoryListResource repo : repositories) {
      Assert.assertTrue("Repository '" + repo.getId()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + repo.getResourceURI(),
          repo.getResourceURI().startsWith(baseUrl));

      List<ContentListResource> contents = contentUtil.getContentListResource(repo.getId(), "/", false);

      for (ContentListResource content : contents) {
        Assert.assertTrue("Repository content '" + content.getText() + "' uri do not start with baseUrl.  Expected: "
            + baseUrl + ", but got: " + content.getResourceURI(),
            content.getResourceURI().startsWith(baseUrl));
      }
    }
  }

  @Test
  public void checkPrivs()
      throws Exception
  {
    List<PrivilegeStatusResource> privs =
        new PrivilegesMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML).getList();
    Assert.assertFalse("No itens to be tested", privs.isEmpty());

    for (PrivilegeStatusResource priv : privs) {
      Assert.assertTrue("Privilege '" + priv.getId()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + priv.getResourceURI(),
          priv.getResourceURI().startsWith(baseUrl));
    }
  }

  @Test
  public void checkRoles()
      throws Exception
  {
    List<RoleResource> roles = new RoleMessageUtil(this, null, null).getList();
    Assert.assertFalse("No itens to be tested", roles.isEmpty());

    for (RoleResource role : roles) {
      Assert.assertTrue("Role '" + role.getId()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + role.getResourceURI(),
          role.getResourceURI().startsWith(baseUrl));
    }
  }

  @Test
  public void checkUsers()
      throws Exception
  {
    List<UserResource> users = new UserMessageUtil(this, null, null).getList();
    Assert.assertFalse("No itens to be tested", users.isEmpty());

    for (UserResource user : users) {
      Assert.assertTrue("User '" + user.getUserId()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + user.getResourceURI(),
          user.getResourceURI().startsWith(baseUrl));
    }
  }

  @Test
  public void checkRouting()
      throws Exception
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("public");
    resource.setPattern(".*/org/.*");
    resource.setRuleType(RepositoryRouteResource.INCLUSION_RULE_TYPE);
    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    RoutesMessageUtil routesUtil = new RoutesMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
    Status status = routesUtil.sendMessage(Method.POST, resource).getStatus();
    Assert.assertTrue("Unable to create a route " + status, status.isSuccess());

    List<RepositoryRouteListResource> routes = RoutesMessageUtil.getList();
    Assert.assertFalse("No itens to be tested", routes.isEmpty());

    for (RepositoryRouteListResource route : routes) {
      Assert.assertTrue("Route '" + route.getGroupId()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + route.getResourceURI(),
          route.getResourceURI().startsWith(baseUrl));
    }
  }

  @Test
  public void checkTasks()
      throws Exception
  {
    ScheduledServiceBaseResource scheduledTask = new ScheduledServiceBaseResource();
    scheduledTask.setEnabled(true);
    scheduledTask.setId(null);
    scheduledTask.setName("taskManual");
    scheduledTask.setSchedule("manual");
    scheduledTask.setTypeId(UpdateIndexTaskDescriptor.ID);

    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue("all_repo");
    scheduledTask.addProperty(prop);

    Status status = TaskScheduleUtil.create(scheduledTask);
    Assert.assertTrue("Unable to create a task " + status, status.isSuccess());

    List<ScheduledServiceListResource> tasks = TaskScheduleUtil.getTasks();
    Assert.assertFalse("No itens to be tested", tasks.isEmpty());

    for (ScheduledServiceListResource task : tasks) {
      Assert.assertTrue("Task '" + task.getName()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + task.getResourceURI(),
          task.getResourceURI().startsWith(baseUrl));
    }
  }

  @Test
  public void checkRepositoryTargets()
      throws Exception
  {
    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("createTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    patterns.add(".*bar.*");
    resource.setPatterns(patterns);

    TargetMessageUtil targetUtil = new TargetMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    targetUtil.createTarget(resource);

    List<RepositoryTargetListResource> targets = TargetMessageUtil.getList();
    Assert.assertFalse("No itens to be tested", targets.isEmpty());

    for (RepositoryTargetListResource target : targets) {
      Assert.assertTrue("Target '" + target.getName()
          + "' uri do not start with baseUrl.  Expected: " + baseUrl + ", but got: " + target.getResourceURI(),
          target.getResourceURI().startsWith(baseUrl));
    }
  }

  @After
  public void resetBaseUrl()
      throws Exception
  {
    baseUrl = nexusBaseUrl;

    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
    RestApiSettings restApiSettings = new RestApiSettings();
    restApiSettings.setForceBaseUrl(true);
    restApiSettings.setBaseUrl(baseUrl);
    settings.setGlobalRestApiSettings(restApiSettings);

    SettingsMessageUtil.save(settings);
  }

}
