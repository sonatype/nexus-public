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

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

import org.sonatype.goodies.testsupport.TestData
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.common.hash.HashAlgorithm
import org.sonatype.nexus.repository.view.ContentTypes
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BinaryNode
import com.google.common.collect.Maps
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.http.HttpResponse
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime

import static org.sonatype.nexus.repository.http.HttpStatus.CREATED
import static org.sonatype.nexus.repository.http.HttpStatus.OK

/**
 * npm Client.
 */
class NpmClient
    extends FormatClientSupport
{
  private static final ObjectMapper mapper = new ObjectMapper()

  private static final TypeReference<HashMap<String, Object>> RAW_JSON = new TypeReference<HashMap<String, Object>>() {
    // nop
  }

  private final TestData testData

  String apiToken

  NpmClient(final CloseableHttpClient httpClient,
            final HttpClientContext httpClientContext,
            final URI repositoryBaseUri,
            final TestData testData)
  {
    super(httpClient, httpClientContext, repositoryBaseUri)
    this.testData = testData
  }

  NpmClient(final CloseableHttpClient httpClient,
            final HttpClientContext httpClientContext,
            final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri)
  }

  NestedAttributesMap createPackage() {
    return new NestedAttributesMap('packageJson', Maps.newHashMap())
  }

  NestedAttributesMap createDeployablePackage(
      String pName,
      String pVersion,
      String pScope = null,
      String username = 'admin',
      String description = 'Simple package for testing',
      List<String> keywords = ['test', 'simple'],
      String author = 'Tamas Cservenak <tamas@cservenak.net> (http://cstamas.org/)')
  {
    String tarballName = "${pName}-${pVersion}.tgz"
    String scopedName = pScope ? "${pScope}/${pName}" : pName

    NestedAttributesMap packageRoot = createPackage()
    packageRoot.set('name', scopedName)
    packageRoot.set('version', pVersion)
    packageRoot.set('description', description)
    packageRoot.set('maintainers', getMaintainers(username))
    packageRoot.set('nullField', null)

    Map<String, Object> pkgJson = [
        name: scopedName,
        version: pVersion,
        description: description,
        main: 'index.js',
        publishConfig: [
            registry: 'http://localhost:8081/repository/npmhosted1/'
        ],
        scripts: [
            test: 'echo "No Tests"'
        ],
        keywords: keywords,
        author: author,
        license: 'ISC'
    ]

    byte[] attachmentBytes = packageTgz(pkgJson)

    packageRoot.child('dist-tags').set('latest', pVersion)
    NestedAttributesMap versions = packageRoot.child('versions')
    NestedAttributesMap version = versions.child(pVersion)
    version.set('name', scopedName)
    version.set('version', pVersion)
    version.set('maintainers', getMaintainers(username))
    NestedAttributesMap npmUser = version.child('_npmUser')
    npmUser.set('name', username)
    NestedAttributesMap dist = version.child('dist')
    dist.set('shasum', HashAlgorithm.SHA1.function().hashBytes(attachmentBytes).toString())
    dist.set('tarball', resolve("${pName}/-/${tarballName}"))

    NestedAttributesMap attachments = packageRoot.child('_attachments')
    NestedAttributesMap attachment = attachments.child(tarballName)
    attachment.set('content_type', ContentTypes.APPLICATION_GZIP)
    attachment.set('data', new BinaryNode(attachmentBytes).asText())
    attachment.set('length', attachmentBytes.length)

    return packageRoot
  }

  byte[] packageTgz(Map<String, Object> pkgJson) {
    def pkgJsonBytes = JsonOutput.toJson(pkgJson).getBytes("UTF-8")

    TarArchiveEntry tae = new TarArchiveEntry("package/package.json")
    tae.setSize(pkgJsonBytes.length)

    ByteArrayOutputStream bos = new ByteArrayOutputStream()
    new TarArchiveOutputStream(new GzipCompressorOutputStream(bos)).withCloseable { taos ->
      taos.putArchiveEntry(tae)
      taos.write(pkgJsonBytes)
      taos.closeArchiveEntry()
    }

    bos.toByteArray()
  }

  private List<Object> getMaintainers(String username) {
    def listOfMaintainers = new ArrayList<>()
    HashMap<String, Object> data = getMaintainer(username)
    listOfMaintainers.add(data)
    return listOfMaintainers
  }

  private HashMap<String, Object> getMaintainer(String username) {
    Map<String, Object> data = Maps.newHashMap()
    data.put('name', username)
    data.put('email', 'foo@bar.com')
    return data
  }

  void publish(NestedAttributesMap packageRoot) {
    HttpResponse response = put(
        resolve(packageRoot.get('name', String)),
        packageRoot
    )
    EntityUtils.consume(response.entity)
    assert status(response) == OK || status(response) == CREATED
  }

  void publishRev(NestedAttributesMap packageRoot, String rev) {
    HttpResponse response = put(
        resolve(packageRoot.get('name', String) + '/-rev/' + rev),
        packageRoot
    )
    EntityUtils.consume(response.entity)
    assert status(response) == OK
  }

  void unpublish1x(String packageName) {
    HttpResponse response = delete(
        resolve(packageName)
    )
    EntityUtils.consume(response.entity)
    assert status(response) == OK
  }

  void unpublish(String packageName) {
    String rev
    HttpResponse response = get(
        resolve(packageName), true
    )
    try {
      assert status(response) == OK
      assert response.entity
      assert response.entity.contentType.value == ContentTypes.APPLICATION_JSON
      NestedAttributesMap metadata = deserialize(response.entity.content)
      rev = metadata['_rev']
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }

    try {
      response = delete(
          resolve(packageName + '/-rev/' + rev)
      )
    }
    finally {
      EntityUtils.consume(response.entity)
    }
    assert status(response) == OK
  }

  void unpublishTarball1x(String tarballUrl) {
    HttpResponse response = delete(
        new URI(tarballUrl)
    )
    EntityUtils.consume(response.entity)
    assert status(response) == OK
  }

  /**
   * "npm deprecate" -- is stricter than CLI (for testing purposes): requires that deprecated version exists,
   * unlike CLI, which would just "edit" the JSON without performing any modification.
   */
  void deprecate(String packageName, String version, String deprecationMessage) {
    HttpResponse response = get(
        resolve(packageName), true
    )
    try {
      assert status(response) == OK
      assert response.entity
      assert response.entity.contentType.value == ContentTypes.APPLICATION_JSON
      NestedAttributesMap metadata = deserialize(response.entity.content)
      NestedAttributesMap versionMap = metadata.child('versions').child(version)
      assert versionMap
      versionMap.set('deprecated', deprecationMessage)
      publish(metadata)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  /**
   * "npm undeprecate" -- is stricter than CLI (for testing purposes): requires that undeprecated version exists,
   * and to be deprecated, unlike CLI, which would just "edit" the JSON without performin any modification.
   */
  void undeprecate(String packageName, String version) {
    HttpResponse response = get(
        resolve(packageName), true
    )
    try {
      assert status(response) == OK
      assert response.entity
      assert response.entity.contentType.value == ContentTypes.APPLICATION_JSON
      NestedAttributesMap metadata = deserialize(response.entity.content)
      NestedAttributesMap versionMap = metadata.child('versions').child(version)
      assert versionMap
      assert versionMap.contains('deprecated')
      versionMap.remove('deprecated')
      publish(metadata)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  NestedAttributesMap fetch(String packageName) {
    HttpResponse response = get(
        resolve(packageName)
    )
    try {
      assert status(response) == OK
      assert response.entity
      assert response.entity.contentType.value == ContentTypes.APPLICATION_JSON
      return deserialize(response.entity.content)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  NestedAttributesMap conditionalFetch(final String packageName, final String modified, final int expectedStatus) {
    HttpResponse response = conditionalGet(resolve(packageName), modified)
    try {
      assert status(response) == expectedStatus
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  NestedAttributesMap search(final DateTime since = null) {
    HttpResponse response
    if (since) {
      response = get(resolve("-/all/since?stale=update_after&startkey=${since.millis}"))
    }
    else {
      response = get(resolve('-/all'))
    }
    try {
      assert status(response) == OK
      assert response.entity
      assert response.entity.contentType.value == ContentTypes.APPLICATION_JSON
      return deserialize(response.entity.content)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  NestedAttributesMap searchV1(final String term, final Integer size) {
    HttpResponse response

    String encodedTerm = URLEncoder.encode(term, 'UTF-8')
    response = get(resolve("-/v1/search?text=${encodedTerm}&size=${size}"))

    try {
      assert status(response) == OK
      assert response.entity
      assert response.entity.contentType.value == ContentTypes.APPLICATION_JSON
      return deserialize(response.entity.content)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  CloseableHttpResponse login(final String username, final String password, final String email) {
    def body = [
        _id: 'org.couchdb.user:' + username,
        name: username,
        password: password,
        email: email,
        type: 'user',
        roles: [],
        date: new Date()
    ]
    def nam = new NestedAttributesMap("foo", body)

    CloseableHttpResponse response = put(
        resolve("-/user/org.couchdb.user:${username}"),
        nam,
        username,
        password
    )

    return response
  }

  void authenticate(final String username, final String password, final String email) {
    login(username, password, email).withCloseable { response ->
      apiToken = new JsonSlurper().parse(response.entity.content).token
    }
  }

  void logout(final String token) {
    delete(resolve("-/user/token/${token}")).withCloseable { response ->
      assert status(response) == OK
    }
  }

  byte[] fetchTarball(String tarballUrl) {
    HttpResponse response = fetchTarballResponse(tarballUrl)
    try {
      assert status(response) == OK
      assert response.entity
      return EntityUtils.toByteArray(response.entity)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  HttpResponse fetchTarballResponse(String tarballUrl) {
    return get(new URI(tarballUrl))
  }

  NestedAttributesMap deserialize(final InputStream inputStream) {
    Map<String, Object> data = (Map<String, Object>) mapper.readValue(inputStream, RAW_JSON)
    NestedAttributesMap result = createPackage()
    result.backing().putAll(data)
    return result
  }

  private byte[] serialize(final NestedAttributesMap metadata) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream()
    mapper.writeValue(baos, metadata.backing())
    return baos.toByteArray()
  }

  HttpResponse getResponseForPackage(final String packageName) {
    return get(resolve(packageName))
  }

  private HttpResponse get(URI uri, boolean edit = false) {
    HttpGet get = new HttpGet(uri)
    if (edit) {
      get = new HttpGet(uri.toASCIIString() + "?write=true")
    }
    return execute(get)
  }

  private HttpResponse conditionalGet(URI uri, String modified) {
    HttpGet get = new HttpGet(uri)
    get.setHeader('If-Modified-Since', modified)
    return execute(get)
  }

  private CloseableHttpResponse put(URI uri, NestedAttributesMap entity, String username = null, String password = null) {
    HttpPut put = new HttpPut(uri)
    put.setEntity(new ByteArrayEntity(serialize(entity), ContentType.APPLICATION_JSON))
    if (apiToken) {
      put.setHeader("authorization", "Bearer $apiToken")
    }
    if (username && password) {
      return execute(put, username, password)
    }
    else {
      return execute(put)
    }
  }

  NestedAttributesMap listDistTags(String packageName) {
    return fetch('-/package/' + packageName + '/dist-tags')
  }

  void addDistTag(String packageName, String tag, String value, int stat = OK) {
    CloseableHttpResponse response = put(resolve('-/package/' + packageName + '/dist-tags/' + tag), value)
    EntityUtils.consume(response.entity)
    assert status(response) == stat
  }

  void removeDistTag(String packageName, String tag, int stat = OK) {
    CloseableHttpResponse response = delete(resolve('-/package/' + packageName + '/dist-tags/' + tag))
    EntityUtils.consume(response.entity)
    assert status(response) == stat
  }

  void audit(final String packageLock) {
      HttpResponse response = post(
          resolve("-/npm/v1/security/audits"),
          gzip(packageLock))
      EntityUtils.consume(response.entity)
      assert status(response) == OK
  }

  private byte[] gzip(final String str) throws IOException {
    ByteArrayOutputStream obj = new ByteArrayOutputStream()
    GZIPOutputStream gzip = new GZIPOutputStream(obj)
    gzip.write(str.getBytes(StandardCharsets.UTF_8))
    gzip.flush()
    gzip.close()
    return obj.toByteArray()
  }

  private CloseableHttpResponse put(URI uri, String entity, String username = null, String password = null) {
    HttpPut put = new HttpPut(uri)
    put.setEntity(new ByteArrayEntity(entity.bytes, ContentType.TEXT_PLAIN))
    if (apiToken) {
      put.setHeader("authorization", "Bearer $apiToken")
    }
    if (username && password) {
      return execute(put, username, password)
    }
    else {
      return execute(put)
    }
  }

  private CloseableHttpResponse post(URI uri, byte[] entity, String username = null, String password = null, ContentType contentType = null)
  {
    HttpPost post = new HttpPost(uri)
    post.setEntity(new ByteArrayEntity(entity, contentType))
    if (apiToken) {
      post.setHeader("authorization", "Bearer $apiToken")
    }
    if (username && password) {
      return execute(post, username, password)
    }
    else {
      return execute(post)
    }
  }

  CloseableHttpResponse delete(final String path) {
    return delete(resolve(path))
  }

  CloseableHttpResponse delete(URI uri) {
    HttpDelete delete = new HttpDelete(uri)
    return execute(delete)
  }
}
