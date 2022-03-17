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
package org.sonatype.nexus.testsuite.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.exception.NexusClientAccessForbiddenException;
import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.exception.NexusClientErrorResponseException;
import org.sonatype.nexus.client.core.exception.NexusClientException;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.filter.NexusClientExceptionsConverterFilter;
import org.sonatype.nexus.client.core.subsystem.Restlet1xClient;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveRequest;
import org.sonatype.nexus.client.core.subsystem.artifact.ResolveResponse;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenGroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenM1VirtualRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;
import org.sonatype.nexus.client.core.subsystem.routing.Status;
import org.sonatype.nexus.client.core.subsystem.security.Privilege;
import org.sonatype.nexus.client.core.subsystem.security.Role;
import org.sonatype.nexus.client.core.subsystem.security.User;
import org.sonatype.nexus.client.core.subsystem.security.Users;
import org.sonatype.nexus.client.core.subsystem.targets.RepositoryTarget;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.ArtifactResolveResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.security.rest.model.UserListResourceResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;
import org.sonatype.sisu.siesta.client.Filters;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.matchSha1;

public class ClientIT
    extends ClientITSupport
{

  private static final String AOP_POM = "aopalliance/aopalliance/1.0/aopalliance-1.0.pom";

  private static final String AOP_JAR = "aopalliance/aopalliance/1.0/aopalliance-1.0.jar";

  private static final String AOP_META = "aopalliance/aopalliance/maven-metadata.xml";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public ClientIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void artifactMavenResolveSuccess()
      throws IOException
  {
    final MavenHostedRepository repository = repositories()
        .create(MavenHostedRepository.class, repositoryIdForTest())
        .save();

    upload(repository.id(), AOP_POM);
    upload(repository.id(), AOP_JAR);
    upload(repository.id(), AOP_META);

    final ResolveResponse response = artifacts().resolve(
        new ResolveRequest(
            repository.id(), "aopalliance", "aopalliance", ResolveRequest.VERSION_RELEASE
        )
    );
    assertThat(response, is(notNullValue()));
    assertThat(response.getGroupId(), is("aopalliance"));
    assertThat(response.getArtifactId(), is("aopalliance"));
    assertThat(response.getExtension(), is("jar"));
    assertThat(response.isSnapshot(), is(false));
  }

  @Test
  public void artifactMavenResolveFailure() {
    final MavenHostedRepository repository = repositories()
        .create(MavenHostedRepository.class, repositoryIdForTest())
        .save();

    thrown.expect(NexusClientNotFoundException.class);
    artifacts().resolve(
        new ResolveRequest(
            repository.id(), "com.sonatype.nexus.plugin", "nexus-staging-plugin", ResolveRequest.VERSION_RELEASE
        )
    );
  }

  @Test
  public void successfulUploadAndDownloadAndDelete()
      throws IOException
  {
    final MavenHostedRepository repository = repositories()
        .create(MavenHostedRepository.class, repositoryIdForTest())
        .save();

    final Location location = repositoryLocation(repository.id(), AOP_POM);

    final File toDeploy = testData().resolveFile("artifacts/" + AOP_POM);
    final File downloaded = new File(testIndex().getDirectory("downloads"), "aopalliance-1.0.pom");

    content().upload(location, toDeploy);
    content().download(location, downloaded);

    assertThat(downloaded, matchSha1(toDeploy));

    content().delete(location);
  }

  @Test
  public void wrongUploadLocation()
      throws IOException
  {
    thrown.expect(NexusClientNotFoundException.class);
    thrown.expectMessage(
        "Inexistent path: repositories/foo/aopalliance/aopalliance/1.0/aopalliance-1.0.pom"
    );
    content().upload(repositoryLocation("foo", AOP_POM), testData().resolveFile("artifacts/" + AOP_POM));
  }

  @Test
  public void wrongDownloadLocation()
      throws IOException
  {
    thrown.expect(NexusClientNotFoundException.class);
    thrown.expectMessage(
        "Inexistent path: repositories/foo/aopalliance/aopalliance/1.0/aopalliance-1.0.pom"
    );
    content().download(
        repositoryLocation("foo", AOP_POM),
        new File(testIndex().getDirectory("downloads"), "aopalliance-1.0.pom")
    );
  }

  @Test
  public void wrongDeleteLocation()
      throws IOException
  {
    thrown.expect(NexusClientNotFoundException.class);
    thrown.expectMessage(
        "Inexistent path: repositories/foo/aopalliance/aopalliance/1.0/aopalliance-1.0.pom"
    );
    content().delete(repositoryLocation("foo", AOP_POM));
  }

  @Test
  public void convert400WithoutErrorMessage() {
    final JerseyNexusClient client = (JerseyNexusClient) client();
    try {
      client.serviceResource("artifact/maven/resolve").get(ArtifactResolveResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      final NexusClientException converted = client.convertIfKnown(e);
      assertThat(converted, is(instanceOf(NexusClientBadRequestException.class)));
    }
  }

  @Test
  public void convertAlwaysReturnsAnException() {
    final JerseyNexusClient client = (JerseyNexusClient) client();
    try {
      client.serviceResource("artifact/maven/resolve").get(ArtifactResolveResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      final NexusClientException converted = client.convert(e);
      assertThat(converted, is(notNullValue()));
    }
  }

  @Test
  public void entityIsNotConsumed() {
    final JerseyNexusClient client = (JerseyNexusClient) client();
    try {
      client.serviceResource("artifact/maven/resolve").get(ArtifactResolveResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      client.convertIfKnown(e);
      assertThat(e.getResponse().hasEntity(), is(true));
      assertThat(e.getResponse().getEntity(String.class), is(notNullValue()));
    }
  }

  @Test
  public void convert404() {
    final JerseyNexusClient client = (JerseyNexusClient) client();
    try {
      client.serviceResource("repositories/foo").get(RepositoryResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      final NexusClientException converted = client.convertIfKnown(e);
      assertThat(converted, is(instanceOf(NexusClientNotFoundException.class)));

      // do it again so we ensure we consumed and such connection is available
      try {
        client.serviceResource("repositories/foo").get(RepositoryResourceResponse.class);
      }
      catch (UniformInterfaceException e1) {
        final NexusClientException converted1 = client.convertIfKnown(e);
        assertThat(converted1, is(instanceOf(NexusClientNotFoundException.class)));
      }
    }
  }

  @Test
  public void convert403() {
    final JerseyNexusClient client = (JerseyNexusClient) createNexusClient(
        nexus(), "deployment", "deployment123"
    );
    try {
      client.serviceResource("users").get(UserListResourceResponse.class);
    }
    catch (UniformInterfaceException e) {
      final NexusClientException converted = client.convertIfKnown(e);
      assertThat(converted, is(instanceOf(NexusClientAccessForbiddenException.class)));

      // do it again so we ensure we consumed and such connection is available
      try {
        client.serviceResource("users").get(UserListResourceResponse.class);
      }
      catch (UniformInterfaceException e1) {
        final NexusClientException converted1 = client.convertIfKnown(e);
        assertThat(converted1, is(instanceOf(NexusClientAccessForbiddenException.class)));
      }
    }
  }

  @Test
  public void getInexistentRepository() {
    thrown.expect(NexusClientNotFoundException.class);
    thrown.expectMessage("Repository with id 'getInexistentRepository' was not found");
    repositories().get(repositoryIdForTest());
  }

  @Test
  public void getHosted() {
    final Repository repository = repositories().get("releases");
    assertThat(repository, is(instanceOf(MavenHostedRepository.class)));
  }

  @Test
  public void refreshHosted() {
    final MavenHostedRepository repository = repositories()
        .create(MavenHostedRepository.class, repositoryIdForTest())
        .save();

    final String name = repository.name();
    repository.withName("foo");
    repository.refresh();
    assertThat(repository.name(), is(name));
  }

  @Test
  public void createHosted() {
    final String id = repositoryIdForTest();
    repositories().create(MavenHostedRepository.class, id)
        .save();
  }

  @Test
  public void removeHosted() {
    final String id = repositoryIdForTest();
    repositories().create(MavenHostedRepository.class, id)
        .save()
        .remove();
  }

  @Test
  public void statusHosted() {
    final String id = repositoryIdForTest();
    final MavenHostedRepository repository = repositories().create(MavenHostedRepository.class, id);
    assertThat(repository.status().isInService(), is(false));
    repository.save();
    assertThat(repository.status().isInService(), is(true));
    assertThat(repository.putOutOfService().status().isInService(), is(false));
    assertThat(repository.putInService().status().isInService(), is(true));
  }

  @Test
  public void getProxy() {
    final Repository repository = repositories().get("central");
    assertThat(repository, is(instanceOf(MavenProxyRepository.class)));
  }

  @Test
  public void refreshProxy() {
    final Repository repository = repositories().get("central");
    final String name = repository.name();
    repository.withName("foo");
    repository.refresh();
    assertThat(repository.name(), is(name));
  }

  @Test
  public void createProxy() {
    final String id = repositoryIdForTest();
    repositories().create(MavenProxyRepository.class, id)
        .asProxyOf("http://localhost:8080")
        .save();
  }

  public void removeProxy() {
    final String id = repositoryIdForTest();
    repositories().create(MavenProxyRepository.class, id)
        .asProxyOf("http://localhost:8080")
        .save()
        .remove();
  }

  @Test
  public void statusProxy() {
    final String id = repositoryIdForTest();
    final MavenProxyRepository repository = repositories().create(MavenProxyRepository.class, id)
        .asProxyOf("http://localhost:8080");
    assertThat(repository.status().isInService(), is(false));
    repository.save();
    assertThat(repository.status().isInService(), is(true));
    assertThat(repository.putOutOfService().status().isInService(), is(false));
    assertThat(repository.putInService().status().isInService(), is(true));
  }

  @Test
  public void proxyMode() {
    final String id = repositoryIdForTest();
    final MavenProxyRepository repository = repositories().create(MavenProxyRepository.class, id)
        .asProxyOf("http://localhost:8080")
        .doNotAutoBlock();
    assertThat(repository.status().isBlocked(), is(false));
    assertThat(repository.status().isAutoBlocked(), is(false));
    repository.save();
    assertThat(repository.status().isBlocked(), is(false));
    assertThat(repository.status().isAutoBlocked(), is(false));
    repository.block();
    assertThat(repository.status().isBlocked(), is(true));
    assertThat(repository.status().isAutoBlocked(), is(false));
    repository.unblock();
    assertThat(repository.status().isBlocked(), is(false));
    assertThat(repository.status().isAutoBlocked(), is(false));
  }

  @Test
  public void getGroup() {
    final Repository repository = repositories().get("public");
    assertThat(repository, is(instanceOf(MavenGroupRepository.class)));
  }

  @Test
  public void refreshGroup() {
    final Repository repository = repositories().get("public");
    final String name = repository.name();
    repository.withName("foo");
    repository.refresh();
    assertThat(repository.name(), is(name));
  }

  @Test
  public void createGroup() {
    final String id = repositoryIdForTest();
    repositories().create(MavenGroupRepository.class, id)
        .ofRepositories("central", "releases", "snapshots")
        .save();
  }

  @Test
  public void groupMembersOperations() {
    final String id = repositoryIdForTest();
    final MavenGroupRepository repository = repositories().create(MavenGroupRepository.class, id)
        .ofRepositories("central", "releases", "snapshots");

    assertThat(repository.memberRepositories(), contains("central", "releases", "snapshots"));

    repository.ofRepositories("central", "releases");
    assertThat(repository.memberRepositories(), contains("central", "releases"));

    repository.addMember("snapshots");
    assertThat(repository.memberRepositories(), contains("central", "releases", "snapshots"));

    repository.removeMember("releases");
    assertThat(repository.memberRepositories(), contains("central", "snapshots"));
  }

  @Test
  public void removeGroup() {
    final String id = repositoryIdForTest();
    repositories().create(MavenGroupRepository.class, id)
        .ofRepositories("central", "releases", "snapshots")
        .save();
  }

  @Test
  public void statusGroup() {
    final String id = repositoryIdForTest();
    final MavenGroupRepository repository = repositories().create(MavenGroupRepository.class, id)
        .ofRepositories("central", "releases", "snapshots");
    assertThat(repository.status().isInService(), is(false));
    repository.save();
    assertThat(repository.status().isInService(), is(true));
    assertThat(repository.putOutOfService().status().isInService(), is(false));
    assertThat(repository.putInService().status().isInService(), is(true));
  }

  @Test
  public void getShadow() {
    final Repository repository = repositories().get("central-m1");
    assertThat(repository, is(instanceOf(MavenM1VirtualRepository.class)));
  }

  @Test
  public void refreshShadow() {
    final Repository repository = repositories().get("central-m1");
    final String name = repository.name();
    repository.withName("foo");
    repository.refresh();
    assertThat(repository.name(), is(name));
  }

  @Test
  public void createShadow() {
    final String id = repositoryIdForTest();
    repositories().create(MavenM1VirtualRepository.class, id)
        .ofRepository("apache-snapshots")
        .save();
  }

  @Test
  public void removeShadow() {
    final String id = repositoryIdForTest();
    repositories().create(MavenM1VirtualRepository.class, id)
        .ofRepository("apache-snapshots")
        .save()
        .remove();
  }

  @Test
  public void statusShadow() {
    final String id = repositoryIdForTest();
    final MavenM1VirtualRepository repository = repositories().create(MavenM1VirtualRepository.class, id)
        .ofRepository("apache-snapshots");
    assertThat(repository.status().isInService(), is(false));
    repository.save();
    assertThat(repository.status().isInService(), is(true));
    assertThat(repository.putOutOfService().status().isInService(), is(false));
    assertThat(repository.putInService().status().isInService(), is(true));
  }

  @Test
  public void getRepositories() {
    final Collection<Repository> repositories = repositories().get();
    assertThat(repositories.size(), is(greaterThanOrEqualTo(7)));
  }

  @Test
  public void getTargets() {
    final Collection<RepositoryTarget> targets = targets().get();
    assertThat(targets, is(not(empty())));
  }

  @Test
  public void getTarget() {
    final RepositoryTarget target = targets().get().iterator().next();
    final RepositoryTarget direct = targets().get(target.id());
    assertThat(direct.id(), is(target.id()));
    assertThat(direct.name(), is(target.name()));
    assertThat(direct.contentClass(), is(target.contentClass()));
  }

  @Test
  public void createTarget() {
    final String id = "created";
    createTarget(id, "test1", "test2");

    final RepositoryTarget target = targets().get(id);
    assertThat(target.id(), is(id));
    assertThat(target.name(), is(id + "name"));
    assertThat(target.contentClass(), is("maven2"));
    assertThat(target.patterns(), containsInAnyOrder("test1", "test2"));
  }

  private RepositoryTarget createTarget(final String id, final String... patterns) {
    return targets().create(id).withName(id + "name")
        .withContentClass("maven2").withPatterns(patterns)
        .save();
  }

  @Test
  public void updateTarget() {
    RepositoryTarget target = createTarget("updateTarget", "pattern1", "pattern2");

    target.withName("updatedTarget").addPattern("pattern3").save();
    target = targets().get("updateTarget");
    assertThat(target.patterns(), hasItem("pattern3"));
    assertThat(target.name(), is("updatedTarget"));
  }

  @Test(expected = NexusClientNotFoundException.class)
  public void deleteTarget() {
    RepositoryTarget target = createTarget("deleteTarget", "pattern1", "pattern2").remove();
    // targets.get(...) is expected to throw 404
    assertThat(targets().get(target.id()), is(nullValue()));
  }

  @Test
  public void refreshTarget() {
    RepositoryTarget needsRefresh = createTarget("deleteTarget", "pattern1", "pattern2");
    targets().get(needsRefresh.id()).withPatterns("differentPattern").save();
    assertThat(needsRefresh.refresh().patterns(), contains("differentPattern"));
  }

  @Test(expected = NexusClientNotFoundException.class)
  public void getNonExistentStatus() {
    routing().getStatus("no-such-repo-id");
  }

  @Test
  public void getReleaseStatus() {
    final Status status = routing().getStatus("releases");
    assertThat(status, is(not(nullValue())));
    assertThat(status.getPublishedStatus(), equalTo(Status.Outcome.SUCCEEDED));
    assertThat(status.getPublishedMessage(), is(notNullValue()));
    assertThat(status.getPublishedTimestamp(), greaterThan(0L));
    assertThat(status.getPublishedUrl(), is(notNullValue()));
  }

  @Test
  public void getSnapshotsStatus() {
    final Status status = routing().getStatus("snapshots");
    assertThat(status, is(not(nullValue())));
    assertThat(status.getPublishedStatus(), equalTo(Status.Outcome.SUCCEEDED));
    assertThat(status.getPublishedMessage(), is(notNullValue()));
    assertThat(status.getPublishedTimestamp(), greaterThan(0L));
    assertThat(status.getPublishedUrl(), is(notNullValue()));
  }

  @Test(expected = NexusClientBadRequestException.class)
  public void getCentralM1Status() {
    routing().getStatus("central-m1");
  }

  @Test(expected = NexusClientNotFoundException.class)
  public void getNonExistentConfig() {
    routing().getDiscoveryConfigurationFor("no-such-repo-id");
  }

  @Test
  public void getCentralDefaultConfig() {
    final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor("central");
    assertThat(config, is(notNullValue()));
    assertThat(config.isEnabled(), is(true));
    assertThat(config.getIntervalHours(), is(24));
  }

  @Test
  public void modifyDiscoveryConfig() {
    final boolean defaultEnabled;
    final int defaultIntervalHours;
    {
      final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor("central");
      defaultEnabled = config.isEnabled();
      defaultIntervalHours = config.getIntervalHours();
      config.setEnabled(false);
      config.setIntervalHours(12);
      routing().setDiscoveryConfigurationFor("central", config);
    }
    {
      final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor("central");
      assertThat(config.isEnabled(), is(false));
      assertThat(config.getIntervalHours(), is(12));
    }
    {
      // restore nx state as otherwise this disturbs tests like #getCentralDefaultConfig()
      // if executed AFTER this test
      final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor("central");
      config.setEnabled(defaultEnabled);
      config.setIntervalHours(defaultIntervalHours);
      routing().setDiscoveryConfigurationFor("central", config);
    }
  }

  @Test
  public void updatePrefixesOfReleases() {
    routing().updatePrefixFile("releases");
  }

  @Test
  public void updatePrefixesOfSnapshots() {
    routing().updatePrefixFile("snapshots");
  }

  @Test
  public void updatePrefixesOfCentral() {
    routing().updatePrefixFile("central");
  }

  @Test(expected = NexusClientBadRequestException.class)
  public void updatePrefixesOfCentralM1() {
    routing().updatePrefixFile("central-m1");
  }

  @Test(expected = NexusClientNotFoundException.class)
  public void updatePrefixesOfNonExistent() {
    routing().updatePrefixFile("no-such-repo-id");
  }

  @Test
  public void getRoles() {
    final Collection<Role> roles = roles().get();
    assertThat(roles, is(not(empty())));
  }

  @Test
  public void getRole() {
    final Role role = roles().get("ui-search");
    assertThat(role, is(notNullValue()));
    assertThat(role.id(), is("ui-search"));
  }

  @Test
  public void createRole() {
    final String roleId = testName.getMethodName();
    roles().create(roleId)
        .withName(roleId)
        .withPrivilege("19")
        .save();
    final Role role = roles().get(roleId);
    assertThat(role, is(notNullValue()));
    assertThat(role.id(), is(roleId));
    assertThat(role.name(), is(roleId));
  }

  @Test
  public void updateRole() {
    final String roleId = testName.getMethodName();
    roles().create(roleId)
        .withName(roleId)
        .withPrivilege("19")
        .save()
        .withName(roleId + "Bar")
        .save();
    final Role role = roles().get(roleId);
    assertThat(role, is(notNullValue()));
    assertThat(role.name(), is(roleId + "Bar"));
  }

  @Test
  public void deleteRole() {
    final String roleId = testName.getMethodName();
    final Role role = roles().create(roleId)
        .withName(roleId)
        .withPrivilege("19")
        .save();
    role.remove();
  }

  @Test
  public void refreshRole() {
    final String roleId = testName.getMethodName();
    Role role = roles().create(roleId)
        .withName(roleId)
        .withPrivilege("19")
        .save()
        .withName(roleId + "Bar")
        .refresh();
    assertThat(role.id(), is(roleId));
    role = roles().get(roleId);
    assertThat(role, is(notNullValue()));
    assertThat(role.name(), is(roleId));
  }

  @Test
  public void getUsers() {
    final Collection<User> users = users().get();
    assertThat(users, is(not(empty())));
  }

  @Test
  public void createUser() {
    final String username = testMethodName();
    users().create(username)
        .withEmail(username + "@sonatype.org")
        .withFirstName("bar")
        .withLastName("foo")
        .withPassword("super secret")
        .withRole("anonymous")
        .save();

    final User user = users().get(username);
    assertThat(user, is(notNullValue()));
    assertThat(user.firstName(), is("bar"));
  }

  @Test
  public void updateUser() {
    final String username = testMethodName();
    users().create(username)
        .withEmail(username + "@sonatype.org")
        .withFirstName("bar")
        .withLastName("foo")
        .withPassword("super secret")
        .withRole("anonymous")
        .save();

    final User user = users().get(username)
        .withFirstName("Bar the second")
        .save();

    assertThat(user, is(notNullValue()));
    assertThat(user.firstName(), is("Bar the second"));
  }

  @Test
  public void deleteUser() {
    final String username = testMethodName();
    final User user = users().create(username)
        .withEmail(username + "@sonatype.org")
        .withFirstName("bar")
        .withLastName("foo")
        .withPassword("super secret")
        .withRole("anonymous")
        .save();
    user.remove();
  }

  @Test
  public void getUser() {
    final User user = users().get("admin");
    assertThat(user, is(notNullValue()));
    assertThat(user.id(), is("admin"));
  }

  @Test
  public void getPrivileges() {
    assertThat(privileges().get(), is(not(empty())));
  }

  @Test
  public void getPrivilege() {
    // admin privilege
    final Privilege privilege = privileges().get("1000");
    assertThat(privilege, is(not(nullValue())));
    assertThat(privilege.name(), containsString("Administrator"));
  }

  @Test
  public void createPrivilege() {
    final String targetId = createRepoTarget("createPrivileges").id();
    final Privilege saved = privileges().create()
        .withName("foo")
        .withDescription("bar")
        .withMethods("read")
        .withRepositoryGroupId("public")
        .withTargetId(targetId)
        .create().iterator().next();

    final Privilege privilege = privileges().get(saved.id());
    assertThat(privilege, is(notNullValue()));
    assertThat(privilege.description(), is("bar"));

    // name is mangled on creation - "$name - ($method)"
    assertThat(privilege.name(), is(saved.name()));

    assertThat(privilege.methods(), contains("read"));
    assertThat(privilege.repositoryGroupId(), is("public"));
    assertThat(privilege.targetId(), is(targetId));
  }

  @Test(expected = IllegalStateException.class)
  public void refuseCreateAlreadyExistingPrivilege() {
    final String targetId = createRepoTarget("refuseCreatePrivileges").id();
    final Privilege saved = privileges().create()
        .withName("foo")
        .withDescription("bar")
        .withMethods("read")
        .withRepositoryGroupId("public")
        .withTargetId(targetId)
        .create().iterator().next();

    saved.create();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedUpdatePrivilege() {
    final String targetId = createRepoTarget("unsupportedUpdatePrivileges").id();
    final Privilege saved = privileges().create()
        .withName("foo")
        .withDescription("bar")
        .withMethods("read")
        .withRepositoryGroupId("public")
        .withTargetId(targetId)
        .create().iterator().next();

    saved.save();
  }

  @Test(expected = NexusClientNotFoundException.class)
  public void deletePrivilege() {
    final String targetId = createRepoTarget("deletePrivileges").id();
    final Privilege saved = privileges().create()
        .withName("foo")
        .withDescription("bar")
        .withMethods("read")
        .withRepositoryGroupId("public")
        .withTargetId(targetId)
        .create().iterator().next();

    saved.remove();

    privileges().get(saved.id());
  }

  /**
   * Related to NEXUS-5037 ensure that html escaped passwords(specifically quote character in this case) can be used
   * as credentials.
   */
  @Test
  public void testUserWithSingleQuotePassword() {
    Users users = client().getSubsystem(Users.class);
    String password = "\"";
    users.create("test").withPassword(password).withRole("nx-admin").withEmail("no@where.com").save();
    NexusClient client = createNexusClient(nexus(), "test", password);
    //will fail if can't authenticate
    Assert.assertThat(client.getNexusStatus(), Is.is(notNullValue()));
  }

  /**
   * Verify that a client impl is automatically created using siesta client.
   */
  @Test
  public void getUsersUsingSiestaClient() {
    final UserListResourceResponse userListResourceResponse = client().getSubsystem(UserClient.class).get();
    assertThat(userListResourceResponse, is(notNullValue()));
    assertThat(userListResourceResponse.getData(), is(not(empty())));
  }


  @Test
  public void checkValidationErrorsFilter() {
    final UserResource user = new UserResource();
    final UserResourceRequest userRequest = new UserResourceRequest();
    userRequest.setData(user);

    thrown.expect(NexusClientErrorResponseException.class);
    thrown.expectMessage("Users status is not valid");
    client().getSubsystem(UserClient.class).create(userRequest);
  }

  @Path("/service/local/users")
  public static interface UserClient
      extends Restlet1xClient
  {
    @GET
    UserListResourceResponse get();

    @POST
    UserResourceResponse create(UserResourceRequest request);
  }

}
