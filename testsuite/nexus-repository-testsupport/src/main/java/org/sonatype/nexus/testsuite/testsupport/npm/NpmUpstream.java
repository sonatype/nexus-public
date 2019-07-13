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
package org.sonatype.nexus.testsuite.testsupport.npm;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.goodies.lifecycle.LifecycleSupport;

import static org.sonatype.goodies.httpfixture.server.jetty.behaviour.Content.content;

public class NpmUpstream
    extends LifecycleSupport
{
  private Server remote;

  public NpmUpstream() {
  }

  @Override
  public void doStart() throws Exception {
    remote = Server.withPort(0).start();
  }

  @Override
  public void doStop() throws Exception {
    if (remote != null) {
      remote.stop();
    }
  }

  public String repositoryUrl() {
    ensureStarted();
    return "http://localhost:" + remote.getPort() + "/";
  }

  public void registerPackageRoot(final String proxyPackageRoot, final byte[] bytes) {
    remote.serve(proxyPackageRoot).withBehaviours(content(bytes));
  }
}
