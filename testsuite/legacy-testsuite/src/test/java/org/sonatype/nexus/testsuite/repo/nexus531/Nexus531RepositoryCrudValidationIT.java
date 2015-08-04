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

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import com.thoughtworks.xstream.converters.ConversionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus531RepositoryCrudValidationIT
    extends AbstractPrivilegeTest
{

  private RepositoryMessageUtil messageUtil = new RepositoryMessageUtil(this, this.getXMLXStream(),
      MediaType.APPLICATION_XML);

  @BeforeClass
  public static void setSecureTest()
      throws ComponentLookupException
  {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void createNoCheckSumTest()
      throws IOException
  {
    RepositoryResource resource = new RepositoryResource();

    resource.setId("createNoCheckSumTest");
    resource.setRepoType("proxy"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
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
    // resource.setChecksumPolicy( "ignore" ); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Repo should not have been created: " + response.getStatus() + "\n" + responseText);
    }
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.contains("<errors>"));
  }

  @Test
  public void createNoRepoPolicyTest()
      throws IOException
  {
    RepositoryResource resource = new RepositoryResource();

    resource.setId("createNoRepoPolicyTest");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    // resource.setAllowWrite( true );
    // resource.setBrowseable( true );
    // resource.setIndexable( true );
    // resource.setNotFoundCacheTTL( 1440 );
    // resource.setRepoPolicy( "release" ); // [snapshot, release] Note: needs param name change
    // resource.setRealmnId(?)
    // resource.setOverrideLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDefaultLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    // resource.setChecksumPolicy( "IGNORE" ); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Repo should not have been created: " + response.getStatus() + "\n" + responseText);
    }
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.contains("<errors>"));
  }

  @Test
  public void createNoRepoTypeTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("createNoRepoTypeTest");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
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
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    Assert.assertTrue("Expected RepoType to default: " + response.getStatus()
        + "\n" + responseText, response.getStatus().isSuccess());
    // change in functionality
    // Assert.assertFalse( "Expected failure: "+ response.getStatus()+"\n"+responseText,
    // response.getStatus().isSuccess() );
  }

  @Test
  public void noRepoTypeSerializationError()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("createNoRepoTypeTest");
    // resource.setRepoType( "hosted" ); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
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
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]

    try {
      this.messageUtil.sendMessage(Method.POST, resource);
      Assert.fail("Expected to throw ConversionException");
    }
    catch (ConversionException e) {
      // expected
    }
  }

  @Test
  public void createNoIdTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
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
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    Assert.assertFalse("Repo should not have been created: " + response.getStatus() + "\n" + responseText,
        response.getStatus().isSuccess());
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.contains("<errors>"));

    // with null

    resource.setId(null);

    response = this.messageUtil.sendMessage(Method.POST, resource);
    responseText = response.getEntity().getText();

    Assert.assertFalse("Repo should not have been created: " + response.getStatus() + "\n" + responseText,
        response.getStatus().isSuccess());
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.contains("<errors>"));

  }

  @Test
  public void createNoNameTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("createNoNameTest");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("");
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
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    Assert.assertTrue("Expected name to default." + responseText, response.getStatus().isSuccess());
    Assert.assertTrue("Expected name to default to id",
        this.messageUtil.getRepository("createNoNameTest").getName().equals("createNoNameTest"));

    // with null
    resource.setId("createNoNameTestnull");
    resource.setName(null);

    response = this.messageUtil.sendMessage(Method.POST, resource);
    responseText = response.getEntity().getText();

    Assert.assertTrue("Expected name to default." + responseText, response.getStatus().isSuccess());
    Assert.assertTrue(
        "Expected name to default to id",
        this.messageUtil.getRepository("createNoNameTestnull").getName().equals("createNoNameTestnull"));

  }

  @Test
  public void createJunkOverrideUrlTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("createJunkOverrideUrlTest");
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    resource.setFormat("maven2"); // Repository Format, maven1, maven2, maven-site, eclipse-update-site
    // resource.setAllowWrite( true );
    // resource.setBrowseable( true );
    // resource.setIndexable( true );
    // resource.setNotFoundCacheTTL( 1440 );
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name()); // [snapshot, release] Note: needs param name change
    // resource.setRealmnId(?)
    resource.setOverrideLocalStorageUrl("foo.bar"); // file://repos/internal
    // resource.setDefaultLocalStorageUrl( "" ); //file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    Assert.assertFalse("Repo should not have been created: " + response.getStatus() + "\n" + responseText,
        response.getStatus().isSuccess());
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.contains("<errors>"));
  }

  @Test
  public void createJunkDefaultStorageUrlTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("createJunkDefaultStorageUrlTest");
    resource.setRepoType("proxy"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
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
    resource.setDefaultLocalStorageUrl("foo.bar"); // file://repos/internal
    // resource.setDownloadRemoteIndexes( true );
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    Assert.assertTrue("Expected DefaultLocalStorageUrl to be ignored on create"
        + response.getStatus() + responseText, response.getStatus().isSuccess());
  }

  @Test
  public void updateValidatioinTest()
      throws IOException
  {
    RepositoryResource resource = new RepositoryResource();

    resource.setId("updateValidatioinTest");
    resource.setRepoType("proxy"); // [hosted, proxy, virtual]
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
    resource.setChecksumPolicy("IGNORE"); // [ignore, warn, strictIfExists, strict]
    RepositoryResourceRemoteStorage remote = new RepositoryResourceRemoteStorage();
    remote.setRemoteStorageUrl("http://localhost:123/remote_resource_repo/");
    resource.setRemoteStorage(remote);

    // this also validates
    resource = (RepositoryResource) this.messageUtil.createRepository(resource);

    // invalid policy
    resource.setRepoPolicy("junk");
    this.sendAndExpectError(Method.PUT, resource);
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());

    // invalid policy
    resource.setRepoPolicy("junk");
    Response response = this.messageUtil.sendMessage(Method.PUT, resource);
    this.sendAndExpectError(Method.PUT, resource);
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());

    // no policy
    resource.setRepoPolicy(null);
    this.sendAndExpectError(Method.PUT, resource);
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());

    // invalid override local storage
    resource.setOverrideLocalStorageUrl("foo.bar");
    this.sendAndExpectError(Method.PUT, resource);
    resource.setOverrideLocalStorageUrl(null);

    // invalid checksum
    resource.setChecksumPolicy("JUNK");
    this.sendAndExpectError(Method.PUT, resource);
    resource.setChecksumPolicy("IGNORE");

    // no checksum
    resource.setChecksumPolicy(null);
    this.sendAndExpectError(Method.PUT, resource);
    resource.setChecksumPolicy("IGNORE");

    // FIXME: these tests are disabled... NEXUS-741 NEXUS-740
    if (!this.printKnownErrorButDoNotFail(this.getClass(), "updateValidatioinTest")) {

      // invalid repoType
      resource.setRepoType("junk");
      this.sendAndExpectError(Method.PUT, resource);
      resource.setRepoType("hosted");

      // empty name
      resource.setName("");
      this.sendAndExpectError(Method.PUT, resource);
      resource.setName("Update Test Repo");

      // null name
      resource.setName(null);
      this.sendAndExpectError(Method.PUT, resource);
      resource.setName("Update Test Repo");

      // change id
      resource.setId("newId");

      response = this.messageUtil.sendMessage(Method.PUT, resource, "updateValidatioinTest");
      String responseText = response.getEntity().getText();

      Assert.assertFalse("Repo should not have been updated: " + response.getStatus() + "\n" + responseText,
          response.getStatus().isSuccess());
      Assert.assertTrue("Response text did not contain an error message. Status: " + response.getStatus()
          + "\nResponse Text:\n " + responseText,
          responseText.contains("<errors>"));
      resource.setId("updateValidatioinTest");

    }
  }

  private void sendAndExpectError(Method method, RepositoryBaseResource resource)
      throws IOException
  {
    Response response = this.messageUtil.sendMessage(method, resource);
    String responseText = response.getEntity().getText();

    Assert.assertFalse("Repo should not have been updated: " + response.getStatus() + "\n" + responseText,
        response.getStatus().isSuccess());
    Assert.assertTrue("Response text did not contain an error message. "
        + response.getStatus() + "\nResponse Text:\n " + responseText, responseText.contains("<errors>"));
  }

}
