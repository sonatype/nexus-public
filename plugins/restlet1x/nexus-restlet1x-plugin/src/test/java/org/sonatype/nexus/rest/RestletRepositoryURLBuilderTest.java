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
package org.sonatype.nexus.rest;

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.maven1.M1Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class RestletRepositoryURLBuilderTest
{
  private static final String MOCK_REPO_ID = "test-id";

  private static final String MOCK_GROUP_ID = "test-group-id";

  private static final String NOT_FOUND_REPO_ID = "not-found-mock-id";

  private static final String GLOBAL_BASE_URL = "http://global/baseurl";

  private static final String MOCK_PATH_PREFIX = "mockprefix";

  private Repository repository;

  private Repository group;

  private RepositoryRegistry repositoryRegistry;

  private RepositoryTypeRegistry repositoryTypeRegistry;

  private GlobalRestApiSettings globalRestApiSettings;

  @Before
  public void setUp()
      throws Exception
  {
    repository = Mockito.mock(Repository.class);
    Mockito.doReturn(MOCK_REPO_ID).when(repository).getId();
    Mockito.doReturn(M2Repository.class.getName()).when(repository).getProviderRole();
    Mockito.doReturn(MOCK_REPO_ID).when(repository).getPathPrefix();
    Mockito.doReturn(M2Repository.class.getName()).when(repository).getProviderRole();
    Mockito.doReturn("my-hint").when(repository).getProviderHint();

    group = Mockito.mock(GroupRepository.class);
    Mockito.doReturn(MOCK_GROUP_ID).when(group).getId();
    Mockito.doReturn(M2GroupRepository.class.getName()).when(group).getProviderRole();
    Mockito.doReturn(MOCK_REPO_ID).when(group).getPathPrefix();

    repositoryRegistry = Mockito.mock(RepositoryRegistry.class);
    Mockito.doReturn(repository).when(repositoryRegistry).getRepository(MOCK_REPO_ID);
    Mockito.doReturn(group).when(repositoryRegistry).getRepository(MOCK_GROUP_ID);
    Mockito.doThrow(new NoSuchRepositoryException(NOT_FOUND_REPO_ID)).when(repositoryRegistry).getRepository(
        NOT_FOUND_REPO_ID);

    globalRestApiSettings = Mockito.mock(GlobalRestApiSettings.class);

    Set<RepositoryTypeDescriptor> typeDescriptors = new HashSet<RepositoryTypeDescriptor>();
    RepositoryTypeDescriptor myHintRtd;
    RepositoryTypeDescriptor invalidRtd;
    typeDescriptors.add(myHintRtd = new RepositoryTypeDescriptor(M2Repository.class, "my-hint", MOCK_PATH_PREFIX));
    typeDescriptors.add(invalidRtd = new RepositoryTypeDescriptor(M1Repository.class, "invalid", "invalid"));

    repositoryTypeRegistry = Mockito.mock(RepositoryTypeRegistry.class);
    Mockito.doReturn(myHintRtd).when(repositoryTypeRegistry).getRepositoryTypeDescriptor(
        M2Repository.class.getName(), "my-hint");
    Mockito.doReturn(invalidRtd).when(repositoryTypeRegistry).getRepositoryTypeDescriptor(
        M1Repository.class.getName(), "invalid");
    Mockito.doReturn(typeDescriptors).when(repositoryTypeRegistry).getRegisteredRepositoryTypeDescriptors();
  }

  @Test
  public void testForceBaseUrlById()
      throws Exception
  {

    Mockito.doReturn(true).when(globalRestApiSettings).isEnabled();
    Mockito.doReturn(true).when(globalRestApiSettings).isForceBaseUrl();
    Mockito.doReturn(GLOBAL_BASE_URL).when(globalRestApiSettings).getBaseUrl();

    RestletRepositoryURLBuilder urlFinder =
        new RestletRepositoryURLBuilder(repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings);

    Assert.assertEquals(GLOBAL_BASE_URL + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
        urlFinder.getRepositoryContentUrl(MOCK_REPO_ID));
  }

  @Test
  public void testForceBaseUrlByIdGlobalDisabled()
      throws Exception
  {

    Mockito.doReturn(false).when(globalRestApiSettings).isEnabled();
    Mockito.doReturn(true).when(globalRestApiSettings).isForceBaseUrl();
    Mockito.doReturn(GLOBAL_BASE_URL).when(globalRestApiSettings).getBaseUrl();

    RestletRepositoryURLBuilder urlFinder =
        new RestletRepositoryURLBuilder(repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings);

    Assert.assertEquals(GLOBAL_BASE_URL + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
        urlFinder.getRepositoryContentUrl(MOCK_REPO_ID));
  }

  @Test
  public void testNoRequestBaseURLNotSet()
      throws Exception
  {
    Mockito.doReturn(true).when(globalRestApiSettings).isEnabled();
    Mockito.doReturn(false).when(globalRestApiSettings).isForceBaseUrl();
    Mockito.doReturn(null).when(globalRestApiSettings).getBaseUrl();

    RestletRepositoryURLBuilder urlFinder =
        new RestletRepositoryURLBuilder(repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings);

    Assert.assertNull(urlFinder.getRepositoryContentUrl(MOCK_REPO_ID));
  }

  @Test
  public void testBaseUrlGlobalNoRequest()
      throws Exception
  {

    Mockito.doReturn(true).when(globalRestApiSettings).isEnabled();
    Mockito.doReturn(false).when(globalRestApiSettings).isForceBaseUrl();
    Mockito.doReturn(GLOBAL_BASE_URL).when(globalRestApiSettings).getBaseUrl();

    RestletRepositoryURLBuilder urlFinder =
        new RestletRepositoryURLBuilder(repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings);

    Assert.assertEquals(GLOBAL_BASE_URL + "/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
        urlFinder.getRepositoryContentUrl(MOCK_REPO_ID));
  }

  @Test
  public void testBaseUrlGlobalRequest()
      throws Exception
  {
    // NEXUS-6045: Restlet1x request cannot come in from anywhere else than /service/local anymore
    String restletBaseURL = "http://from/request/service/local";

    Mockito.doReturn(true).when(globalRestApiSettings).isEnabled();
    Mockito.doReturn(false).when(globalRestApiSettings).isForceBaseUrl();
    Mockito.doReturn(GLOBAL_BASE_URL).when(globalRestApiSettings).getBaseUrl();

    try {
      Request request = new Request();
      request.setRootRef(new Reference(restletBaseURL));
      Response response = new Response(request);
      Response.setCurrent(response);

      RestletRepositoryURLBuilder urlFinder =
          new RestletRepositoryURLBuilder(repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings);

      // NEXUS-6045: Restlet1x request will point to the webapp root/content
      Assert.assertEquals("http://from/request/content/" + MOCK_PATH_PREFIX + "/" + MOCK_REPO_ID,
          urlFinder.getRepositoryContentUrl(MOCK_REPO_ID));
    }
    finally {
      Response.setCurrent(null);
    }
  }

  @Test(expected = NoSuchRepositoryException.class)
  public void testNotFound()
      throws NoSuchRepositoryException
  {
    RestletRepositoryURLBuilder urlFinder =
        new RestletRepositoryURLBuilder(repositoryRegistry, repositoryTypeRegistry, globalRestApiSettings);

    urlFinder.getRepositoryContentUrl(NOT_FOUND_REPO_ID);
  }
}
