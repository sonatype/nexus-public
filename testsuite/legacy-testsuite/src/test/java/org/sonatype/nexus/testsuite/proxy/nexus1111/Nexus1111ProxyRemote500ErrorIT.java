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
package org.sonatype.nexus.testsuite.proxy.nexus1111;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.tests.http.server.fluent.Server;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.restlet.data.MediaType;

import static org.sonatype.tests.http.server.fluent.Behaviours.error;

/**
 * @author Juven Xu
 */
public class Nexus1111ProxyRemote500ErrorIT
    extends AbstractNexusProxyIntegrationTest
{

  public Nexus1111ProxyRemote500ErrorIT() {
    super("release-proxy-repo-1");
  }

  @Test
  @Category(PROXY.class)
  public void remote500Error()
      throws Exception
  {
    // first the proxy works
    downloadArtifact("nexus1111", "artifact", "1.0", "jar", null, "target/downloads");

    // stop the healthy server
    ServletServer server = lookup(ServletServer.class);
    server.stop();

    int port = server.getPort();

    // start a server which always return HTTP-500 for get
    Server return500Server = Server.withPort(port).serve("/*").withBehaviours(error(500));

    // download again
    try {
      downloadArtifact("nexus1111", "artifact", "1.1", "jar", null, "target/downloads");
      Assert.fail("Should throw exception coz the remote is in a error status");
    }
    catch (Exception e) {
      // skip
    }

    // This commented stuff below makes IT unpredictable
    // By starting "healthy" server, repo will eventually unblock during ExpireCache task run (more than 20sec)
    // So, I commented this out, we have NFC ITs anyway (that's what following fetch would test)
    // -- cstamas

    // // stop the error server, start the healthy server
    // return500Server.stop();
    // server.start();
    //
    // try
    // {
    // downloadArtifact( "nexus1111", "artifact", "1.1", "jar", null, "target/downloads" );
    // Assert.fail( "Still fails before a clear cache." );
    // }
    // catch ( Exception e )
    // {
    // // skip
    // }

    // clear cache, then download
    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(testRepositoryId);
    TaskScheduleUtil.runTask(ExpireCacheTaskDescriptor.ID, prop);

    try {
      // the proxy is now working <- NOT TRUE, it is auto blocked!
      downloadArtifact("nexus1111", "artifact", "1.1", "jar", null, "target/downloads");
      Assert.fail("Should fail, since repository is in AutoBlock mode!");
    }
    catch (Exception e) {
      // skip
    }

    // check for auto block
    // TODO: interestingly RepositoryMessageUtil.getStatus() neglects JSON here, so
    // not using it and switched back to XML as it is wired in it this util class.
    RepositoryMessageUtil util = new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);

    RepositoryStatusResource status = util.getStatus(this.testRepositoryId);

    Assert.assertEquals("Repository should be auto-blocked", status.getProxyMode(), ProxyMode.BLOCKED_AUTO.name());

    // stop the error server, start the healthy server
    return500Server.stop();
    server.start();

    // unblock it manually
    // NEXUS-4410: since this issue is implemented, the lines below are not enough,
    // since NFC will still contain the artifact do be downloaded, so we need to make it manually blocked and then allow proxy
    // those steps DOES clean NFC
    status.setProxyMode(ProxyMode.BLOCKED_MANUAL.name());
    util.updateStatus(status);
    status.setProxyMode(ProxyMode.ALLOW.name());
    util.updateStatus(status);

    // and now, all should go well
    downloadArtifact("nexus1111", "artifact", "1.1", "jar", null, "target/downloads");
  }
}
