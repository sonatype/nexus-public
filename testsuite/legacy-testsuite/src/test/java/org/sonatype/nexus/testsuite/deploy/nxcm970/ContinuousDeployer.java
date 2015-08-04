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
package org.sonatype.nexus.testsuite.deploy.nxcm970;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class ContinuousDeployer
    implements Runnable
{
  private final HttpClient httpClient;

  private volatile boolean deploying;

  private final String targetUrl;

  private int result = -1;

  public ContinuousDeployer(final String targetUrl) {
    super();
    this.targetUrl = targetUrl;
    this.deploying = true;
    this.httpClient = new DefaultHttpClient();
  }

  public boolean isDeploying() {
    return deploying;
  }

  public void finishDeploying() {
    this.deploying = false;
  }

  public boolean isFinished() {
    return result != -1;
  }

  public int getResult() {
    return result;
  }

  public void run() {
    final HttpPut method = new HttpPut(targetUrl);
    method.setEntity(new InputStreamEntity(new EndlessBlockingInputStream(this), -1));

    try {
      result = httpClient.execute(method).getStatusLine().getStatusCode();
    }
    catch (Exception e) {
      result = -2;
      e.printStackTrace();
    }
  }

  /**
   * This is an endless stream, that will sleep a little and then serve the 'T' character.
   *
   * @author cstamas
   */
  public static class EndlessBlockingInputStream
      extends InputStream
  {
    private final ContinuousDeployer continuousDeployer;

    public EndlessBlockingInputStream(final ContinuousDeployer deployer) {
      this.continuousDeployer = deployer;
    }

    @Override
    public int read()
        throws IOException
    {
      if (continuousDeployer.isDeploying()) {
        try {
          Thread.sleep(300);
          return 'T';
        }
        catch (InterruptedException e) {
          throw new IOException(e.getMessage());
        }
      }
      else {
        // finish
        return -1;
      }
    }
  }
}
