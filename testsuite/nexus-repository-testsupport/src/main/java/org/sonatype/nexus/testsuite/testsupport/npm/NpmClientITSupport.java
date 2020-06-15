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
package org.sonatype.nexus.testsuite.testsupport.npm;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;
import com.sonatype.nexus.docker.testsupport.npm.NpmCommandLineITSupport;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.security.NpmToken;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.testsuite.testsupport.FormatClientITSupport;
import org.sonatype.nexus.testsuite.testsupport.utility.SearchTestHelper;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * Support class for npm Format Client ITs.
 */
public abstract class NpmClientITSupport
    extends FormatClientITSupport
{
  protected static final String EMPTY = "";

  protected static final String TEST_APP_DIR = "/root/TestApp";

  protected static final String ROOT_DIR = "/root";

  protected static final String TEST_APP_NAME = "testproject";

  protected static final String V1 = "0.0.1";

  protected static final String V2 = "0.0.2";

  protected static final String V3 = "0.0.3";

  protected static final String TEST_APP_FULL_NAME =  "testproject@" + V1;

  protected static final String TEST_APP_FULL_NAME_V2 =  "testproject@" + V2;

  protected static final String TEST_APP_SCOPED_NAME = "@sonatype/testproject";

  protected static final String TEST_APP_SCOPE_FULL_NAME = "@sonatype/testproject@" + V1;

  protected NpmCommandLineITSupport npmCli;

  private NpmClientFactory npmClientFactory = new NpmClientFactory();

  @Inject
  protected SearchTestHelper searchTestHelper;

  @Inject
  AnonymousManager anonymousManager;

  protected abstract DockerContainerConfig createTestConfig() throws Exception;

  @Before
  public void onInitializeClientIT() throws Exception {
    addTestDataDirectory("target/it-resources/npm");
    enableRealm();
  }

  @After
  public void onTearDownClientIT() throws Exception {
    npmCli.exit();
  }

  private void enableRealm() {
    RealmConfiguration config = realmManager.getConfiguration();

    if (!config.getRealmNames().contains(NpmToken.NAME)) {
      config.getRealmNames().add(NpmToken.NAME);
      realmManager.setConfiguration(config);
    }
  }

  protected void disableAnonymous() {
    final AnonymousConfiguration anonymousConfiguration = anonymousManager.getConfiguration();
    anonymousConfiguration.setEnabled(false);
    anonymousManager.setConfiguration(anonymousConfiguration);
  }

  protected void maybeEnableAnonymous() {
   AnonymousConfiguration anonymousConfiguration = anonymousManager.getConfiguration();
   anonymousConfiguration.setEnabled(true);
   anonymousManager.setConfiguration(anonymousConfiguration);
  }

  protected Repository createNpmHostedRepository(final String repositoryName) {
    return repos.createNpmHosted(repositoryName + "-hosted");
  }

  protected Repository createNpmProxyRepository(final String repositoryName, final String targetUrl) {
    return repos.createNpmProxy(repositoryName + "-proxy", targetUrl);
  }

  protected Repository createNpmGroupRepository(final String repositoryName, final String... members) {
    return repos.createNpmGroup(repositoryName + "-group", members);
  }

  protected NpmClient createNpmClient(final String repositoryName) {
    return npmClientFactory
        .createClient(resolveUrl(nexusUrl, "/repository/" + repositoryName + "/"), "admin", "admin123");
  }

  protected void setRegistryUrl(final String registryUrl) {
    npmCli.execNpmConfig("registry", registryUrl);
  }

  protected void initializeNpmrc(final String registryUrl) throws Exception {
    npmCli = new NpmCommandLineITSupport(createTestConfig());
    npmCli.execNpmConfig("registry ", registryUrl);
    npmCli.execNpmConfig("email", "my@example.com");
    npmCli.execNpmConfig("always-auth", "true");
    npmCli.execNpmConfig("_auth",  "YWRtaW46YWRtaW4xMjM=");
  }

  protected void configureAndPublish(final Repository repository,
                                     final String directory,
                                     final String name,
                                     final String version)
  {
    configureAndPublish(directory, name, version, EMPTY, constructPublishConfig(getRepoUrl(repository.getName())));
  }

  protected void configureAndPublish(final String directory,
                                     final String name,
                                     final String version,
                                     final String maintainers,
                                     final String publishConfig)
  {
    configurePackageJson(directory, escape(name), version, maintainers, publishConfig);
    List<String> results = npmCli.publish(directory);

    assertThat(results.size(), is(greaterThan(0)));
    assertThat(results.stream().anyMatch(s -> s.contains("+ " + name + "@" + version)), is(true));
  }

  protected void configurePackageJson(
      final String directory,
      final String npmComponentName,
      final String npmComponentVersion)
  {
    npmCli.updatePackageJson(directory, npmComponentName, npmComponentVersion);
    npmCli.updatePackageLockJson(directory, npmComponentName, npmComponentVersion);
  }

  protected void configurePackageJson(final String directory,
                                      final String name,
                                      final String version,
                                      final String maintainers,
                                      final String publishConfig)
  {
    npmCli.updatePackageJson(directory, name, version, maintainers, publishConfig);
  }

  protected void npmLogin(final String directory,
                          final String repostioryUrl,
                          final String username,
                          final String password)
  {
    npmCli.updateLoginScript(directory, repostioryUrl, username, password);
    npmCli.login(directory);
  }

  protected String npmWhoami() {
    List<String> results = npmCli.execNpm("whoami").orElse(null);

    if (results != null && results.size() == 1) {
      return results.get(0);
    }
    return null;
  }

  protected String npmPing() {
    List<String> results = npmCli.execNpm("ping").orElse(null);

    if (results != null && results.size() == 1) {
      return results.get(0);
    }
    return null;
  }

  protected void npmLogout(final String repositoryUrl) {
    npmCli.execNpm("logout --registry=" + repositoryUrl);
  }

  protected void verifyComponentExists(final String repositoryName,
                                       final String name,
                                       final String version) throws Exception
  {
    List<Map<String, Object>> items = searchForComponent(repositoryName, name, version);
    assertThat(items.size(), is(1));
  }

  protected void verifyComponentNotExists(
      final String repositoryName,
      final String name,
      final String version) throws Exception
  {
    List<Map<String, Object>> items = searchForComponent(repositoryName, name, version);
    assertThat(items.size(), is(0));
  }

  protected void verifyInstalled(final List<String> results, final String packageName) {
    assertThat(results.size(), is(greaterThan(0)));
    assertThat(results.stream().anyMatch(s -> s.contains("+ " + packageName)), is(true));
    assertThat(results.stream().anyMatch(s -> s.contains("added 1 package")), is(true));
  }

  protected void verifyAudit(final List<String> results, final String verifyText) {
    assertThat(results.size(), is(greaterThan(0)));
    assertThat(results.stream().anyMatch(s -> s.contains(verifyText)), is(true));
  }

  protected void verifyUpdated(final List<String> results, final String packageName) {
    assertThat(results.size(), is(greaterThan(0)));
    assertThat(results.stream().anyMatch(s -> s.contains("+ " + packageName)), is(true));
    assertThat(results.stream().anyMatch(s -> s.contains("updated 1 package")), is(true));
  }

  protected void verifyNotUpdated(final List<String> results, final String packageName) {
    assertThat(results.size(), is(greaterThan(0)));
    assertThat(results.stream().anyMatch(s -> s.contains("No matching version found for " + packageName)), is(true));
  }

  protected void verifyNotFound(final List<String> results) {
    assertThat(results.stream().anyMatch(s -> s.contains("npm ERR! code E404")), is(true));
  }

  protected void verifyDeprecation(final String repository,
                                   final String name,
                                   final String version,
                                   final String expected)
  {
    NpmClient npmClient = getNpmClient(repository);

    String deprecatedValue = npmClient.fetch(name).child("versions").child(version).get("deprecated", String.class);

    assertThat(deprecatedValue, is(expected));
  }

  protected void verifyMerged(final String repository,
                              final String name,
                              final String...versions)
  {

    NpmClient npmClient = getNpmClient(repository);

    NestedAttributesMap fetchedPackageRoot = npmClient.fetch(name);
    NestedAttributesMap versionsMap = fetchedPackageRoot.child("versions");

    assertThat(versionsMap.backing().keySet(), equalTo(ImmutableSet.copyOf(versions)));
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> searchForComponent(final String repository,
                                                       final String name,
                                                       final String version) throws Exception
  {
    searchTestHelper.waitForSearch();

    Response response = restClient().target(nexusUrl("/service/rest/v1/search"))
        .queryParam("repository", repository)
        .queryParam("name", name)
        .queryParam("version", version)
        .request()
        .buildGet()
        .invoke();

    Map<String, Object> map = response.readEntity(Map.class);
    return (List<Map<String, Object>>) map.get("items");
  }


  private String nexusUrl(final String... segments) {
    try {
      return nexusUrl.toURI().resolve(Joiner.on('/').join(Arrays.asList(segments))).toString();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private NpmClient getNpmClient(final String repository) {
    return npmClientFactory.createClient(resolveUrl(nexusUrl, "/repository/" + repository + "/"),
        "admin", "admin123");
  }

  /**
   * Simple utility to construct the publishConfig string for inclusion in the package.json
   */
  protected static String constructPublishConfig(final String repositoryName) {
    return escape("\"registry\" : \"" + repositoryName + "\"");
  }

  /**
   * Simple utility to add escapes for slashes to ensure properly formatted strings in the package.json
   */
  protected static String escape(final String input) {
    return input.replace("/", "\\/");
  }
}
