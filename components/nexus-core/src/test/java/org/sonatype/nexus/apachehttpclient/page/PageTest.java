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
package org.sonatype.nexus.apachehttpclient.page;

import java.net.SocketException;

import org.sonatype.nexus.apachehttpclient.page.Page.PageContext;
import org.sonatype.nexus.apachehttpclient.page.Page.UnexpectedPageResponse;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PageTest
    extends TestSupport
{
  @Test
  public void simpleCase()
      throws Exception
  {
    final Server server =
        Server.withPort(0).serve("/foo/bar/").withBehaviours(Behaviours.content("<html></html>"));
    server.start();
    try {
      final String repoRootUrl = server.getUrl().toString() + "/foo/bar/";
      final PageContext context = new PageContext(new DefaultHttpClient());
      final Page page = Page.getPageFor(context, repoRootUrl);
      assertThat(page.getUrl(), equalTo(repoRootUrl));
      assertThat(page.getHttpResponse().getStatusLine().getStatusCode(), equalTo(200));
      assertThat(page.getDocument(), notNullValue());
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void error404WithBody()
      throws Exception
  {
    final Server server = Server.withPort(0).serve("/foo/bar/").withBehaviours(Behaviours.error(404));
    server.start();
    try {
      final String repoRootUrl = server.getUrl().toString() + "/foo/bar/";
      final PageContext context = new PageContext(new DefaultHttpClient());
      final Page page = Page.getPageFor(context, repoRootUrl);
      assertThat(page.getUrl(), equalTo(repoRootUrl));
      assertThat(page.getHttpResponse().getStatusLine().getStatusCode(), equalTo(404));
      assertThat(page.getDocument(), notNullValue());
    }
    finally {
      server.stop();
    }
  }

  @Test(expected = UnexpectedPageResponse.class)
  public void error500IsException()
      throws Exception
  {
    final Server server = Server.withPort(0).serve("/*").withBehaviours(Behaviours.error(500));
    server.start();
    try {
      final String repoRootUrl = server.getUrl().toString() + "/foo/bar/";
      final PageContext context = new PageContext(new DefaultHttpClient());
      final Page page = Page.getPageFor(context, repoRootUrl);
    }
    finally {
      server.stop();
    }
  }

  @Test(expected = SocketException.class)
  public void errorConnectionRefusedException()
      throws Exception
  {
    final String repoRootUrl;
    final Server server = Server.withPort(0).serve("/*").withBehaviours(Behaviours.error(500));
    server.start();
    try {
      repoRootUrl = server.getUrl().toString() + "/foo/bar/";
    }
    finally {
      server.stop();
    }
    final PageContext context = new PageContext(new DefaultHttpClient());
    final Page page = Page.getPageFor(context, repoRootUrl);
  }
}
