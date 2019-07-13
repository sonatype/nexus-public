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

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours
import org.sonatype.goodies.httpfixture.server.fluent.Server
import org.sonatype.goodies.lifecycle.LifecycleSupport

import groovy.xml.XmlUtil

import static com.google.common.io.BaseEncoding.base16
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.content

/**
 * Support for a very basic mock PyPI repository (especially for proxy testing).
 */
class PyPiUpstream
    extends LifecycleSupport
{
  private static final byte[] MIN_ZIP = base16().decode('504B0506000000000000000000000000000000000000')

  private static final String MIN_ZIP_MD5 = '76cdb2bad9582d23c1f6f4d868218d6c'

  private static final String ROOT_INDEX_EXAMPLE = '''\
<html>
  <head>
    <title>Simple index</title>
  </head>
  <body>
    <a href="/simple/1and1/">1and1</a>
    <a href="/simple/ansible-shell/">ansible-shell</a>
    <a href="/simple/roleplay/">roleplay</a>
    <a href="/simple/tibl-cli/">tibl-cli</a>
    <a href="/simple/zzhfun/">zzhfun</a>
  </body>
</html>
'''

  private static final String INDEX_TEMPLATE = '''\
<html>
<head>
  <title>Links for $name</title>
  <meta name="api-version" value="2"/>
</head>
<body>
<h1>Links for $name</h1>
$entries
</body>
</html>
'''

  private static final String INDEX_ENTRY_TEMPLATE = '<a href="$url" rel="internal">$filename</a><br/>'

  private static final String ARCHIVE_URL_TEMPLATE = '/packages/00/01/02/$filename#md5=$digest'

  private static final String INDEX_URL_TEMPLATE = '/simple/$name/'

  private static final String FILENAME_TEMPLATE = '$name-$version-py2.py3-none-any.whl'

  private Server repository

  @Override
  void doStart() throws Exception {
    repository = Server.withPort(0).start()

    repository.serve('/simple/').withBehaviours(Behaviours.content(ROOT_INDEX_EXAMPLE))
  }

  @Override
  void doStop() throws Exception {
    if (repository != null) {
      repository.stop()
    }
  }

  /**
   * Enrolls mock package versions to the {@link #repository}, building index pages and registering empty zip files.
   */
  void enrollPackages(final String name, final String... versions) throws Exception {
    ensureStarted()
    StringBuilder sb = new StringBuilder()
    for (String version : versions) {
      String filename = buildFilename(name, version)
      String url = buildUrl(filename)
      sb.append(buildIndexEntry(url, filename)).append('\n')
      repository.serve(url.substring(0, url.indexOf('#md5='))).withBehaviours(content(MIN_ZIP))
    }
    repository.serve(INDEX_URL_TEMPLATE.replace('$name', XmlUtil.escapeXml(name))).
        withBehaviours(Behaviours.content(buildIndex(name, sb.toString())))
  }

  /**
   * Returns the upstream URL that the proxy should point at.
   */
  String upstreamUrl() {
    ensureStarted()
    return "http://localhost:${repository.getPort()}/"
  }

  private String buildIndex(final String name, final String entries) {
    return INDEX_TEMPLATE
        .replace('$name', XmlUtil.escapeXml(name))
        .replace('$entries', entries)

  }
  private String buildIndexEntry(final String url, final String filename) {
    return INDEX_ENTRY_TEMPLATE
        .replace('$url', '../..' + url)
        .replace('$filename', XmlUtil.escapeXml(filename))
  }

  private String buildUrl(final String filename) {
    return ARCHIVE_URL_TEMPLATE
        .replace('$filename', XmlUtil.escapeXml(filename))
        .replace('$digest', XmlUtil.escapeXml(MIN_ZIP_MD5))
  }

  private String buildFilename(final String name, final String version) {
    return FILENAME_TEMPLATE
        .replace('$name', name)
        .replace('$version', version)
  }
}
