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
package org.sonatype.nexus.plugins.lvo.strategy;

import java.io.IOException;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.plugins.lvo.DiscoveryRequest;
import org.sonatype.nexus.plugins.lvo.DiscoveryResponse;
import org.sonatype.nexus.plugins.lvo.DiscoveryStrategy;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.xstream.XStreamUtil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.XppDomDriver;

/**
 * This is a "remote" strategy, uses HTTP GET to get a remote LVO Plugin response. It extends the
 * HttpGetDiscoveryStrategy, and assumes that a GETted content is a LVO Plugin response.
 *
 * @author cstamas
 */
@Singleton
@Named("http-get-lvo")
@Typed(DiscoveryStrategy.class)
public class HttpGetLvoDiscoveryStrategy
    extends AbstractRemoteDiscoveryStrategy
{
  private XStream xstream;

  @Inject
  public HttpGetLvoDiscoveryStrategy(final Hc4Provider hc4Provider) {
    super(hc4Provider);
  }

  protected synchronized XStream getXStream() {
    if (xstream == null) {
      xstream = new XStream(new XppDomDriver());
      XStreamUtil.configure(xstream);
      DiscoveryResponse.configureXStream(xstream);
    }

    return xstream;
  }

  public DiscoveryResponse discoverLatestVersion(DiscoveryRequest request)
      throws NoSuchRepositoryException, IOException
  {
    final DiscoveryResponse dr = new DiscoveryResponse(request);
    // handle
    final RequestResult response = handleRequest(getRemoteUrl(request));
    if (response != null) {
      try {
        final DiscoveryResponse remoteResponse = (DiscoveryResponse) getXStream().fromXML(response.getInputStream());
        return remoteResponse;
      }
      catch (XStreamException e) {
        // handle gracefully, but only XStream problems!
        dr.setSuccessful(false);
        return dr;
      }
      finally {
        response.close();
      }
    }

    return dr;
  }
}
