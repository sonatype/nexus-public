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
package com.sonatype.nexus.docker.testsupport.framework;

import java.util.Optional;

import org.junit.After;
import org.junit.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerFetchException;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Docker containers can be pulled and running with commands.
 */
public class DockerContainerClientTestIT
{
  private static final String IMAGE_HELLO_WORLD = "docker-all.repo.sonatype.com/hello-world";

  private static final String IMAGE_DOCKER = "docker-all.repo.sonatype.com/docker";

  private static final String IMAGE_CENTOS = "docker-all.repo.sonatype.com/centos";

  private DockerContainerClient underTest;

  @After
  public void cleanUp() {
    underTest.close();
  }

  @Test(expected = Test.None.class)
  public void when_Pull_HelloWorld_Expect_Container_UpAndRunning() {
    underTest = new DockerContainerClient(IMAGE_HELLO_WORLD);
    underTest.run();
  }

  @Test
  public void when_Exec_YumVersion_On_CentosLatest_Expect_Execution_To_Succeed() {
    underTest = new DockerContainerClient(IMAGE_CENTOS);
    Optional<ExecResult> result = underTest.exec("yum --version");
    assertTrue(result.isPresent());
    assertEquals(0, result.get().getExitCode());
    assertFalse(result.get().getStdout().isEmpty());
    assertTrue(result.get().getStderr().isEmpty());
  }

  @Test
  public void when_Exec_YumVersion_On_Centos_6_9_Expect_Execution_To_Succeed() {
    underTest = new DockerContainerClient(IMAGE_CENTOS + ":6.9");
    Optional<ExecResult> result = underTest.exec("yum --version");
    assertTrue(result.isPresent());
    assertEquals(0, result.get().getExitCode());
    assertFalse(result.get().getStdout().isEmpty());
    assertTrue(result.get().getStderr().isEmpty());
  }

  @Test
  public void when_Exec_DockerVersion_On_DockerLatest_Expect_Execution_To_Succeed() {
    underTest = new DockerContainerClient(IMAGE_DOCKER);
    Optional<ExecResult> result = underTest.exec("docker --version");
    assertTrue(result.isPresent());
    assertEquals(0, result.get().getExitCode());
    assertTrue(result.get().getStderr().isEmpty());
    assertThat(result.get().getStdout(), is(containsString("Docker version")));
  }

  @Test(expected = ContainerFetchException.class)
  public void when_Pull_UnknownImage_Expect_Fail() {
    underTest = new DockerContainerClient("unknown-image-" + randomUUID());
    underTest.run();
  }

  @Test
  public void test_Successfully_Bind_Port() {
    String exposedPort = "80";
    DockerContainerConfig containerConfig = DockerContainerConfig.builder(IMAGE_CENTOS)
        .withExposedPort(exposedPort)
        .build();
    underTest = new DockerContainerClient(containerConfig);
    underTest.runAndKeepAlive();
    Integer mappedPort = underTest.getMappedPort(exposedPort);
    assertThat(mappedPort, notNullValue());
  }
}
