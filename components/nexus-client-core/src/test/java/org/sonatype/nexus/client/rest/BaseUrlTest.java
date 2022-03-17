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
package org.sonatype.nexus.client.rest;

import java.net.MalformedURLException;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;

public class BaseUrlTest
    extends TestSupport
{

  @Test
  public void simple()
      throws MalformedURLException
  {
    final BaseUrl baseUrl = BaseUrl.baseUrlFrom("http://repository.sonatype.org/");
    Assert.assertEquals(Protocol.HTTP, baseUrl.getProtocol());
    Assert.assertEquals("repository.sonatype.org", baseUrl.getHost());
    Assert.assertEquals(80, baseUrl.getPort());
    Assert.assertEquals("/", baseUrl.getPath());
    Assert.assertEquals("http://repository.sonatype.org:80/", baseUrl.toUrl());
  }

  @Test
  public void simpleNoTrailingSlash()
      throws MalformedURLException
  {
    final BaseUrl baseUrl = BaseUrl.baseUrlFrom("https://repository.sonatype.org");
    Assert.assertEquals(Protocol.HTTPS, baseUrl.getProtocol());
    Assert.assertEquals("repository.sonatype.org", baseUrl.getHost());
    Assert.assertEquals(443, baseUrl.getPort());
    Assert.assertEquals("/", baseUrl.getPath());
    Assert.assertEquals("https://repository.sonatype.org:443/", baseUrl.toUrl());
  }

  @Test
  public void simpleCrazy()
      throws MalformedURLException
  {
    final BaseUrl baseUrl = BaseUrl.baseUrlFrom("https://192.168.0.1:1234/mynexus/instancea");
    Assert.assertEquals(Protocol.HTTPS, baseUrl.getProtocol());
    Assert.assertEquals("192.168.0.1", baseUrl.getHost());
    Assert.assertEquals(1234, baseUrl.getPort());
    Assert.assertEquals("/mynexus/instancea/", baseUrl.getPath());
    Assert.assertEquals("https://192.168.0.1:1234/mynexus/instancea/", baseUrl.toUrl());
  }
}
