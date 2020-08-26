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
package org.sonatype.nexus.testsuite.testsupport.saml

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
 * Saml Client.
 */
class SamlClient
    extends FormatClientSupport
{
  SamlClient(final CloseableHttpClient httpClient,
             final HttpClientContext httpClientContext,
             final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri)
  }

  CloseableHttpResponse fetch(final String path) throws IOException {
    return execute(new HttpGet(resolve(path)));
  }
}
