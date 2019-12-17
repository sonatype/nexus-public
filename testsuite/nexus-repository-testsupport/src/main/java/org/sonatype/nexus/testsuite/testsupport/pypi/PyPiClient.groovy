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


import org.sonatype.goodies.testsupport.TestData
import org.sonatype.nexus.repository.http.HttpStatus
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport

import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * PyPI client.
 */
class PyPiClient
    extends FormatClientSupport
{

  static final String CONTENT = "content"

  static final String GPG_SIGNATURE = "gpg_signature"

  private final String SEARCH_REQUEST = '''\
<methodCall>
  <methodName>search</methodName>
  <params>
    <param>
      <value>
        <struct>
          <member>
            <name>name</name>
            <value>
              <array>
                <data>
                  <value>
                    <string>$query</string>
                  </value>
                </data>
              </array>
            </value>
          </member>
          <member>
            <name>summary</name>
            <value>
              <array>
                <data>
                  <value>
                    <string>$query</string>
                  </value>
                </data>
              </array>
            </value>
          </member>
        </struct>
      </value>
    </param>
    <param>
      <value>
        <string>or</string>
      </value>
    </param>
  </params>
</methodCall>
'''

  PyPiClient(final CloseableHttpClient httpClient,
             final HttpClientContext httpClientContext,
             final URI repositoryUri,
             final TestData testData)
  {
    super(httpClient, httpClientContext, repositoryUri)
  }

  List<Link> rootIndex() {
    def response = get("simple/")
    try {
      assert status(response) == HttpStatus.OK
      Document document = Jsoup.parse(EntityUtils.toString(response.entity))
      Elements links = document.select("a[href]")
      return links.collect {
        Map<String, String> attributes = it.attributes().collectEntries { [it.key, it.value] }
        return new Link(url: it.attr('href'), name: it.text(), attributes: attributes)
      }
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  List<Link> index(String pkg) {
    getIndex("$pkg/")
  }

  List<Link> indexWithoutSlash(String pkg) {
    getIndex("$pkg")
  }

  List<Link> getIndex(String pkg) {
    def response = get("simple/$pkg")
    try {
      assert status(response) == HttpStatus.OK
      def content = new XmlSlurper().parse(response.entity.content)
      List<NodeChild> links = content.depthFirst().findAll { it.name() == 'a' }
      return links.collect {
        return new Link(url: it.@href, name: it.text(), attributes: it.attributes())
      }
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  byte[] fetch(String path) {
    HttpResponse response = get("packages/$path")
    try {
      assert status(response) == HttpStatus.OK
      assert response.entity
      return EntityUtils.toByteArray(response.entity)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  byte[] fetchWithoutPackagesUrlPart(String path) {
    HttpResponse response = get("$path")
    try {
      assert status(response) == HttpStatus.OK
      assert response.entity
      return EntityUtils.toByteArray(response.entity)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  List<Result> search(String query) {
    HttpPost request = new HttpPost(resolve('pypi'))
    request.setEntity(new StringEntity(SEARCH_REQUEST.replace('$query', XmlUtil.escapeXml(query))))
    HttpResponse response = execute(request)
    try {
      assert status(response) == HttpStatus.OK
      def content = new XmlSlurper().parse(response.entity.content)
      def results = content.depthFirst().findAll { it.name() == 'struct' }
      return results.collect {
        String name = it.children().findResult { it.name.text() == 'name' ? it.value.string.text() : null }
        String version = it.children().findResult { it.name.text() == 'version' ? it.value.string.text() : null }
        String summary = it.children().findResult { it.name.text() == 'summary' ? it.value.string.text() : null }
        String order = it.children().findResult { it.name.text() == '_pypi_ordering' ? it.value.boolean.text() : null }
        return new Result(name: name, version: version, summary: summary, pypiOrdering: order)
      }
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  int upload(final Map<String, List<String>> attributes, 
             final String filename, 
             final File file)
  {
    upload(attributes, { it.addBinaryBody(CONTENT, file, ContentType.DEFAULT_BINARY, filename) })
  }

  int upload(final Map<String, List<String>> attributes, 
             final String filename, 
             final InputStream input) 
  {
    upload(attributes, { it.addBinaryBody(CONTENT, input, ContentType.DEFAULT_BINARY, filename)} )
  }

  int upload(final Map<String, List<String>> attributes,
             final Closure<MultipartEntityBuilder> addBinaryBody)
  {
    MultipartEntityBuilder builder = aMultipartEntityBuilder(attributes)
    addBinaryBody(builder)
    return sendPostRequest(builder)
  }

  private static MultipartEntityBuilder aMultipartEntityBuilder(Map<String, List<String>> attributes) {
    MultipartEntityBuilder multipartPayloadBuilder = new MultipartEntityBuilder()
    attributes.each { entry ->
      entry.value.each { value ->
        multipartPayloadBuilder.addTextBody(entry.key, value)
      }
    }
    multipartPayloadBuilder
  }

  private int sendPostRequest(MultipartEntityBuilder builder) {
    HttpPost request = new HttpPost(repositoryBaseUri)
    request.setEntity(builder.build())
    HttpResponse response = execute(request)
    try {
      return status(response)
    }
    finally {
      EntityUtils.consumeQuietly(response.entity)
    }
  }

  int uploadWithContentSetTwice(final Map<String, List<String>> attributes,
                                final File file) {
    MultipartEntityBuilder multipartPayloadBuilder = aMultipartEntityBuilder(attributes)
    multipartPayloadBuilder.addBinaryBody(CONTENT, file, ContentType.DEFAULT_BINARY, file.name)
    multipartPayloadBuilder.addBinaryBody(CONTENT, file, ContentType.DEFAULT_BINARY, file.name)
    sendPostRequest(multipartPayloadBuilder)
  }

  int uploadNoContent(final Map<String, List<String>> attributes) {
    MultipartEntityBuilder multipartPayloadBuilder = aMultipartEntityBuilder(attributes)
    sendPostRequest(multipartPayloadBuilder)
  }

  int uploadWheelFileAndSignature(final Map<String, List<String>> attributes, final File wheelFile,
                                  final File gpgSignature)
  {
    MultipartEntityBuilder multipartPayloadBuilder = aMultipartEntityBuilder(attributes)
    multipartPayloadBuilder.addBinaryBody(CONTENT, wheelFile, ContentType.DEFAULT_BINARY, wheelFile.name)
    multipartPayloadBuilder.addBinaryBody(GPG_SIGNATURE, gpgSignature, ContentType.TEXT_PLAIN, gpgSignature.name)
    sendPostRequest(multipartPayloadBuilder)
  }

  int uploadWithGpgSignatureSetTwice(final Map<String, List<String>> attributes,
                                     final File wheelFile,
                                     final File gpgSignature)
  {
    MultipartEntityBuilder multipartPayloadBuilder = aMultipartEntityBuilder(attributes)
    multipartPayloadBuilder.addBinaryBody(CONTENT, wheelFile, ContentType.DEFAULT_BINARY, wheelFile.name)
    multipartPayloadBuilder.addBinaryBody(GPG_SIGNATURE, gpgSignature, ContentType.TEXT_PLAIN, gpgSignature.name)
    multipartPayloadBuilder.addBinaryBody(GPG_SIGNATURE, gpgSignature, ContentType.TEXT_PLAIN, gpgSignature.name)
    sendPostRequest(multipartPayloadBuilder)
  }

  static class Link {

    String url

    String name

    Map<String, String> attributes
  }

  static class Result {

    String pypiOrdering

    String version

    String name

    String summary
  }
}
