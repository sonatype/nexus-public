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
package org.sonatype.repository.conan.upgrade

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.orient.OClassNameBuilder
import org.sonatype.nexus.orient.OIndexNameBuilder
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.repository.conan.internal.AssetKind
import org.sonatype.repository.conan.internal.ConanFormat
import org.sonatype.repository.conan.internal.orient.hosted.ConanHostedRecipe
import org.sonatype.repository.conan.internal.orient.proxy.v1.ConanProxyRecipe

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.index.OIndex
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import org.apache.commons.lang3.tuple.Pair
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue

class ConanUpgrade_1_1_Test
    extends TestSupport
{
  static final String REPOSITORY_CLASS = new OClassNameBuilder()
      .type("repository")
      .build()

  static final String I_REPOSITORY_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(REPOSITORY_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String BUCKET_CLASS = new OClassNameBuilder()
      .type("bucket")
      .build()

  static final String I_BUCKET_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(BUCKET_CLASS)
      .property(P_REPOSITORY_NAME)
      .build()

  static final String ASSET_CLASS = new OClassNameBuilder()
      .type("asset")
      .build()

  static final String I_ASSET_NAME = new OIndexNameBuilder()
      .type(ASSET_CLASS)
      .property(P_NAME)
      .build()

  static final List<Pair<AssetKind, String>> PROXY_BASE_ON_MASTER_CHANGES_ACTUAL = Collections.unmodifiableList(Arrays.asList(

      // master changes does not contain CONAN_PACKAGE_SNAPSHOT
      //Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "proxy-master-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.CONAN_PACKAGE, "conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      Pair.of(AssetKind.DOWNLOAD_URL, "conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),

      Pair.of(AssetKind.CONAN_EXPORT, "conan/proxy-master-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "conan/proxy-master-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conan/proxy-master-lib/1.2.11/stable/conanmanifest.txt"),
      Pair.of(AssetKind.DOWNLOAD_URL, "conan/proxy-master-lib/1.2.11/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> PROXY_BASE_ON_V1_CHANGES_ACTUAL = Collections.unmodifiableList(Arrays.asList(
      Pair.of(AssetKind.CONAN_PACKAGE, "v1/conans/conan/proxy-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "v1/conans/conan/proxy-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "v1/conans/conan/proxy-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      Pair.of(AssetKind.CONAN_EXPORT, "v1/conans/conan/proxy-v1-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "v1/conans/conan/proxy-v1-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "v1/conans/conan/proxy-v1-lib/1.2.11/stable/conanmanifest.txt"),

      Pair.of(AssetKind.DOWNLOAD_URL, "v1/conans/proxy-v1-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),
      Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "v1/conans/proxy-v1-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.DOWNLOAD_URL, "v1/conans/proxy-v1-lib/1.2.11/conan/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> HOSTED_BASE_ON_MASTER_CHANGES_ACTUAL = Collections.unmodifiableList(Arrays.asList(

      //Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "/v1/conans/hosted-master-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.CONAN_PACKAGE, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),

      Pair.of(AssetKind.CONAN_EXPORT, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> HOSTED_BASE_ON_V1_CHANGES_ACTUAL = Collections.unmodifiableList(Arrays.asList(

      //Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "/v1/conans/v1/conans/hosted-v1-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.CONAN_PACKAGE, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),

      Pair.of(AssetKind.CONAN_EXPORT, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> HOSTED_BASE_ON_V1_CHANGES_ACTUAL_THREE_PREFIXES = Collections.unmodifiableList(Arrays.asList(

      Pair.of(AssetKind.CONAN_PACKAGE, "/v1/conans/v1/conans/v1/conans/conan/hosted-v2-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "/v1/conans/v1/conans/v1/conans/conan/hosted-v2-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "/v1/conans/v1/conans/v1/conans/conan/hosted-v2-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),

      Pair.of(AssetKind.CONAN_EXPORT, "/v1/conans/v1/conans/v1/conans/conan/hosted-v2-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "/v1/conans/v1/conans/v1/conans/conan/hosted-v2-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "/v1/conans/v1/conans/v1/conans/conan/hosted-v2-lib/1.2.11/stable/conanmanifest.txt"),
  ))

  static final List<Pair<AssetKind, String>> PROXY_BASE_ON_MASTER_CHANGES_EXPECTED = Collections.unmodifiableList(Arrays.asList(

      // master changes does not contain CONAN_PACKAGE_SNAPSHOT
      //Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "proxy-master-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.CONAN_PACKAGE, "conans/conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "conans/conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/proxy-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),

      Pair.of(AssetKind.CONAN_EXPORT, "conans/conan/proxy-master-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "conans/conan/proxy-master-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/proxy-master-lib/1.2.11/stable/conanmanifest.txt"),

      Pair.of(AssetKind.DOWNLOAD_URL, "conans/proxy-master-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),
      Pair.of(AssetKind.DOWNLOAD_URL, "conans/proxy-master-lib/1.2.11/conan/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> PROXY_BASE_ON_V1_CHANGES_EXPECTED = Collections.unmodifiableList(Arrays.asList(
      Pair.of(AssetKind.CONAN_PACKAGE, "conans/conan/proxy-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "conans/conan/proxy-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/proxy-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      Pair.of(AssetKind.CONAN_EXPORT, "conans/conan/proxy-v1-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "conans/conan/proxy-v1-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/proxy-v1-lib/1.2.11/stable/conanmanifest.txt"),

      Pair.of(AssetKind.DOWNLOAD_URL, "conans/proxy-v1-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),
      Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "conans/proxy-v1-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.DOWNLOAD_URL, "conans/proxy-v1-lib/1.2.11/conan/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> HOSTED_BASE_ON_MASTER_CHANGES_EXPECTED = Collections.unmodifiableList(Arrays.asList(

      //Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "/v1/conans/hosted-master-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.CONAN_PACKAGE, "conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),

      Pair.of(AssetKind.CONAN_EXPORT, "conans/conan/hosted-master-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "conans/conan/hosted-master-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/hosted-master-lib/1.2.11/stable/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/conan/hosted-master-lib/1.2.11/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> HOSTED_BASE_ON_V1_CHANGES_EXPECTED = Collections.unmodifiableList(Arrays.asList(

      //Pair.of(AssetKind.CONAN_PACKAGE_SNAPSHOT, "/v1/conans/v1/conans/hosted-v1-lib/1.2.11/conan/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55"),
      Pair.of(AssetKind.CONAN_PACKAGE, "conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/download_urls"),

      Pair.of(AssetKind.CONAN_EXPORT, "conans/conan/hosted-v1-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "conans/conan/hosted-v1-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/hosted-v1-lib/1.2.11/stable/conanmanifest.txt"),
      //Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/download_urls")
  ))

  static final List<Pair<AssetKind, String>> HOSTED_DOWNLOAD_URLS_ASSETS = Collections.unmodifiableList(Arrays.asList(
      Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/v1/conans/conan/hosted-v1-lib/1.2.11/stable/download_urls"),
      Pair.of(AssetKind.DOWNLOAD_URL, "/v1/conans/conan/hosted-v1-lib/1.2.11/stable/download_urls"),
      Pair.of(AssetKind.DOWNLOAD_URL, "conans/conan/hosted-v1-lib/1.2.11/stable/download_urls")
  ))


  static final List<Pair<AssetKind, String>> HOSTED_BASE_ON_V1_CHANGES_EXPECTED_THREE_PREFIXES = Collections.unmodifiableList(Arrays.asList(

      Pair.of(AssetKind.CONAN_PACKAGE, "conans/conan/hosted-v2-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conan_package.tgz"),
      Pair.of(AssetKind.CONAN_INFO, "conans/conan/hosted-v2-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conaninfo.txt"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/hosted-v2-lib/1.2.11/stable/packages/534dcc368c999e07e81f146b3466b8f656ef1f55/conanmanifest.txt"),

      Pair.of(AssetKind.CONAN_EXPORT, "conans/conan/hosted-v2-lib/1.2.11/stable/conan_export.tgz"),
      Pair.of(AssetKind.CONAN_FILE, "conans/conan/hosted-v2-lib/1.2.11/stable/conanfile.py"),
      Pair.of(AssetKind.CONAN_MANIFEST, "conans/conan/hosted-v2-lib/1.2.11/stable/conanmanifest.txt"),
  ))

  private static final String P_NAME = "name"

  private static final String P_FORMAT = "format"

  private static final String P_ATTRIBUTES = "attributes"

  private static final String P_BUCKET = "bucket"

  private static final String P_REPOSITORY_NAME = "repository_name"

  private static final String P_RECIPE_NAME = "recipe_name"

  private static String CONAN_PROXY_RECIPE_NAME = ConanProxyRecipe.NAME

  private static final String CONAN_PROXY_REPOSITORY_NAME = "conan-proxy"

  private static String CONAN_HOSTED_RECIPE_NAME = ConanHostedRecipe.NAME

  private static final String CONAN_HOSTED_REPOSITORY_NAME = "conan-hosted"

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config")

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component")

  ConanUpgrade_1_1 underTest

  @Before
  void setUp() {
    configDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()

      // repository
      def repositoryType = schema.createClass(REPOSITORY_CLASS)
      repositoryType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(true)
          .setNotNull(true)
      repositoryType.createProperty(P_RECIPE_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      repositoryType.createIndex(I_REPOSITORY_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      repository(CONAN_PROXY_REPOSITORY_NAME, CONAN_PROXY_RECIPE_NAME)
      repository(CONAN_HOSTED_REPOSITORY_NAME, CONAN_HOSTED_RECIPE_NAME)
    }

    componentDatabase.instance.connect().withCloseable { db ->
      OSchema schema = db.getMetadata().getSchema()

      // bucket
      def bucketType = schema.createClass(BUCKET_CLASS)
      bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      bucketType.createIndex(I_BUCKET_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)

      bucket(CONAN_PROXY_REPOSITORY_NAME)
      bucket(CONAN_HOSTED_REPOSITORY_NAME)

      // asset
      def assetType = schema.createClass(ASSET_CLASS)

      assetType.createProperty(P_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      assetType.createProperty(P_FORMAT, OType.STRING)
          .setMandatory(true)
          .setNotNull(true)
      assetType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
      assetType.createIndex(I_ASSET_NAME, INDEX_TYPE.UNIQUE, P_NAME)

      // create some test data
      OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_BUCKET_REPOSITORY_NAME)
      def proxies = PROXY_BASE_ON_V1_CHANGES_ACTUAL + PROXY_BASE_ON_MASTER_CHANGES_ACTUAL
      proxies.each { asset(bucketIdx, CONAN_PROXY_REPOSITORY_NAME, it.value, attributes(it.key)) }

      def hostedList = HOSTED_BASE_ON_MASTER_CHANGES_ACTUAL + HOSTED_BASE_ON_V1_CHANGES_ACTUAL +
          HOSTED_DOWNLOAD_URLS_ASSETS + HOSTED_BASE_ON_V1_CHANGES_ACTUAL_THREE_PREFIXES
      hostedList.each { asset(bucketIdx, CONAN_HOSTED_REPOSITORY_NAME, it.value, attributes(it.key)) }
    }

    underTest = new ConanUpgrade_1_1(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider())
  }

  private static Map<String, Object> attributes(final AssetKind assetKind) {
    Map<String, String> conan = new HashMap<>()
    conan.asset_kind = assetKind.name()
    conan.some_conan_key = "some_conan_value"

    Map<String, String> attributes = new HashMap<>()
    attributes.conan = conan
    attributes.another_attributes = "another_attributes_value"
    return attributes
  }


  private static repository(final String repositoryName, final String recipe) {
    ODocument repository = new ODocument(REPOSITORY_CLASS)
    repository.field(P_REPOSITORY_NAME, repositoryName)
    repository.field(P_RECIPE_NAME, recipe)
    repository.save()
  }

  private static bucket(final String repositoryName) {
    ODocument bucket = new ODocument(BUCKET_CLASS)
    bucket.field(P_REPOSITORY_NAME, repositoryName)
    bucket.save()
  }

  private static asset(final OIndex bucketIdx, final String repositoryName, final String name,
                       Map<String, Object> attributes)
  {
    OIdentifiable idf = bucketIdx.get(repositoryName) as OIdentifiable
    ODocument asset = new ODocument(ASSET_CLASS)
    asset.field(P_BUCKET, idf)
    asset.field(P_NAME, name)
    asset.field(P_FORMAT, ConanFormat.NAME)
    asset.field("attributes", attributes)
    asset.save()
  }

  @Test
  void 'asset name'() {
    underTest.apply()
    componentDatabase.instance.connect().withCloseable { db ->
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME)
      def list = HOSTED_BASE_ON_MASTER_CHANGES_EXPECTED + HOSTED_BASE_ON_V1_CHANGES_EXPECTED +
          PROXY_BASE_ON_V1_CHANGES_EXPECTED + PROXY_BASE_ON_MASTER_CHANGES_EXPECTED +
          HOSTED_BASE_ON_V1_CHANGES_EXPECTED_THREE_PREFIXES
      list.each {
        OIdentifiable idf = idx.get(it.value) as OIdentifiable
        assertThat(idf, notNullValue())
        ODocument asset = idf.record
        assertThat(asset, notNullValue())
      }
    }
  }

  @Test
  void 'delete download_urls hosted'() {
    underTest.apply()
    componentDatabase.instance.connect().withCloseable {
      OIndex<?> idx = it.getMetadata().getIndexManager().getIndex(I_ASSET_NAME)
      HOSTED_DOWNLOAD_URLS_ASSETS.each {
        OIdentifiable idf = idx.get(it.value) as OIdentifiable
        assertThat(idf, nullValue())
      }
    }
  }

  @Test
  void 'conan manifest'() {
    underTest.apply()
    componentDatabase.instance.connect().withCloseable { db ->
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME)

      def list = HOSTED_BASE_ON_MASTER_CHANGES_EXPECTED + HOSTED_BASE_ON_V1_CHANGES_EXPECTED + PROXY_BASE_ON_V1_CHANGES_EXPECTED + PROXY_BASE_ON_MASTER_CHANGES_EXPECTED

      list.each {
        OIdentifiable idf = idx.get(it.value) as OIdentifiable
        ODocument asset = idf.record

        Map<String, Object> attributes = asset.field("attributes")

        Map<String, Object> conan = attributes.conan as Map<String, Object>
        String assetKind = conan.asset_kind
        assertThat(assetKind, Matchers.is(it.key.name()))

        if (it.key == AssetKind.CONAN_MANIFEST) {
          assertThat(conan.size(), Matchers.is(1))
        }
        else {
          assertThat(conan.size(), Matchers.is(2))
          String some_conan_key = conan.some_conan_key
          assertThat(some_conan_key, Matchers.is("some_conan_value"))
        }

        String anotherAttributes = attributes.another_attributes
        assertThat(anotherAttributes, Matchers.is("another_attributes_value"))
      }
    }
  }
}
