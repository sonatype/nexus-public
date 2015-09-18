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
package org.sonatype.nexus.bootstrap;

import org.sonatype.nexus.bootstrap.ConfigurationBuilder.Customizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for customizing configuration from environment variables.
 *
 * @since 2.8
 */
public class EnvironmentVariables
  implements Customizer
{
  private static final Logger log = LoggerFactory.getLogger(EnvironmentVariables.class);

  @Override
  public void apply(final ConfigurationBuilder builder) throws Exception {
    // complain if we find any legacy environment variables
    maybeSetLegacy(builder, "application-host", "PLEXUS_APPLICATION_HOST");
    maybeSetLegacy(builder, "application-port", "PLEXUS_APPLICATION_PORT");
    maybeSetLegacy(builder, "nexus-work", "PLEXUS_NEXUS_WORK");
    maybeSetLegacy(builder, "nexus-context-path", "PLEXUS_CONTEXT_PATH");

    // non-legacy environment variables take precedence
    maybeSet(builder, "application-host", "NEXUS_APPLICATION_HOST");
    maybeSet(builder, "application-port", "NEXUS_APPLICATION_PORT");
    maybeSet(builder, "nexus-work", "NEXUS_WORK");
    maybeSet(builder, "nexus-context-path", "NEXUS_CONTEXT_PATH");
  }

  private boolean maybeSet(final ConfigurationBuilder builder, final String property, final String env) {
    String value = System.getenv(env);
    if (value != null) {
      log.debug("Environment variable: {}={}", env, value);
      builder.set(property, value);
      return true;
    }
    return false;
  }

  private void maybeSetLegacy(final ConfigurationBuilder builder, final String property, final String env) {
    if (maybeSet(builder, property, env)) {
      log.warn("Detected legacy environment variable: {}", env);
    }
  }
}
