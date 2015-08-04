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

import org.sonatype.nexus.integrationtests.TestContext;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Due to a <a href='http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4941958'>bug</a> ( i wouldn't call this a
 * feature request ) You cannot change change/clear the cache of the default Authenticator, and that is what the Wagon
 * uses. So to work around this very stupid problem I am forking the VM to do a deploy. </p> I wanted to be able to
 * catch each exception, yes, there are better ways to do this, i know.... but this was really easy... and its only for
 * testing... </p> We can look into using the forked app-booter, but that might be a little over kill, and we still
 * couldn't trap the individual exceptions.
 */
public class WagonDeployer
{

  private Wagon wagon;

  private String protocol = "http";

  private String username;

  private String password;

  private String repositoryUrl;

  private File fileToDeploy;

  private String artifactPath;

  private final TestContext testContext;

  private static final Logger LOG = LoggerFactory.getLogger(WagonDeployer.class);

  public WagonDeployer(final Wagon wagon,
                       final String protocol,
                       final String username,
                       final String password,
                       final String repositoryUrl,
                       final File fileToDeploy,
                       final String artifactPath,
                       final TestContext testContext)
  {
    super();
    this.wagon = wagon;
    this.protocol = protocol;
    this.username = username;
    this.password = password;
    this.repositoryUrl = repositoryUrl;
    this.fileToDeploy = fileToDeploy;
    this.artifactPath = artifactPath;
    this.testContext = testContext;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public File getFileToDeploy() {
    return fileToDeploy;
  }

  public String getArtifactPath() {
    return artifactPath;
  }

  public void deploy()
      throws ComponentLookupException, ConnectionException, AuthenticationException, TransferFailedException,
             ResourceDoesNotExistException, AuthorizationException
  {
    Repository repository = new Repository();
    repository.setUrl(repositoryUrl);

    wagon.connect(repository, getWagonAuthenticationInfo());
    wagon.put(fileToDeploy, artifactPath);
    wagon.disconnect();

  }

  public AuthenticationInfo getWagonAuthenticationInfo() {
    AuthenticationInfo authInfo = null;
    // check the text context to see if this is a secure test
    if (testContext.isSecureTest()) {
      authInfo = new AuthenticationInfo();
      authInfo.setUserName(testContext.getUsername());
      authInfo.setPassword(testContext.getPassword());
    }
    return authInfo;
  }

  public static interface Factory
  {
    Wagon get(String protocol);
  }
}
