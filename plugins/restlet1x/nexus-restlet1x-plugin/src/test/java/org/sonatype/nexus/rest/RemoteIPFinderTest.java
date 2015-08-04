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
package org.sonatype.nexus.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.Request;

@SuppressWarnings("unchecked")
public class RemoteIPFinderTest
{
  @Test
  @Ignore("Fails when not running on VPN; ignoring for now")
  public void testResolveIP() {
    Request restlet = Mockito.mock(Request.class);
    Map<String, Object> attributes = Mockito.mock(Map.class);
    Form form = Mockito.mock(Form.class);
    ClientInfo clientInfo = new ClientInfo();

    Mockito.doReturn(attributes).when(restlet).getAttributes();
    Mockito.doReturn(form).when(attributes).get("org.restlet.http.headers");
    Mockito.doReturn(clientInfo).when(restlet).getClientInfo();

    Mockito.doReturn("").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals(null, RemoteIPFinder.findIP(restlet));

    Mockito.doReturn(null).when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals(null, RemoteIPFinder.findIP(restlet));

    Mockito.doReturn("127.0.0.1").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals("127.0.0.1", RemoteIPFinder.findIP(restlet));

    Mockito.doReturn("missing, 127.0.0.2, unknown, 127.0.0.1").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals("127.0.0.2", RemoteIPFinder.findIP(restlet));

    Mockito.doReturn("127.0.0.3, 127.0.0.2, 127.0.0.1").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals("127.0.0.3", RemoteIPFinder.findIP(restlet));

    Mockito.doReturn("localhost").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals("localhost", RemoteIPFinder.findIP(restlet));

    clientInfo.setAddresses(null);
    Mockito.doReturn("client, proxy1, proxy2").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals(null, RemoteIPFinder.findIP(restlet));

    clientInfo.setAddresses(Collections.<String>emptyList());
    Mockito.doReturn("client, proxy1, proxy2").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals(null, RemoteIPFinder.findIP(restlet));

    // restlet1x clientInfo addresses are the last known upstream address + reverse of XFF
    clientInfo.setAddresses(Arrays.asList("upstream", "proxy2", "proxy1", "client"));
    Mockito.doReturn("client, proxy1, proxy2").when(form).getFirstValue(org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER);
    Assert.assertEquals("upstream", RemoteIPFinder.findIP(restlet));
  }
}
