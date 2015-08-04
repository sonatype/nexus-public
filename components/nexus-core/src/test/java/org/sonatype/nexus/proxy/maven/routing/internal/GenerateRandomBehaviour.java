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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.util.Map;
import java.util.Random;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.api.Behaviour;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link Behaviour} that generates random count of bytes as response.
 *
 * @author cstamas
 */
public class GenerateRandomBehaviour
    implements Behaviour
{
  private final Random random = new Random();

  private static final byte[] bytes = new byte[1024];

  private final int length;

  /**
   * Constructor.
   *
   * @param length the length of the response in bytes.
   */
  public GenerateRandomBehaviour(final int length) {
    checkArgument(length > 0, "Length must be greater than zero!");
    this.length = length;
  }

  @Override
  public boolean execute(final HttpServletRequest request, final HttpServletResponse response,
                         final Map<Object, Object> ctx)
      throws Exception
  {
    if ("GET".equals(request.getMethod())) {
      response.setContentType("application/octet-stream");
      response.setContentLength(length);

      ServletOutputStream out = response.getOutputStream();
      for (int i = length; i > 0; ) {
        random.nextBytes(bytes);
        int n = Math.min(i, bytes.length);
        i -= n;
        out.write(bytes, 0, n);
      }
      out.close();
      return false;
    }

    return true;
  }

}
