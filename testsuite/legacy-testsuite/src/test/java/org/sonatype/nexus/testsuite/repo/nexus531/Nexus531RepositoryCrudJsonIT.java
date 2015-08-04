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
package org.sonatype.nexus.testsuite.repo.nexus531;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.repositories.RepositoryBaseResourceConverter;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * CRUD tests for JSON request/response.
 */
public class Nexus531RepositoryCrudJsonIT
    extends AbstractNexusIntegrationTest
{

  protected RepositoryMessageUtil messageUtil = new RepositoryMessageUtil(this, this.getJsonXStream(),
      MediaType.APPLICATION_JSON);

  public Nexus531RepositoryCrudJsonIT() {

  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void createRepositoryTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("createTestRepo");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
    // resource.setRepoType( ? )
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    // resource.setAllowWrite( true );
    // resource.setBrowseable( true );
    // resource.setIndexable( true );
    // resource.setNotFoundCacheTTL( 1440 );
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name()); // [snapshot, release] Note: needs param name change
    // resource.setRealmnId(?)
    // resource.setOverrideLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDefaultLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    // only valid for proxy repos resource.setChecksumPolicy( "IGNORE" ); // [ignore, warn, strictIfExists, strict]

    // this also validates
    this.messageUtil.createRepository(resource);
  }

  @Test
  public void readTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("readTestRepo");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Read Test Repo");
    // resource.setRepoType( ? )
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    // resource.setAllowWrite( true );
    // resource.setBrowseable( true );
    // resource.setIndexable( true );
    // resource.setNotFoundCacheTTL( 1440 );
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name()); // [snapshot, release] Note: needs param name change
    // resource.setRealmnId(?)
    // resource.setOverrideLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDefaultLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    // only valid for proxy repos resource.setChecksumPolicy( "IGNORE" ); // [ignore, warn, strictIfExists, strict]

    // this also validates
    this.messageUtil.createRepository(resource); // this currently also calls GET, but that will change

    RepositoryResource responseRepo = (RepositoryResource) this.messageUtil.getRepository(resource.getId());

    // validate they are the same
    this.messageUtil.validateResourceResponse(resource, responseRepo);

  }

  @Test
  public void updateTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("updateTestRepo");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Update Test Repo");
    // resource.setRepoType( ? )
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    // resource.setAllowWrite( true );
    // resource.setBrowseable( true );
    // resource.setIndexable( true );
    // resource.setNotFoundCacheTTL( 1440 );
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name()); // [snapshot, release] Note: needs param name change
    // resource.setRealmnId(?)
    // resource.setOverrideLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDefaultLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    // only valid for proxy repos resource.setChecksumPolicy( "IGNORE" ); // [ignore, warn, strictIfExists, strict]

    // this also validates
    resource = (RepositoryResource) this.messageUtil.createRepository(resource);

    // udpdate the repo
    resource.setRepoPolicy(RepositoryPolicy.SNAPSHOT.name());

    this.messageUtil.updateRepo(resource);

  }

  @Test
  public void deleteTest()
      throws IOException
  {
    RepositoryResource resource = new RepositoryResource();

    resource.setId("deleteTestRepo");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Delete Test Repo");
    // resource.setRepoType( ? )
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    // resource.setAllowWrite( true );
    // resource.setBrowseable( true );
    // resource.setIndexable( true );
    // resource.setNotFoundCacheTTL( 1440 );
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name()); // [snapshot, release] Note: needs param name change
    // resource.setRealmnId(?)
    // resource.setOverrideLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDefaultLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    // only valid for proxy repos resource.setChecksumPolicy( "IGNORE" ); // [ignore, warn, strictIfExists, strict]

    // this also validates
    resource = (RepositoryResource) this.messageUtil.createRepository(resource);

    // now delete it...
    // use the new ID
    Response response = this.messageUtil.sendMessage(Method.DELETE, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not delete Repository: " + response.getStatus());
    }
    Assert.assertNull(getNexusConfigUtil().getRepo(resource.getId()));
  }

  @Test
  public void listTest()
      throws IOException
  {

    RepositoryResource repo = new RepositoryResource();

    repo.setId("listTestRepo");
    repo.setRepoType("hosted"); // [hosted, proxy, virtual]
    repo.setName("List Test Repo");
    repo.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    repo.setRepoPolicy(RepositoryPolicy.RELEASE.name()); // [snapshot, release] Note: needs param name change
    // only valid for proxy repos repo.setChecksumPolicy( "IGNORE" ); // [ignore, warn, strictIfExists, strict]

    // this also validates
    repo = (RepositoryResource) this.messageUtil.createRepository(repo);

    // now get the lists
    List<RepositoryListResource> repos = this.messageUtil.getList();

    for (Iterator<RepositoryListResource> iter = repos.iterator(); iter.hasNext(); ) {
      RepositoryListResource listRepo = iter.next();

      if (listRepo.getId().equals(repo.getId())) {
        Assert.assertEquals(listRepo.getId(), repo.getId());
        Assert.assertEquals(listRepo.getName(), repo.getName());
        Assert.assertEquals(listRepo.getFormat(), repo.getFormat());
        Assert.assertEquals(listRepo.getRepoPolicy(), repo.getRepoPolicy());
        Assert.assertEquals(listRepo.getRepoType(), repo.getRepoType());
        Assert.assertEquals(listRepo.getRemoteUri(), repo.getRemoteStorage());

        String storageURL =
            repo.getDefaultLocalStorageUrl() != null ? repo.getDefaultLocalStorageUrl()
                : repo.getOverrideLocalStorageUrl();

        storageURL = storageURL.endsWith("/") ? storageURL : storageURL + "/";
        String effectiveLocalStorage =
            listRepo.getEffectiveLocalStorageUrl().endsWith("/") ? listRepo.getEffectiveLocalStorageUrl()
                : listRepo.getEffectiveLocalStorageUrl() + "/";

        Assert.assertEquals(effectiveLocalStorage, storageURL);
      }

      // now check all agaist the the cRepo
      CRepository cRepo = getNexusConfigUtil().getRepo(listRepo.getId());

      if (cRepo != null) {
        M2RepositoryConfiguration cM2Repo = getNexusConfigUtil().getM2Repo(listRepo.getId());
        Assert.assertEquals(listRepo.getId(), cRepo.getId());
        Assert.assertEquals(listRepo.getName(), cRepo.getName());
        // Assert.assertEquals( cM2Repo.getType(), listRepo.getFormat() );
        Assert.assertEquals(listRepo.getRepoPolicy(), cM2Repo.getRepositoryPolicy().name());

        log.debug("cRepo.getRemoteStorage(): " + cRepo.getRemoteStorage());
        log.debug("listRepo.getRemoteUri(): " + listRepo.getRemoteUri());

        Assert.assertTrue((cRepo.getRemoteStorage() == null && listRepo.getRemoteUri() == null)
            || (cRepo.getRemoteStorage().getUrl().equals(listRepo.getRemoteUri())));
      }
      else {
        M2LayoutedM1ShadowRepositoryConfiguration cShadow =
            getNexusConfigUtil().getRepoShadow(listRepo.getId());

        Assert.assertEquals(listRepo.getId(), cRepo.getId());
        Assert.assertEquals(listRepo.getName(), cRepo.getName());
        // Assert.assertEquals( cShadow.getType(), this.formatToType( listRepo.getFormat() ) );
        Assert.assertEquals(listRepo.getRepoType(), RepositoryBaseResourceConverter.REPO_TYPE_VIRTUAL);
      }

    }
  }

  // private String formatToType( String format )
  // {
  // Map<String, String> formatToTypeMap = new HashMap<String, String>();
  // formatToTypeMap.put( "maven2", "m1-m2-shadow" );
  // formatToTypeMap.put( "maven1", "m2-m1-shadow" );
  //
  // return formatToTypeMap.get( format );
  // }

}
