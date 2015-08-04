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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.configuration.model.CRepositoryTarget;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class TargetMessageUtil
    extends ITUtil
{
  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(TargetMessageUtil.class);

  public TargetMessageUtil(AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType) {
    super(test);
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public RepositoryTargetResource createTarget(RepositoryTargetResource target)
      throws IOException
  {
    return saveTarget(target, false);
  }

  public RepositoryTargetResource saveTarget(RepositoryTargetResource target, boolean update)
      throws IOException
  {
    Response response = null;
    String entityText;
    try {
      response = this.sendMessage(update ? Method.PUT : Method.POST, target);
      entityText = response.getEntity().getText();
      assertThat(response, isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.getResourceFromResponse(entityText);

    // validate
    // make sure the id != null
    Assert.assertTrue(StringUtils.isNotEmpty(responseResource.getId()));
    if (update) {
      Assert.assertEquals(responseResource.getId(), target.getId());
    }

    Assert.assertEquals(responseResource.getContentClass(), target.getContentClass());
    Assert.assertEquals(responseResource.getName(), target.getName());
    Assert.assertEquals(target.getPatterns(), responseResource.getPatterns());

    this.verifyTargetsConfig(responseResource);

    return responseResource;
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response sendMessage(Method method, RepositoryTargetResource resource)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String repoTargetId = (resource.getId() == null) ? "?undefined" : "/" + resource.getId();

    String serviceURI = "service/local/repo_targets" + repoTargetId;

    RepositoryTargetResourceResponse requestResponse = new RepositoryTargetResourceResponse();
    requestResponse.setData(resource);
    // now set the payload
    representation.setPayload(requestResponse);

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  @SuppressWarnings("unchecked")
  public static List<RepositoryTargetListResource> getList()
      throws IOException
  {

    String responseText = RequestFacade.doGetForText("service/local/repo_targets");
    LOG.debug("responseText: \n" + responseText);

    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML);

    RepositoryTargetListResourceResponse resourceResponse =
        (RepositoryTargetListResourceResponse) representation.getPayload(new RepositoryTargetListResourceResponse());

    return resourceResponse.getData();

  }

  public RepositoryTargetResource getResourceFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();
    return this.getResourceFromResponse(responseString);
  }

  public RepositoryTargetResource getResourceFromResponse(String responseText)
      throws IOException
  {
    LOG.debug(" getResourceFromResponse: " + responseText);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseText, mediaType);

    RepositoryTargetResourceResponse resourceResponse =
        (RepositoryTargetResourceResponse) representation.getPayload(new RepositoryTargetResourceResponse());

    return resourceResponse.getData();

  }

  public void verifyTargetsConfig(RepositoryTargetResource targetResource)
      throws IOException
  {
    ArrayList<RepositoryTargetResource> targetResources = new ArrayList<RepositoryTargetResource>();
    targetResources.add(targetResource);
    this.verifyTargetsConfig(targetResources);
  }

  @SuppressWarnings("unchecked")
  public void verifyTargetsConfig(List<RepositoryTargetResource> targetResources)
      throws IOException
  {
    // check the nexus.xml
    Configuration config = getTest().getNexusConfigUtil().getNexusConfig();

    List<CRepositoryTarget> repoTargets = config.getRepositoryTargets();

    // TODO: we can't check the size unless we reset the config after each run...
    // check to see if the size matches
    // Assert.assertTrue( "Configuration had a different number: (" + repoTargets.size()
    // + ") of targets then expected: (" + targetResources.size() + ")",
    // repoTargets.size() == targetResources.size() );

    // look for the target by id

    for (Iterator<RepositoryTargetResource> iter = targetResources.iterator(); iter.hasNext(); ) {
      RepositoryTargetResource targetResource = iter.next();
      boolean found = false;

      for (Iterator<CRepositoryTarget> iterInner = repoTargets.iterator(); iterInner.hasNext(); ) {
        CRepositoryTarget repositoryTarget = iterInner.next();

        if (targetResource.getId().equals(repositoryTarget.getId())) {
          found = true;
          Assert.assertEquals(repositoryTarget.getId(), targetResource.getId());
          Assert.assertEquals(repositoryTarget.getContentClass(), targetResource.getContentClass());
          Assert.assertEquals(repositoryTarget.getName(), targetResource.getName());
          // order doesn't matter
          Assert.assertEquals(new HashSet<String>(targetResource.getPatterns()), new HashSet<String>(
              repositoryTarget.getPatterns()));

          break;
        }

      }

      if (!found) {

        Assert.fail("Target with ID: " + targetResource.getId() + " could not be found in configuration.");
      }
    }
  }

  public void verifyCompleteTargetsConfig(List<RepositoryTargetListResource> targets)
      throws IOException
  {
    // check the nexus.xml
    Configuration config = getTest().getNexusConfigUtil().getNexusConfig();

    List<CRepositoryTarget> repoTargets = config.getRepositoryTargets();
    // check to see if the size matches
    Assert.assertTrue("Configuration had a different number: (" + repoTargets.size()
        + ") of targets then expected: (" + targets.size() + ")", repoTargets.size() == targets.size());

    // look for the target by id

    for (Iterator<RepositoryTargetListResource> iter = targets.iterator(); iter.hasNext(); ) {
      RepositoryTargetListResource targetResource = iter.next();
      boolean found = false;

      for (Iterator<CRepositoryTarget> iterInner = repoTargets.iterator(); iterInner.hasNext(); ) {
        CRepositoryTarget repositoryTarget = iterInner.next();

        if (targetResource.getId().equals(repositoryTarget.getId())) {
          found = true;
          Assert.assertEquals(repositoryTarget.getId(), targetResource.getId());
          Assert.assertEquals(repositoryTarget.getContentClass(), targetResource.getContentClass());
          Assert.assertEquals(repositoryTarget.getName(), targetResource.getName());

          break;
        }

      }

      if (!found) {

        Assert.fail("Target with ID: " + targetResource.getId() + " could not be found in configuration.");
      }
    }

  }

  public static void removeAllTarget()
      throws IOException
  {
    List<RepositoryTargetListResource> targets = getList();
    for (RepositoryTargetListResource target : targets) {
      Status status =
          RequestFacade.sendMessage("service/local/repo_targets/" + target.getId(), Method.DELETE).getStatus();
      Assert.assertTrue("Failt to delete: " + status.getDescription(), status.isSuccess());
    }
  }

  public static RepositoryTargetResource get(String targetId)
      throws IOException
  {
    String responseText = RequestFacade.doGetForText("service/local/repo_targets/" + targetId);
    LOG.debug("responseText: \n" + responseText);

    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML);

    RepositoryTargetResourceResponse resourceResponse =
        (RepositoryTargetResourceResponse) representation.getPayload(new RepositoryTargetResourceResponse());

    return resourceResponse.getData();
  }

  /**
   * Deletes the target id and ensures the delete was successful
   */
  public static void delete(String targetId)
      throws IOException
  {
    RequestFacade.doDelete("service/local/repo_targets/" + targetId, isSuccessful());
  }

}
