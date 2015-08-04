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
package org.sonatype.nexus.testsuite.repo.nexus3709;

import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus3709FileTypeValidationIT
    extends AbstractNexusProxyIntegrationTest
{

  public Nexus3709FileTypeValidationIT() {
    super("nexus3709");
  }

  @Test
  public void testGoodZip()
      throws Exception
  {
    String relativePath =
        this.getRelitiveArtifactPath(new Gav("nexus3709.foo.bar", "goodzip", "1.0.0", null, "zip", null, null,
            null, false, null, false, null));
    String url = this.getRepositoryUrl(this.getTestRepositoryId()) + relativePath;
    Response response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    Assert.assertEquals(200, response.getStatus().getCode());
  }

  @Test
  public void testBadZip()
      throws Exception
  {
    String relativePath =
        this.getRelitiveArtifactPath(new Gav("nexus3709.foo.bar", "badzip", "1.0.0", null, "zip", null, null,
            null, false, null, false, null));
    String url = this.getRepositoryUrl(this.getTestRepositoryId()) + relativePath;
    Response response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    Assert.assertEquals(404, response.getStatus().getCode());
  }

  @Test
  public void testGoodJar()
      throws Exception
  {
    String relativePath =
        this.getRelitiveArtifactPath(new Gav("nexus3709.foo.bar", "goodjar", "1.0.0", null, "jar", null, null,
            null, false, null, false, null));
    String url = this.getRepositoryUrl(this.getTestRepositoryId()) + relativePath;
    Response response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    Assert.assertEquals(200, response.getStatus().getCode());
  }

  @Test
  public void testBadJar()
      throws Exception
  {
    String relativePath =
        this.getRelitiveArtifactPath(new Gav("nexus3709.foo.bar", "badjar", "1.0.0", null, "jar", null, null,
            null, false, null, false, null));
    String url = this.getRepositoryUrl(this.getTestRepositoryId()) + relativePath;
    Response response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    Assert.assertEquals(404, response.getStatus().getCode());
  }

  @Test
  public void testGoodPom()
      throws Exception
  {
    String relativePath =
        this.getRelitiveArtifactPath(new Gav("nexus3709.foo.bar", "goodpom", "1.0.0", null, "pom", null, null,
            null, false, null, false, null));
    String url = this.getRepositoryUrl(this.getTestRepositoryId()) + relativePath;
    Response response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    Assert.assertEquals(200, response.getStatus().getCode());
  }

  @Test
  public void testBadPom()
      throws Exception
  {
    String relativePath =
        this.getRelitiveArtifactPath(new Gav("nexus3709.foo.bar", "badpom", "1.0.0", null, "pom", null, null,
            null, false, null, false, null));
    String url = this.getRepositoryUrl(this.getTestRepositoryId()) + relativePath;
    Response response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    Assert.assertEquals(404, response.getStatus().getCode());
  }

}
