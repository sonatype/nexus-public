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
package org.sonatype.nexus.testsuite.repository;

import java.io.IOException;

import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenGroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.testsuite.client.ClientITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_XML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class GroupRollbackIT
    extends ClientITSupport
{
  private static final String ID_PLACEHOLDER = "$id_placeholder";

  private static final String XML =
      "<repo-group>" +
          "    <data>" +
          "        <contentResourceURI>http://localhost:8081/nexus/content/groups/public</contentResourceURI>" +
          "        <id>" + ID_PLACEHOLDER + "</id>" +
          "        <name>group rollback test repo</name>" +
          "        <provider>maven2</provider>" +
          "        <format>maven2</format>" +
          "        <repoType>group</repoType>" +
          "        <exposed>true</exposed>" +
          "        <repositories>" +
          "            <repo-group-member>" +
          "                <id>central</id>" +
          "                <name>Central</name>" +
          "                <resourceURI>http://localhost:8081/nexus/service/local/repo_groups/public/central</resourceURI>\n" +
          "            </repo-group-member>" +
          "            <repo-group-member>" +
          "                <id>does-not-exist</id>" +
          "                <name>does-not-exist</name>" +
          "                <resourceURI>http://localhost:8081/nexus/service/local/repo_groups/public/does-not-exist</resourceURI>\n" +
          "            </repo-group-member>" +
          "        </repositories>" +
          "    </data>" +
          "</repo-group>";

  public GroupRollbackIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /*
   * Test for https://issues.sonatype.org/browse/NEXUS-16487
   *
   * The update method for a group repository starts by clearing out the member ids and then adding each member
   * supplied in the update payload. If one of the repositories doesn't exist an exception is thrown however the state
   * is still stored in memory with the member repositories cleared. Before the fix, if a save for any other repository
   * came through it would also write the configuration for the group repository, therefore removing all members
   * permanently.
   */
  @Test
  public void rollbackWhenMemberNotFound() throws Exception {
    String groupId = repositoryIdForTest("group");

    String hostedId = repositoryIdForTest("hosted");
    MavenHostedRepository hostedRepository = repositories().create(MavenHostedRepository.class, hostedId)
        .save();

    MavenGroupRepository groupRepository = repositories().create(MavenGroupRepository.class, groupId)
        .ofRepositories("central")
        .save();

    //Update the group repository with invalid XML so that it fails
    updateGroupRepositoryViaREST(groupId);

    // Triggering the save of any repository will trigger a save of the "application configuration" which is stored in
    // memory.
    hostedRepository.withName("updated-name")
        .save();

    groupRepository.refresh();

    assertThat(groupRepository.memberRepositories(), is(not(empty())));
  }

  @Test(expected = NexusClientNotFoundException.class)
  public void doNotCreateRepositoryWhenMemberNotFound() throws Exception {
    String groupId = repositoryIdForTest("group");

    //Create the group with XML that contains a member that does not exist so that it fails.
    createGroupViaREST(groupId);

    repositories().get(groupId);
  }

  private void updateGroupRepositoryViaREST(String repositoryId) throws IOException {
    final String groupUpdateUrl = nexus().getUrl() + "service/local/repo_groups/" + repositoryId;
    HttpPut put = new HttpPut(groupUpdateUrl);
    put.addHeader(CONTENT_TYPE, APPLICATION_XML.getMimeType());
    put.addHeader(ACCEPT, APPLICATION_XML.getMimeType());
    put.setEntity(new StringEntity(XML.replace(ID_PLACEHOLDER, repositoryId), "utf-8"));

    sendFailingRequest(put);
  }

  private void createGroupViaREST(String repositoryId) throws IOException {
    final String groupUpdateUrl = nexus().getUrl() + "service/local/repo_groups";
    HttpPost post = new HttpPost(groupUpdateUrl);
    post.addHeader(CONTENT_TYPE, APPLICATION_XML.getMimeType());
    post.addHeader(ACCEPT, APPLICATION_XML.getMimeType());
    post.setEntity(new StringEntity(XML.replace(ID_PLACEHOLDER, repositoryId), "utf-8"));

    sendFailingRequest(post);
  }

  private void sendFailingRequest(final HttpUriRequest request) throws IOException {
    HttpClientBuilder builder = HttpClients.custom();
    builder.setDefaultCredentialsProvider(credentialsProvider());

    try (CloseableHttpClient client = builder.build()) {
      try (CloseableHttpResponse response = client.execute(request)) {
        assertThat(response.getStatusLine().getStatusCode(), equalTo(400));
      }
    }
  }

  private CredentialsProvider credentialsProvider() {
    String hostname = nexus().getConfiguration().getHostName();
    AuthScope scope = new AuthScope(hostname, -1);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(scope, credentials());
    return credentialsProvider;
  }

  private Credentials credentials() {
    return new UsernamePasswordCredentials("admin", "admin123");
  }
}
