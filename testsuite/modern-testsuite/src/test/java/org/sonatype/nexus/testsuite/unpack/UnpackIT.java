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
package org.sonatype.nexus.testsuite.unpack;

import org.sonatype.nexus.client.core.exception.NexusClientAccessForbiddenException;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.security.User;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @since 2.5.1
 */
public class UnpackIT
    extends UnpackITSupport
{

  private static final boolean EXISTS = true;

  private static final boolean DOES_NOT_EXIST = false;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public UnpackIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Verify that content of a zip is unpacked into repository.
   */
  @Test
  public void upload()
      throws Exception
  {
    final MavenHostedRepository repository = repositories().create(
        MavenHostedRepository.class, repositoryIdForTest()
    ).save();

    upload(
        client(),
        repository.id(),
        testData().resolveFile("bundle.zip"),
        null,
        false
    );

    assertFilesPresentOnStorage(
        repository.id(), EXISTS,
        "nxcm1312/artifact/2.0/artifact-2.0.jar",
        "org/nxcm1312/maven-deploy-released/1.0/maven-deploy-released-1.0.jar",
        "org/nxcm1312/maven-mixed-released/1.0/maven-mixed-released-1.0.jar"
    );
  }

  /**
   * Verify that content of a zip is unpacked into repository under the specified path.
   */
  @Test
  public void uploadWithPath()
      throws Exception
  {
    final MavenHostedRepository repository = repositories().create(
        MavenHostedRepository.class, repositoryIdForTest()
    ).save();

    upload(
        client(),
        repository.id(),
        testData().resolveFile("bundle.zip"),
        "foo/bar",
        false
    );

    assertFilesPresentOnStorage(
        repository.id(), EXISTS,
        "foo/bar/nxcm1312/artifact/2.0/artifact-2.0.jar",
        "foo/bar/org/nxcm1312/maven-deploy-released/1.0/maven-deploy-released-1.0.jar",
        "foo/bar/org/nxcm1312/maven-mixed-released/1.0/maven-mixed-released-1.0.jar"
    );
  }

  /**
   * 1. upload without delete flag, it should succeed
   * 2. then validate that upload happened okay
   * 3. then we upload bundle1.zip (it does not contains nxcm1312 root dir) without delete flag
   * 4. then validate that upload happened okay, but root directory nxcm1312 is still in place
   * 5. then we upload bundle1.zip again, this time with delete flag
   * 6. then we validate that upload happened okay, and root directory nxcm1312 is deleted
   */
  @Test
  public void uploadWithDelete()
      throws Exception
  {
    final MavenHostedRepository repository = repositories().create(
        MavenHostedRepository.class, repositoryIdForTest()
    ).allowRedeploy().save();

    upload(
        client(),
        repository.id(),
        testData().resolveFile("bundle.zip"),
        "foo",
        false
    );

    assertFilesPresentOnStorage(
        repository.id(), EXISTS,
        "foo/nxcm1312/artifact/2.0/artifact-2.0.jar",
        "foo/org/nxcm1312/maven-deploy-released/1.0/maven-deploy-released-1.0.jar",
        "foo/org/nxcm1312/maven-mixed-released/1.0/maven-mixed-released-1.0.jar"
    );

    upload(
        client(),
        repository.id(),
        testData().resolveFile("bundle1.zip"),
        "foo",
        false
    );

    assertFilesPresentOnStorage(
        repository.id(), EXISTS,
        "foo/nxcm1312/artifact/2.0/artifact-2.0.jar",
        "foo/org/nxcm1312/maven-deploy-released/1.0/maven-deploy-released-1.0.jar",
        "foo/org/nxcm1312/maven-mixed-released/1.0/maven-mixed-released-1.0.jar"
    );

    upload(
        client(),
        repository.id(),
        testData().resolveFile("bundle1.zip"),
        "foo",
        true
    );

    assertFilesPresentOnStorage(
        repository.id(), DOES_NOT_EXIST,
        "foo/nxcm1312/artifact/2.0/artifact-2.0.jar"
    );
    assertFilesPresentOnStorage(
        repository.id(), EXISTS,
        "foo/org/nxcm1312/maven-deploy-released/1.0/maven-deploy-released-1.0.jar",
        "foo/org/nxcm1312/maven-mixed-released/1.0/maven-mixed-released-1.0.jar"
    );
  }

  /**
   * Verify content of a zip file uploaded via Maven.
   */
  @Test
  public void uploadViaMaven()
      throws Exception
  {
    final MavenHostedRepository repository = repositories().create(
        MavenHostedRepository.class, repositoryIdForTest()
    ).save();

    executeMaven("upload", repository.id(), "install");

    assertFilesPresentOnStorage(
        repository.id(), EXISTS,
        "foo/bar/b.bin",
        "foo/bar/x/a.txt",
        "foo/bar/META-INF/MANIFEST.MF",
        "foo/bar/META-INF/maven/org.sonatype.nexus.unpack/upload/pom.properties",
        "foo/bar/META-INF/maven/org.sonatype.nexus.unpack/upload/pom.xml"
    );
  }

  /**
   * Verify that uploading a zip, using a user that does not have the "unpack" role (privilege "content-compressed"),
   * will fail with 403.
   */
  @Test
  public void uploadUsingUserWithoutUnpackPrivilege()
      throws Exception
  {
    final User user = createUser();

    final MavenHostedRepository repository = repositories().create(
        MavenHostedRepository.class, repositoryIdForTest()
    ).save();

    thrown.expect(NexusClientAccessForbiddenException.class);
    upload(
        createNexusClient(nexus(), user.id(), PASSWORD),
        repository.id(),
        testData().resolveFile("bundle.zip"),
        null,
        false
    );
  }

  /**
   * Verify that uploading a zip, using a user that has the "unpack" role (privilege "content-compressed"),
   * will succeed.
   */
  @Test
  public void uploadUsingUserWithUnpackPrivilege()
      throws Exception
  {
    final User user = createUser().withRole("unpack").save();

    final MavenHostedRepository repository = repositories().create(
        MavenHostedRepository.class, repositoryIdForTest()
    ).save();

    upload(
        createNexusClient(nexus(), user.id(), PASSWORD),
        repository.id(),
        testData().resolveFile("bundle.zip"),
        null,
        false
    );
  }

}
