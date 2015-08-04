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
package org.sonatype.nexus.testsuite.routing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Content.ForceDirective;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.routing.Routing;
import org.sonatype.nexus.testsuite.NexusCoreITSupport;
import org.sonatype.nexus.testsuite.client.RoutingTest;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Support class for Automatic Routing Core feature (NEXUS-5472), aka "proxy404".
 *
 * @author cstamas
 * @since 2.4
 */
public abstract class RoutingITSupport
    extends NexusCoreITSupport
{
  protected RoutingITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration)
        .setLogLevel("org.sonatype.nexus.proxy", "DEBUG")
        .setLogLevel("org.sonatype.nexus.proxy.maven.routing", "TRACE");
  }

  /**
   * Returns {@link Routing} client subsystem.
   *
   * @return client for routing.
   */
  public Routing routing() {
    return client().getSubsystem(Routing.class);
  }

  /**
   * Returns {@link RoutingTest} client subsystem.
   *
   * @return client for routing ITs.
   */
  public RoutingTest routingTest() {
    return client().getSubsystem(RoutingTest.class);
  }

  /**
   * Does HTTP GET against given URL.
   */
  protected HttpResponse executeGet(final String url)
      throws IOException
  {
    final HttpClient httpClient = new DefaultHttpClient();
    final HttpGet get = new HttpGet(url);
    final HttpResponse httpResponse = httpClient.execute(get);
    return httpResponse;
  }

  /**
   * Fetches file from given URL.
   */
  protected InputStream getPrefixFileFrom(final String url)
      throws IOException
  {
    final HttpResponse httpResponse = executeGet(url);
    assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
    assertThat(httpResponse.getEntity(), is(notNullValue()));
    return httpResponse.getEntity().getContent();
  }

  protected boolean exists(final Location location, ForceDirective directive)
      throws IOException
  {
    return content().existsWith(location, directive);
  }

  protected boolean noscrape(final Location location, ForceDirective directive)
      throws IOException
  {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try {
      content().downloadWith(location, directive, buf);
      return new String(buf.toByteArray(), "UTF-8").contains("@ unsupported");
    }
    catch (NexusClientNotFoundException e) {
      return false; // requested file was not found, so the repository is not marked as no-scrape
    }
  }

}
