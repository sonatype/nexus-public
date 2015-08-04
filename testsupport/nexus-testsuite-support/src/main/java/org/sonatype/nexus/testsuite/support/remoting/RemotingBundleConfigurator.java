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

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.testsuite.support.NexusITSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Configures Nexus bundles for groovy-remote control support.
 *
 * @since 2.6
 */
public class RemotingBundleConfigurator
{
  private final NexusITSupport test;

  private Integer port;

  public RemotingBundleConfigurator(final NexusITSupport test) {
    this.test = checkNotNull(test);
  }

  public RemotingBundleConfigurator setPort(final Integer port) {
    this.port = port;
    return this;
  }

  public NexusBundleConfiguration configure(final NexusBundleConfiguration config) {
    checkState(port != null, "Missing port");

    return config.setLogLevel("org.sonatype.nexus.groovyremote", "DEBUG")
        // configure port for groovy-remote plugin
        .setSystemProperty("nexus.groovyremote.port", String.valueOf(port))
        .addPlugins(
            // install groovy-remote plugin
            test.artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-groovyremote-plugin"
            )
        );
  }
}
