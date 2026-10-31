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
package org.sonatype.nexus.bootstrap.entrypoint.core;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusProperties;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.springframework.boot.Banner.Mode.OFF;

// Minimalist list of root packages to scan, to start bootstrapping
@SpringBootApplication(
    scanBasePackages = {
        "org.sonatype.nexus.bootstrap.entrypoint"
    })
public class NexusRepositoryCoreApplication
{
  public static void main(final String[] args) {
    System.setProperty("spring.config.location", "etc/default-application.properties");
    // Ensure karaf.data is set as logback needs it early
    NexusDirectoryConfiguration.load();
    // since logback is going to start on its own, we need to tell it where to find its configuration
    System.setProperty("logging.config", "etc/logback/logback.xml");

    // NEXUS-47740: Disable the Spring logging shutdown hook is disabled
    // as it is abruptly terminating logging during shutdown
    // and thus preventing the logs from Nexus's shutdown sequence from being added to the log file
    System.setProperty("logging.register-shutdown-hook", "false");
    System.setProperty("nexus.edition", "CORE");

    new SpringApplicationBuilder(NexusRepositoryCoreApplication.class)
        .bannerMode(OFF)
        .initializers(NexusRepositoryCoreApplication::initialize)
        .web(WebApplicationType.NONE)
        .run(args);
  }

  private static void initialize(final ConfigurableApplicationContext applicationContext) {
    applicationContext.getEnvironment()
        .getPropertySources()
        .addFirst(new NexusProperties());
  }
}
