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
import java.io.IOException;

import org.sonatype.nexus.integrationtests.NexusRestClient;
import org.sonatype.nexus.integrationtests.TestContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.wagon.Wagon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class DeployUtils
{

  private static final Logger LOG = LoggerFactory.getLogger(DeployUtils.class);

  private final NexusRestClient nexusRestClient;

  private final WagonDeployer.Factory wagonFactory;

  public DeployUtils(final NexusRestClient nexusRestClient) {
    this.nexusRestClient = checkNotNull(nexusRestClient);
    this.wagonFactory = null;
  }

  public DeployUtils(final NexusRestClient nexusRestClient,
                     final WagonDeployer.Factory wagonFactory)
  {
    this.nexusRestClient = checkNotNull(nexusRestClient);
    this.wagonFactory = checkNotNull(wagonFactory);
  }

  public void deployWithWagon(final String wagonHint,
                              final String repositoryUrl,
                              final File fileToDeploy,
                              final String artifactPath)
      throws Exception
  {
    checkState(wagonFactory != null, "Wagon factory must be provided to be able to deploy");

    final TestContext testContext = nexusRestClient.getTestContext();
    final Wagon wagon = wagonFactory.get(wagonHint);

    String username = null;
    String password = null;

    if (testContext.isSecureTest()) {
      username = testContext.getUsername();
      password = testContext.getPassword();
    }

    new WagonDeployer(
        wagon, wagonHint, username, password, repositoryUrl, fileToDeploy, artifactPath,
        nexusRestClient.getTestContext()).deploy();
  }

  public int deployUsingGavWithRest(final String repositoryId,
                                    final Gav gav,
                                    final File fileToDeploy)
      throws IOException
  {
    return deployUsingGavWithRest(
        nexusRestClient.toNexusURL("service/local/artifact/maven/content").toExternalForm(),
        repositoryId,
        gav,
        fileToDeploy);
  }

  public int deployUsingGavWithRest(final String restServiceURL,
                                    final String repositoryId,
                                    final Gav gav,
                                    final File fileToDeploy)
      throws IOException
  {
    return deployWithRest(
        repositoryId,
        gav.getGroupId(),
        gav.getArtifactId(),
        gav.getVersion(),
        gav.getClassifier(),
        gav.getExtension(),
        fileToDeploy);
  }

  public int deployUsingPomWithRest(final String repositoryId,
                                    final File fileToDeploy,
                                    final File pomFile,
                                    final String classifier,
                                    final String extension)
      throws IOException
  {
    return deployUsingPomWithRest(
        nexusRestClient.toNexusURL("service/local/artifact/maven/content").toExternalForm(),
        repositoryId,
        fileToDeploy,
        pomFile,
        classifier,
        extension);
  }

  public HttpResponse deployUsingPomWithRestReturnResult(final String repositoryId,
                                                         final File fileToDeploy,
                                                         final File pomFile,
                                                         final String classifier,
                                                         final String extension)
      throws IOException
  {
    return deployUsingPomWithRestReturnResult(
        nexusRestClient.toNexusURL("service/local/artifact/maven/content").toExternalForm(),
        repositoryId,
        fileToDeploy,
        pomFile,
        classifier,
        extension);
  }

  public HttpResponse deployUsingPomWithRestReturnResult(final String restServiceURL,
                                                         final String repositoryId,
                                                         final File fileToDeploy,
                                                         final File pomFile,
                                                         final String classifier,
                                                         final String extension)
      throws IOException
  {
    // the method we are calling
    final HttpPost filePost = new HttpPost(restServiceURL);
    filePost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
    final MultipartEntity multipartEntity = new MultipartEntity();

    final String fixedClassifier = (classifier == null) ? "" : classifier;
    final String fixedExtension = (extension == null) ? "" : extension;

    multipartEntity.addPart("r", new StringBody(repositoryId));
    multipartEntity.addPart("e", new StringBody(fixedExtension));
    multipartEntity.addPart("c", new StringBody(fixedClassifier));
    multipartEntity.addPart("hasPom", new StringBody(Boolean.TRUE.toString()));
    multipartEntity.addPart(pomFile.getName(), new FileBody(pomFile));
    multipartEntity.addPart(fileToDeploy.getName(), new FileBody(fileToDeploy));

    filePost.setEntity(multipartEntity);

    LOG.debug("URL:  " + restServiceURL);
    LOG.debug("Method: Post");
    LOG.debug("params: ");
    LOG.debug("\tr: " + repositoryId);
    LOG.debug("\thasPom: true");
    LOG.debug("\tpom: " + pomFile);
    LOG.debug("\tfileToDeploy: " + fileToDeploy);

    return nexusRestClient.executeHTTPClientMethod(filePost);
  }

  public int deployUsingPomWithRest(final String restServiceURL,
                                    final String repositoryId,
                                    final File fileToDeploy,
                                    final File pomFile,
                                    final String classifier,
                                    final String extension)
      throws IOException
  {
    return deployUsingPomWithRestReturnResult(
        restServiceURL,
        repositoryId,
        fileToDeploy,
        pomFile,
        classifier,
        extension).getStatusLine().getStatusCode();
  }

  public HttpResponse deployPomWithRest(final String repositoryId,
                                        final File pomFile)
      throws IOException
  {
    // the method we are calling
    final HttpPost filePost =
        new HttpPost(nexusRestClient.toNexusURL("service/local/artifact/maven/content").toExternalForm());
    filePost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
    final MultipartEntity multipartEntity = new MultipartEntity();

    multipartEntity.addPart("r", new StringBody(repositoryId));
    multipartEntity.addPart("hasPom", new StringBody(Boolean.TRUE.toString()));
    multipartEntity.addPart(pomFile.getName(), new FileBody(pomFile));

    filePost.setEntity(multipartEntity);

    LOG.debug("URL:  " + filePost.getURI());
    LOG.debug("Method: Post");
    LOG.debug("params: ");
    LOG.debug("\tr: " + repositoryId);
    LOG.debug("\thasPom: true");
    LOG.debug("\tpom: " + pomFile);

    return nexusRestClient.executeHTTPClientMethod(filePost);
  }

  public int deployWithRest(final String repositoryId,
                            final String groupId,
                            final String artifactId,
                            final String version,
                            final String classifier,
                            final String extension,
                            final File fileToDeploy)
      throws IOException
  {
    return deployWithRest(
        nexusRestClient.toNexusURL("service/local/artifact/maven/content").toExternalForm(),
        repositoryId,
        groupId,
        artifactId,
        version,
        classifier,
        extension,
        fileToDeploy);
  }

  public int deployWithRest(final String restServiceURL,
                            final String repositoryId,
                            final String groupId,
                            final String artifactId,
                            final String version,
                            final String classifier,
                            final String extension,
                            final File fileToDeploy)
      throws IOException
  {
    // the method we are calling
    final HttpPost filePost = new HttpPost(restServiceURL);
    filePost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
    final MultipartEntity multipartEntity = new MultipartEntity();

    multipartEntity.addPart("r", new StringBody(repositoryId));
    multipartEntity.addPart("g", new StringBody(groupId));
    multipartEntity.addPart("a", new StringBody(artifactId));
    multipartEntity.addPart("v", new StringBody(version));
    multipartEntity.addPart("p", new StringBody(extension == null ? "" : extension));
    multipartEntity.addPart("c", new StringBody(classifier == null ? "" : classifier));
    multipartEntity.addPart(fileToDeploy.getName(), new FileBody(fileToDeploy));

    filePost.setEntity(multipartEntity);

    return nexusRestClient.executeHTTPClientMethod(filePost).getStatusLine().getStatusCode();
  }
}
