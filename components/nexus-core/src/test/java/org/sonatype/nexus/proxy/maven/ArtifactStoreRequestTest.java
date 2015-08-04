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
package org.sonatype.nexus.proxy.maven;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.maven.gav.Gav;

import org.junit.Assert;
import org.junit.Test;

public class ArtifactStoreRequestTest
    extends AbstractProxyTestEnvironment
{

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    return new M2TestsuiteEnvironmentBuilder(ss);
  }

  @Test
  public void testNoDots() throws Exception {
    Gav gav = new Gav("nodots", "artifact", "1.0", null, "xml", null, null, null, false, null, false, null);
    MavenRepository mavenRepository = (MavenRepository) this.getRepositoryRegistry().getRepository("repo1");
    ArtifactStoreRequest request = new ArtifactStoreRequest(mavenRepository, gav, true, false);

    Assert.assertEquals("/nodots/artifact/1.0/artifact-1.0.xml", request.getRequestPath());
  }

  @Test
  public void testDots() throws Exception {
    Gav gav = new Gav("a.bunch.of.dots.yeah", "artifact", "1.0", null, "xml", null, null, null, false, null, false,
        null);
    MavenRepository mavenRepository = (MavenRepository) this.getRepositoryRegistry().getRepository("repo1");
    ArtifactStoreRequest request = new ArtifactStoreRequest(mavenRepository, gav, true, false);

    Assert.assertEquals("/a/bunch/of/dots/yeah/artifact/1.0/artifact-1.0.xml", request.getRequestPath());
  }

  // undefined extra dot
  //    @Test
  //    public void testExtraDot() throws Exception
  //    {
  //        Gav gav = new Gav("extra..dot", "artifact", "1.0", null, "xml", null, null, null, false, false, null, false, null);
  //        MavenRepository mavenRepository = (MavenRepository) this.getRepositoryRegistry().getRepository( "repo1" );
  //        ArtifactStoreRequest request = new ArtifactStoreRequest( mavenRepository, gav, true );
  //
  //        Assert.assertEquals( "/extra/dot/artifact/1.0/artifact-1.0.xml", request.getRequestPath() );
  //    }

  @Test
  public void testGroupStartsWithDot() throws Exception {
    Gav gav = new Gav(".meta/foo/bar", "artifact", "1.0", null, "xml", null, null, null, false, null, false, null);
    MavenRepository mavenRepository = (MavenRepository) this.getRepositoryRegistry().getRepository("repo1");
    ArtifactStoreRequest request = new ArtifactStoreRequest(mavenRepository, gav, true, false);

    Assert.assertEquals("/.meta/foo/bar/artifact/1.0/artifact-1.0.xml", request.getRequestPath());
  }


}
