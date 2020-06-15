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
package org.sonatype.nexus.testsuite.testsupport.pypi

import javax.inject.Inject

import org.sonatype.nexus.common.log.LogManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.ComponentMaintenance
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.storage.WritePolicy
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport

import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.junit.experimental.categories.Category

import static java.lang.Thread.sleep
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME
import static org.sonatype.nexus.repository.storage.WritePolicy.ALLOW

/**
 * Support for PyPI ITs.
 */
@Category(PyPiTestGroup)
class PyPiITSupport
    extends RepositoryITSupport
{
  public static final Map<String, List<String>> METADATA = [
      comment         : ['comment'],
      maintainer      : ['maintainer'],
      metadata_version: ['2.0'],
      filetype        : [''],
      keywords        : ['keyword1 keyword2 keyword3'],
      author          : ['author'],
      home_page       : ['www.example.com'],
      platform        : ['UNKNOWN'],
      version         : ['1.2.0'],
      protcol_version : ['1'], // yes, it's actually "protcol" in the twine upload traffic
      description     : ['description'],
      md5_digest      : [''],
      ':action'       : ['file_upload'],
      requires_dist   : [
          'peppercorn',
          'check-manifest; extra == \'dev\'',
          'coverage; extra == \'test\''
      ],
      requires_python : [''],
      classifiers     : [
          'Development Status :: 3 - Alpha',
          'Intended Audience :: Developers',
          'Topic :: Software Development :: Build Tools',
          'License :: OSI Approved :: MIT License',
          'Programming Language :: Python :: 2',
          'Programming Language :: Python :: 2.6',
          'Programming Language :: Python :: 2.7',
          'Programming Language :: Python :: 3',
          'Programming Language :: Python :: 3.3',
          'Programming Language :: Python :: 3.4',
          'Programming Language :: Python :: 3.5'
      ],
      name            : ['sample'],
      license         : ['MIT'],
      pyversion       : [''],
      summary         : ['A sample project for testing'],
      author_email    : ['none@example.com']
  ].asImmutable()

  @Inject
  protected LogManager logManager

  public PyPiITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/pypi"))
  }

  Repository createPyPiProxyRepository(final String name, final String remoteUrl) {
    return repos.createPyPiProxy(name, remoteUrl)
  }

  Repository createPyPiHostedRepository(final String name, final WritePolicy writePolicy = ALLOW) {
    return repos.createPyPiHosted(name, writePolicy.toString())
  }

  Repository createPyPiGroupRepository(final String name, final String... members) {
    return repos.createPyPiGroup(name, members)
  }

  PyPiClient createPyPiClient(final Repository repository, final Credentials credentials = null) {
    def builder = clientBuilder()

    if (credentials != null) {
      String hostname = nexusUrl.getHost()
      AuthScope scope = new AuthScope(hostname, -1)
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
      credentialsProvider.
          setCredentials(scope, credentials)
      builder.setDefaultCredentialsProvider(credentialsProvider)
    }

    return new PyPiClient(
        builder.build(),
        clientContext(),
        resolveUrl(nexusUrl, "/repository/$repository.name/").toURI(),
        testData
    )
  }

  void waitForIfModified() {
    sleep(1000L)
  }

  void deleteAsset(final Repository repository, final String assetName) {
    componentAssetTestHelper.removeAsset(repository, assetName);
  }

  void deleteComponent(final Repository repository, final String componentName) {
    def maintenanceFacet = repository.facet(ComponentMaintenance.class)
    StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()

    def componentId = null
    try {
      tx.begin()
      componentId = tx.findComponentWithProperty(P_NAME, componentName,
          tx.findBucket(repository)).getEntityMetadata().getId()
    }
    finally {
      tx.close()
    }

    maintenanceFacet.deleteComponent(componentId)
  }
}
