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
package org.sonatype.nexus.testsuite.group.nexus3567;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNode;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNodeDTO;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeViewResponseDTO;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.SearchMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class Nexus3567GroupMemberChangesIndexIT
    extends AbstractNexusIntegrationTest
{
  private RepositoryMessageUtil repoUtil = null;

  private GroupMessageUtil groupUtil = null;

  private SearchMessageUtil searchUtil = null;

  private RepositoryResource repoResource = null;

  public Nexus3567GroupMemberChangesIndexIT()
      throws ComponentLookupException
  {
    repoUtil = new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
    groupUtil = new GroupMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
    searchUtil = new SearchMessageUtil(this);
  }

  @Test
  public void validateGroupIndexTreeOnDelete()
      throws Exception
  {
    prepare("nexus3567deletememberrepo", "nexus3567deletemembergroup");

    IndexBrowserTreeNodeDTO node = getIndexContent("nexus3567deletemembergroup");

    List<IndexBrowserTreeNodeDTO> children = node.getChildren();

    Assert.assertEquals(1, children.size());
    Assert.assertEquals(children.get(0).getNodeName(), "nexus3567");

    // now delete the child repo and validate that there is no root node
    Response response = repoUtil.sendMessage(Method.DELETE, repoResource);
    Assert.assertTrue(response.getStatus().isSuccess());
    TaskScheduleUtil.waitForAllTasksToStop();

    // now reindex the repo
    searchUtil.reindexGroup("nexus3567_deleteandreindex", "nexus3567deletemembergroup", true);
    TaskScheduleUtil.waitForAllTasksToStop();

    node = getIndexContent("nexus3567deletemembergroup");

    assertThat("member removed, root does not have children", node.getChildren(), is(nullValue()));
  }

  @Test
  public void validateGroupIndexTreeOnMemberRemove()
      throws Exception
  {
    prepare("nexus3567removememberrepo", "nexus3567removemembergroup");

    IndexBrowserTreeNodeDTO node = getIndexContent("nexus3567removemembergroup");

    List<IndexBrowserTreeNodeDTO> children = node.getChildren();

    Assert.assertEquals(1, children.size());
    Assert.assertEquals(children.get(0).getNodeName(), "nexus3567");

    // now remove the child repo and validate that there is no root node
    RepositoryGroupResource group = groupUtil.getGroup("nexus3567removemembergroup");
    group.getRepositories().clear();
    groupUtil.updateGroup(group);
    TaskScheduleUtil.waitForAllTasksToStop();

    // now reindex the repo
    searchUtil.reindexGroup("nexus3567_removeandreindex", "nexus3567removemembergroup", true);
    TaskScheduleUtil.waitForAllTasksToStop();

    node = getIndexContent("nexus3567removemembergroup");

    assertThat("member removed, root does not have children", node.getChildren(), is(nullValue()));
  }

  private void prepare(String repoId, String groupId)
      throws Exception
  {
    // first thing, create the repo and the group
    repoResource = new RepositoryResource();
    repoResource.setProvider("maven2");
    repoResource.setFormat("maven2");
    repoResource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repoResource.setChecksumPolicy(ChecksumPolicy.IGNORE.name());
    repoResource.setBrowseable(true);
    repoResource.setIndexable(true);
    repoResource.setExposed(true);
    repoResource.setId(repoId);
    repoResource.setName(repoId);
    repoResource.setRepoType("hosted");
    repoResource.setIndexable(true);
    repoResource.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repoResource.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repoResource.setNotFoundCacheTTL(1440);
    repoResource.setDownloadRemoteIndexes(false);
    repoUtil.createRepository(repoResource, false);

    TaskScheduleUtil.waitForAllTasksToStop();

    // now create the group
    RepositoryGroupResource group = new RepositoryGroupResource();
    group.setId(groupId);
    group.setFormat("maven2");
    group.setProvider("maven2");
    group.setName(groupId);
    group.setExposed(true);
    RepositoryGroupMemberRepository repo = new RepositoryGroupMemberRepository();
    repo.setId(repoId);
    group.addRepository(repo);
    groupUtil.createGroup(group);

    TaskScheduleUtil.waitForAllTasksToStop();

    // now upload an artifact to the repo
    File artifact = getTestFile("artifact.jar");
    Gav gav = GavUtil.newGav("nexus3567", "artifact", "1.0.0");
    int code = getDeployUtils().deployUsingGavWithRest(repoId, gav, artifact);
    Assert.assertTrue("Unable to deploy artifact " + code, Status.isSuccess(code));

    getEventInspectorsUtil().waitForCalmPeriod();
  }

  private IndexBrowserTreeNodeDTO getIndexContent(String repoId)
      throws IOException
  {
    String serviceURI = "service/local/repositories/" + repoId + "/index_content/";

    String responseText = RequestFacade.doGetForText(serviceURI);

    XStream xstream = XStreamFactory.getXmlXStream();

    xstream.processAnnotations(IndexBrowserTreeNode.class);
    xstream.processAnnotations(IndexBrowserTreeViewResponseDTO.class);

    XStreamRepresentation re = new XStreamRepresentation(xstream, responseText, MediaType.APPLICATION_XML);
    IndexBrowserTreeViewResponseDTO resourceResponse =
        (IndexBrowserTreeViewResponseDTO) re.getPayload(new IndexBrowserTreeViewResponseDTO());

    return resourceResponse.getData();
  }
}
