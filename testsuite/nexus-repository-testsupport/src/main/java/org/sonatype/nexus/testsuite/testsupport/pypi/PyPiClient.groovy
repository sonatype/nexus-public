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
        return new Link(url: it.attr('href'), name: it.text())
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
      def links = content.depthFirst().findAll { it.name() == 'a' }
      return links.collect {
        return new Link(url: it.@href, name: it.text())
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
    upload(attributes, { it.addBinaryBody("content", file, ContentType.DEFAULT_BINARY, filename) })
  }

  int upload(final Map<String, List<String>> attributes, 
             final String filename, 
             final InputStream input) 
  {
    upload(attributes, { it.addBinaryBody("content", input, ContentType.DEFAULT_BINARY, filename)} )
  }

  int upload(final Map<String, List<String>> attributes, 
             final Closure<MultipartEntityBuilder> addBinaryBody) 
  {
    MultipartEntityBuilder builder = new MultipartEntityBuilder()
    attributes.each { entry ->
      entry.value.each { value ->
        builder.addTextBody(entry.key, value)
      }
    }
    addBinaryBody(builder)
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

  static class Link {

    String url

    String name

  }

  static class Result {

    String pypiOrdering

    String version

    String name

    String summary
  }
}
