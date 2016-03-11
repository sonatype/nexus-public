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
package org.sonatype.nexus.testsuite.npm;

import java.lang.ProcessBuilder.Redirect;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import categories.npm;
import com.bolyuba.nexus.plugin.npm.client.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assume.assumeTrue;
import static org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy.Strategy.EACH_TEST;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.firstAvailableTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.systemTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.testParameters;
import static org.sonatype.sisu.goodies.common.Varargs.$;

/**
 * Support for NPM integration tests.
 */
@Category(npm.class)
@NexusStartAndStopStrategy(EACH_TEST)
public abstract class NpmITSupport
    extends NexusRunningParametrizedITSupport
{
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return firstAvailableTestParameters(systemTestParameters(), testParameters(
        $("${it.nexus.bundle.groupId}:${it.nexus.bundle.artifactId}:zip:bundle"))).load();
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public NpmITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration)
        .setLogLevel("com.bolyuba.nexus.plugin.npm", "DEBUG")
        .setLogLevel("remote.storage.outbound", "DEBUG") // see outbound HTTP requests
        .addPlugins(
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-orient-plugin"
            ),
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-npm-repository-plugin"
            ));
  }

  @BeforeClass
  public static void ignoreIfNpmNotPresent() {
    List<String> command = Lists.newArrayList();
    command.add("npm");
    command.add("--version");

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(Redirect.INHERIT);

    try {
      int exitCode = processBuilder.start().waitFor();
      assumeTrue("npm is present", exitCode == 0);
    }
    catch (Exception e) {
      assumeTrue("npm is present", false);
    }
  }

  /**
   * Creates a NPM hosted repository in NX instance.
   */
  public NpmHostedRepository createNpmHostedRepository(final String id) {
    checkNotNull(id);
    return repositories().create(NpmHostedRepository.class, id).withName(id).save();
  }

  /**
   * Creates a NPM Proxy repository in NX instance.
   */
  public NpmProxyRepository createNpmProxyRepository(final String id, final String registryUrl) {
    checkNotNull(id);
    checkNotNull(registryUrl);
    return repositories().create(NpmProxyRepository.class, id)
        .asProxyOf(registryUrl).withName(id).save();
  }

  /**
   * Creates a NPM Group repository in NX instance.
   */
  public NpmGroupRepository createNpmGroupRepository(final String id, String... members) {
    checkNotNull(id);
    return repositories().create(NpmGroupRepository.class, id).withName(id).addMember(members).save();
  }

  /**
   * The {@link Content} client.
   */
  public Content content() {
    return client().getSubsystem(Content.class);
  }

  /**
   * The {@link Repositories} client.
   */
  public Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }
}
