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
package org.sonatype.nexus.proxy.maven.routing.internal.scrape;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.api.Behaviour;

/**
 * Behavior that "mimics" S3 response by placing junk S3 headers. Works when only their presence is checked, like in
 * case of S3 scraper.
 *
 * @author cstamas
 */
public class S3ResponseHeadersBehaviour
    implements Behaviour
{
  @Override
  public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
      throws Exception
  {
    response.addHeader("Server", "AmazonS3");
    response.addHeader("x-amz-request-id", "1234567890");
    return true;
  }
}
