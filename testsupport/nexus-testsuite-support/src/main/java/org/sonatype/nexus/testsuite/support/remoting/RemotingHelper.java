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
package org.sonatype.nexus.testsuite.support.remoting;

import java.net.MalformedURLException;
import java.net.URL;

import org.sonatype.nexus.groovyremote.client.GroovyRemoteClient;
import org.sonatype.nexus.groovyremote.client.GroovyRemoteClient.Builder;
import org.sonatype.sisu.litmus.testsupport.TestUtil;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to get a groovy-remote client.
 *
 * @since 2.6
 */
public class RemotingHelper
{
  private final URL url;

  private GroovyRemoteClient client;

  public RemotingHelper(final URL url) {
    this.url = checkNotNull(url);
  }

  public RemotingHelper(final int port) {
    try {
      this.url = new URL("http://localhost:" + port);
    }
    catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    }
  }

  public void prepare(final TestUtil util) throws Exception {
    client = new Builder()
        // this directory is used when using java-based tests:
        .setClassesDir(util.createTempDir("groovy-classes"))
            // groovy-based tests classes end up here:
        .addExtraDir(util.resolveFile("target/test-classes"))
            // for good measure:
        .addExtraDir(util.resolveFile("target/classes"))
        .setClassLoader(Thread.currentThread().getContextClassLoader())
        .setUrl(url)
        .build();
  }

  public void cleanup() throws Exception {
    if (client != null) {
      client.getShell().resetLoadedClasses();
    }
  }

  public GroovyRemoteClient getClient() {
    checkState(client != null, "Not prepared");
    return client;
  }
}
