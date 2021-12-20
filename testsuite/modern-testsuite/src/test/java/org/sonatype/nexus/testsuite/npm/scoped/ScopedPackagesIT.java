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
package org.sonatype.nexus.testsuite.npm.scoped;

import java.io.File;
import java.util.List;

import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.npm.MockNpmRegistry;
import org.sonatype.nexus.testsuite.npm.NpmITSupport;
import org.sonatype.sisu.goodies.testsupport.TestData;

import com.bolyuba.nexus.plugin.npm.client.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;

/**
 * IT for scoped packages, ensuring that NX will obey "scoped" package requests. In this IT we have two proxies that
 * has same named packages (hence the one being member before the other shades the other). Still, the "scope" allows
 * npm to get the proper package by specifying a scope of the package, whereis using "normal" (flat) package
 * name (as in npm 1.x) getting the package would become impossible,
 */
public class ScopedPackagesIT
    extends NpmITSupport
{
  private MockNpmRegistry mockNpmRegistry1;

  private MockNpmRegistry mockNpmRegistry2;

  private NpmGroupRepository npmGroupRepository;

  public ScopedPackagesIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Starts mock NPM registries.
   *
   * @see TestData
   */
  @Before
  public void startMockNpmRegistryServer()
      throws Exception
  {
    // two registries containing same named package
    mockNpmRegistry1 = new MockNpmRegistry(testData().resolveFile("registry1"), null).start();
    mockNpmRegistry2 = new MockNpmRegistry(testData().resolveFile("registry2"), null).start();

    final NpmHostedRepository hosted = createNpmHostedRepository("hosted");
    final NpmProxyRepository proxy1 = createNpmProxyRepository("registry1", mockNpmRegistry1.getUrl());
    final NpmProxyRepository proxy2 = createNpmProxyRepository("registry2", mockNpmRegistry2.getUrl());
    npmGroupRepository = createNpmGroupRepository("npmgroup", hosted.id(), proxy1.id(), proxy2.id());
  }

  /**
   * Stops mock NPM registries.
   *
   * @see #startMockNpmRegistryServer()
   */
  @After
  public void stopMockNpmRegistryServer()
      throws Exception
  {
    if (mockNpmRegistry1 != null) {
      mockNpmRegistry1.stop();
    }
    if (mockNpmRegistry2 != null) {
      mockNpmRegistry2.stop();
    }
  }

  @Test
  public void scopedRequest() throws Exception {
    final File localDirectory = util.createTempDir();

    final File testprojectPlain = new File(localDirectory, testMethodName() + "-testproject-plain");
    content().download(Location.repositoryLocation(npmGroupRepository.id(), "/testproject"), testprojectPlain);

    final File testprojectScoped = new File(localDirectory, testMethodName() + "-testproject-scoped");
    content()
        .download(Location.repositoryLocation(npmGroupRepository.id(), "/@registry2/testproject"), testprojectScoped);

    final String testProjectPlainString = Files.toString(testprojectPlain, Charsets.UTF_8);
    final String testProjectScopedString = Files.toString(testprojectScoped, Charsets.UTF_8);

    assertThat(testProjectPlainString, containsString("from registry1"));
    assertThat(testProjectScopedString, containsString("from registry2"));

    JSONObject registryPlainDoc = new JSONObject(testProjectPlainString);
    JSONObject registryScopedDoc = new JSONObject(testProjectScopedString);

    assertThat(
        (String) registryPlainDoc.getJSONObject("versions").getJSONObject("0.0.1").getJSONObject("dist").get("tarball"),
        endsWith("/nexus/content/groups/npmgroup/testproject/-/testproject-0.0.1.tgz"));
    assertThat(
        (String) registryScopedDoc.getJSONObject("versions").getJSONObject("0.0.1").getJSONObject("dist")
            .get("tarball"),
        endsWith("/nexus/content/groups/npmgroup/@registry2/testproject/-/testproject-0.0.1.tgz"));

    // registry1 should been asked for metadata 2 times
    final List<String> registry1Paths = mockNpmRegistry1.getPathRecorder().getPathsForVerb("GET");
    assertThat(registry1Paths, hasSize(2));
    assertThat(registry1Paths, containsInAnyOrder("/testproject", "/@registry2%2Ftestproject"));

    // registry2 should been asked for metadata 2 times  as well
    final List<String> registry2Paths = mockNpmRegistry2.getPathRecorder().getPathsForVerb("GET");
    assertThat(registry2Paths, hasSize(2));
    assertThat(registry2Paths, containsInAnyOrder("/testproject", "/@registry2%2Ftestproject"));
  }
}
