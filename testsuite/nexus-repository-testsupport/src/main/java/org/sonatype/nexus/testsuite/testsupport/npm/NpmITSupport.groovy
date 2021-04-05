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
package org.sonatype.nexus.testsuite.testsupport.npm

import javax.inject.Inject

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.common.log.LogManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.npm.security.NpmToken
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport

import com.google.common.collect.ImmutableSet
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.junit.experimental.categories.Category
import org.ops4j.pax.exam.Configuration
import org.ops4j.pax.exam.Option

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.ops4j.pax.exam.CoreOptions.when
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut
import static org.sonatype.nexus.common.app.FeatureFlags.EARLY_ACCESS_DATASTORE_DEVELOPER
import static org.sonatype.nexus.repository.http.HttpStatus.CREATED
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND

/**
 * Support for npm ITs.
 */
@Category(NpmTestGroup)
class NpmITSupport
    extends RepositoryITSupport
{
  @Inject
  protected LogManager logManager

  NpmITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/npm"))
  }

  /**
   * Configure Nexus.
   */
  @Configuration
  static Option[] configureNexus() {
    return options(
            configureNexusBase(),
            when(getValidTestDatabase().isUseContentStore()).useOptions(editConfigurationFilePut(NEXUS_PROPERTIES_FILE, EARLY_ACCESS_DATASTORE_DEVELOPER, "true"))
    );
  }

  void enableNpmRealm() {
    def config = realmManager.configuration
    if (!config.realmNames.contains(NpmToken.NAME)) {
      config.realmNames.add(NpmToken.NAME)
      realmManager.configuration = config
    }
  }

  Repository createNpmHostedRepository(String name, String writePolicy = 'ALLOW_ONCE') {
    return repos.createNpmHosted(name, writePolicy)
  }

  Repository createNpmHostedRepositoryWithBlobstore(String name, String blobStoreName) {
    return repos.createNpmHosted(name, 'ALLOW_ONCE', blobStoreName)
  }

  Repository createNpmGroupRepository(String repositoryName, String... members) {
    return repos.createNpmGroup(repositoryName, members)
  }

  Repository createNpmGroupRepository(String repositoryName, Repository writeable, String... members) {
    return repos.createNpmGroup(repositoryName, writeable, members)
  }

  Repository createNpmProxyRepository(final String name, final String remoteUrl,
                                      final Map<String, Object> authentication = [:])
  {
    return repos.createNpmProxy(name, remoteUrl, authentication)
  }

  protected def checkLogin(NpmClient client, String username, String password, int expectedStatus) {
    return client.login(username, password, "foo@bar.baz").withCloseable { response ->
      log.debug("deserialized response: {}", response)
      return response.statusLine.statusCode == expectedStatus
    }
  }

  protected static def login(NpmClient client, String username, String password) {
    return client.login(username, password, "foo@bar.baz").withCloseable { response ->
      if (response.statusLine.statusCode == CREATED) {
        return new JsonSlurper().parse(response.entity.content).token
      }
      else {
        return null
      }
    }
  }

  protected NestedAttributesMap fetchMetadataAndVerifyVersions(final NpmClient client,
                                                               final String... versions)
  {
    return fetchMetadataAndVerifyVersions('foo', client, versions)
  }

  protected NestedAttributesMap fetchMetadataAndVerifyVersions(final String packageName,
                                                               final NpmClient client,
                                                               final String... versions)
  {
    NestedAttributesMap fetchedPackageRoot = client.fetch(packageName)

    assert !fetchedPackageRoot.empty
    assert fetchedPackageRoot['name'] == packageName
    assert fetchedPackageRoot['versions'].size() == versions.length

    NestedAttributesMap versionsMap = fetchedPackageRoot.child('versions')
    assertThat(versionsMap.backing().keySet(), equalTo(ImmutableSet.copyOf(versions)))
    fetchedPackageRoot
  }

  protected verifyNotFound(final NpmClient client, final String packageName) {
    HttpResponse response = client.getResponseForPackage(packageName)
    try {
      assert response.getStatusLine().getStatusCode() == NOT_FOUND
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }
}
