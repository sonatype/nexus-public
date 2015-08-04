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
import java.util.List;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.inError;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class RepositoryMessageUtil
    extends ITUtil
{
  public static final String SERVICE_PART = RepositoriesNexusRestClient.SERVICE_PART;

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryMessageUtil.class);

  private static final RepositoriesNexusRestClient REPOSITORY_NRC = new RepositoriesNexusRestClient(
      RequestFacade.getNexusRestClient(),
      new TasksNexusRestClient(RequestFacade.getNexusRestClient()),
      new EventInspectorsUtil(RequestFacade.getNexusRestClient())
  );

  private final RepositoriesNexusRestClient repositoryNRC;

  public RepositoryMessageUtil(AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType) {
    super(test);
    repositoryNRC = new RepositoriesNexusRestClient(
        RequestFacade.getNexusRestClient(),
        new TasksNexusRestClient(RequestFacade.getNexusRestClient()),
        test.getEventInspectorsUtil(),
        xstream,
        mediaType
    );
  }

  public RepositoryBaseResource createRepository(RepositoryBaseResource repo)
      throws IOException
  {
    return repositoryNRC.createRepository(repo);
  }

  public RepositoryBaseResource createRepository(RepositoryBaseResource repo, boolean validate)
      throws IOException
  {
    final RepositoryBaseResource resource = repositoryNRC.createRepository(repo);
    if (validate) {
      validateResourceResponse(repo, resource);
    }
    return resource;
  }

  public void validateResourceResponse(RepositoryBaseResource repo, RepositoryBaseResource responseResource)
      throws IOException
  {
    Assert.assertEquals(responseResource.getId(), repo.getId());
    Assert.assertEquals(responseResource.getName(), repo.getName());
    // Assert.assertEquals( repo.getDefaultLocalStorageUrl(), responseResource.getDefaultLocalStorageUrl() ); //
    // TODO: add check for this

    // format is not used anymore, removing the check
    // Assert.assertEquals( repo.getFormat(), responseResource.getFormat() );
    Assert.assertEquals(responseResource.getRepoType(), repo.getRepoType());

    if (repo.getRepoType().equals("virtual")) {
      // check mirror
      RepositoryShadowResource expected = (RepositoryShadowResource) repo;
      RepositoryShadowResource actual = (RepositoryShadowResource) responseResource;

      Assert.assertEquals(actual.getShadowOf(), expected.getShadowOf());
    }
    else {
      RepositoryResource expected = (RepositoryResource) repo;
      RepositoryResource actual = (RepositoryResource) responseResource;

      // Assert.assertEquals( expected.getChecksumPolicy(), actual.getChecksumPolicy() );

      // TODO: sometimes the storage dir ends with a '/' SEE: NEXUS-542
      if (actual.getDefaultLocalStorageUrl().endsWith("/")) {
        Assert.assertTrue("Unexpected defaultLocalStorage: <expected to end with> " + "/storage/"
            + repo.getId()
            + "/  <actual>" + actual.getDefaultLocalStorageUrl(),
            actual.getDefaultLocalStorageUrl().endsWith("/storage/" + repo.getId() + "/"));
      }
      // NOTE one of these blocks should be removed
      else {
        Assert.assertTrue("Unexpected defaultLocalStorage: <expected to end with> " + "/storage/"
            + repo.getId()
            + "  <actual>" + actual.getDefaultLocalStorageUrl(),
            actual.getDefaultLocalStorageUrl().endsWith("/storage/" + repo.getId()));
      }

      Assert.assertEquals(expected.getNotFoundCacheTTL(), actual.getNotFoundCacheTTL());
      // Assert.assertEquals( expected.getOverrideLocalStorageUrl(), actual.getOverrideLocalStorageUrl() );

      if (expected.getRemoteStorage() == null) {
        Assert.assertNull(actual.getRemoteStorage());
      }
      else {
        Assert.assertEquals(actual.getRemoteStorage().getRemoteStorageUrl(),
            expected.getRemoteStorage().getRemoteStorageUrl());
      }

      Assert.assertEquals(actual.getRepoPolicy(), expected.getRepoPolicy());
    }

    // check nexus.xml
    this.validateRepoInNexusConfig(responseResource);
  }

  public RepositoryBaseResource getRepository(String repoId)
      throws IOException
  {
    // accepted return codes: OK or redirect
    final String responseText = RequestFacade.doGetForText(SERVICE_PART + "/" + repoId, not(inError()));
    LOG.debug("responseText: \n" + responseText);

    // this should use call to: getResourceFromResponse
    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML);

    RepositoryResourceResponse resourceResponse =
        (RepositoryResourceResponse) representation.getPayload(new RepositoryResourceResponse());

    return resourceResponse.getData();
  }

  public RepositoryBaseResource updateRepo(RepositoryBaseResource repo)
      throws IOException
  {
    return updateRepo(repo, true);
  }

  public RepositoryBaseResource updateRepo(RepositoryBaseResource repo, boolean validate)
      throws IOException
  {

    Response response = null;
    RepositoryBaseResource responseResource;
    try {
      response = this.sendMessage(Method.PUT, repo);
      assertThat("Could not update user", response, isSuccessful());
      responseResource = this.getRepositoryBaseResourceFromResponse(response);
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

    if (validate) {
      this.validateResourceResponse(repo, responseResource);
    }

    return responseResource;
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response sendMessage(Method method, RepositoryBaseResource resource, String id)
      throws IOException
  {
    return repositoryNRC.sendMessage(method, resource, id);
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response sendMessage(Method method, RepositoryBaseResource resource)
      throws IOException
  {
    return repositoryNRC.sendMessage(method, resource);
  }

  /**
   * This should be replaced with a REST Call, but the REST client does not set the Accept correctly on GET's/
   */
  public List<RepositoryListResource> getList()
      throws IOException
  {
    return repositoryNRC.getList();
  }

  public List<RepositoryListResource> getAllList()
      throws IOException
  {
    return repositoryNRC.getAllList();
  }

  public RepositoryBaseResource getRepositoryBaseResourceFromResponse(Response response)
      throws IOException
  {
    return repositoryNRC.getRepositoryBaseResourceFromResponse(response);
  }

  public RepositoryResource getResourceFromResponse(Response response)
      throws IOException
  {
    return repositoryNRC.getResourceFromResponse(response);
  }

  private void validateRepoInNexusConfig(RepositoryBaseResource repo)
      throws IOException
  {

    if (repo.getRepoType().equals("virtual")) {
      // check mirror
      RepositoryShadowResource expected = (RepositoryShadowResource) repo;
      CRepository cRepo = getTest().getNexusConfigUtil().getRepo(repo.getId());
      M2LayoutedM1ShadowRepositoryConfiguration cShadowRepo =
          getTest().getNexusConfigUtil().getRepoShadow(repo.getId());

      Assert.assertEquals(cShadowRepo.getMasterRepositoryId(), expected.getShadowOf());
      Assert.assertEquals(cRepo.getId(), expected.getId());
      Assert.assertEquals(cRepo.getName(), expected.getName());

      // cstamas: This is nonsense, this starts in-process (HERE) of nexus internals while IT runs a nexus too,
      // and they start/try to use same FS resources!
      // ContentClass expectedCc =
      // repositoryTypeRegistry.getRepositoryContentClass( cRepo.getProviderRole(), cRepo.getProviderHint() );
      // Assert.assertNotNull( expectedCc,
      // "Unknown shadow repo type='" + cRepo.getProviderRole() + cRepo.getProviderHint()
      // + "'!" );
      // Assert.assertEquals( expected.getFormat(), expectedCc.getId() );
    }
    else {
      RepositoryResource expected = (RepositoryResource) repo;
      CRepository cRepo = getTest().getNexusConfigUtil().getRepo(repo.getId());

      Assert.assertEquals(expected.getId(), cRepo.getId());

      Assert.assertEquals(expected.getName(), cRepo.getName());

      // cstamas: This is nonsense, this starts in-process (HERE) of nexus internals while IT runs a nexus too,
      // and they start/try to use same FS resources!
      // ContentClass expectedCc =
      // repositoryTypeRegistry.getRepositoryContentClass( cRepo.getProviderRole(), cRepo.getProviderHint() );
      // Assert.assertNotNull( expectedCc, "Unknown repo type='" + cRepo.getProviderRole() +
      // cRepo.getProviderHint()
      // + "'!" );
      // Assert.assertEquals( expected.getFormat(), expectedCc.getId() );

      Assert.assertEquals(expected.getNotFoundCacheTTL(), cRepo.getNotFoundCacheTTL());

      if (expected.getOverrideLocalStorageUrl() == null) {
        Assert.assertNull("Expected CRepo localstorage url not be set, because it is the default.",
            cRepo.getLocalStorage().getUrl());
      }
      else {
        String actualLocalStorage =
            cRepo.getLocalStorage().getUrl().endsWith("/") ? cRepo.getLocalStorage().getUrl()
                : cRepo.getLocalStorage().getUrl() + "/";
        String overridLocalStorage =
            expected.getOverrideLocalStorageUrl().endsWith("/") ? expected.getOverrideLocalStorageUrl()
                : expected.getOverrideLocalStorageUrl() + "/";
        Assert.assertEquals(actualLocalStorage, overridLocalStorage);
      }

      if (expected.getRemoteStorage() == null) {
        Assert.assertNull(cRepo.getRemoteStorage());
      }
      else {
        Assert.assertEquals(cRepo.getRemoteStorage().getUrl(),
            expected.getRemoteStorage().getRemoteStorageUrl());
      }

      // check maven repo props (for not just check everything that is a Repository
      if (expected.getProvider().matches("maven[12]")) {
        M2RepositoryConfiguration cM2Repo = getTest().getNexusConfigUtil().getM2Repo(repo.getId());

        if (expected.getChecksumPolicy() != null) {
          Assert.assertEquals(cM2Repo.getChecksumPolicy().name(), expected.getChecksumPolicy());
        }

        Assert.assertEquals(cM2Repo.getRepositoryPolicy().name(), expected.getRepoPolicy());
      }
    }

  }

  public static void updateIndexes(String... repositories)
      throws Exception
  {
    REPOSITORY_NRC.updateIndexes(repositories);
  }

  public static void updateIncrementalIndexes(String... repositories)
      throws Exception
  {
    REPOSITORY_NRC.updateIncrementalIndexes(repositories);
  }

  public RepositoryStatusResource getStatus(String repoId)
      throws IOException
  {
    return repositoryNRC.getStatus(repoId);
  }

  public RepositoryStatusResource getStatus(String repoId, boolean force)
      throws IOException
  {
    return repositoryNRC.getStatus(repoId, force);
  }

  public void updateStatus(RepositoryStatusResource repoStatus)
      throws IOException
  {
    repositoryNRC.updateStatus(repoStatus);
  }

  /**
   * Change block proxy state.<BR>
   * this method only return after all Tasks and Asynchronous events to finish
   */
  public void setBlockProxy(final String repoId, final boolean block)
      throws Exception
  {
    repositoryNRC.setBlockProxy(repoId, block);
  }

  /**
   * Change block out of service state.<BR>
   * this method only return after all Tasks and Asynchronous events to finish
   */
  public void setOutOfServiceProxy(final String repoId, final boolean outOfService)
      throws Exception
  {
    repositoryNRC.setOutOfServiceProxy(repoId, outOfService);
  }

}
