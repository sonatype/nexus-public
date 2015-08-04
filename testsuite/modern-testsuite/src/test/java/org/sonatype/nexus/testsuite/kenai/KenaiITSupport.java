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
package org.sonatype.nexus.testsuite.kenai;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.sisu.filetasks.FileTaskBuilder;
import org.sonatype.tests.http.server.fluent.Server;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy.Strategy.EACH_TEST;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;
import static org.sonatype.sisu.filetasks.builder.FileRef.path;

/**
 * Support for Kenai integration tests.
 *
 * @since 2.5
 */
@NexusStartAndStopStrategy(EACH_TEST)
public class KenaiITSupport
    extends NexusRunningParametrizedITSupport
{

  @Inject
  private FileTaskBuilder fileTaskBuilder;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public KenaiITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration)
        .setLogLevel("org.sonatype.security.realms.kenai", "DEBUG")
        .addPlugins(
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-kenai-plugin"
            )
        )
        .addOverlays(
            fileTaskBuilder.copy()
                .directory(file(testData().resolveFile("preset-nexus")))
                .filterUsing("mock-kenai-port", String.valueOf(server.getPort()))
                .to().directory(path("sonatype-work/nexus/conf"))
        );
  }

  private static Server server;

  @BeforeClass
  public static void startMockKenaiServer()
      throws Exception
  {
    server = Server.withPort(0).serve("/*").withBehaviours(new KenaiAuthcBehaviour()).start();
  }

  @AfterClass
  public static void stopMockKenaiServer()
      throws Exception
  {
    if (server != null) {
      server.stop();
    }
  }

  public static String uniqueName(final String prefix) {
    return prefix + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
  }

  public Content content() {
    return client().getSubsystem(Content.class);
  }

}
