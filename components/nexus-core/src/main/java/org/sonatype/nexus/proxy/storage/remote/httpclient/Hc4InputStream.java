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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.proxy.RemoteStorageEOFException;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.util.WrappingInputStream;

import org.apache.http.ConnectionClosedException;

/**
 * Simple wrapper input stream implementation that translates some HC4 specific exceptions to Nexus Core specific
 * exceptions, making Core able to properly respond to them.
 *
 * @since 2.4
 */
class Hc4InputStream
    extends WrappingInputStream
{
  private final ProxyRepository proxyRepository;

  public Hc4InputStream(final ProxyRepository proxyRepository, final InputStream stream) {
    super(stream);
    this.proxyRepository = proxyRepository;
  }

  @Override
  public int read()
      throws IOException
  {
    try {
      return super.read();
    }
    catch (ConnectionClosedException e) {
      throw new RemoteStorageEOFException(proxyRepository, e);
    }
  }

  @Override
  public int read(byte[] b)
      throws IOException
  {
    try {
      return super.read(b);
    }
    catch (ConnectionClosedException e) {
      throw new RemoteStorageEOFException(proxyRepository, e);
    }
  }

  @Override
  public int read(byte b[], int off, int len)
      throws IOException
  {
    try {
      return super.read(b, off, len);
    }
    catch (ConnectionClosedException e) {
      throw new RemoteStorageEOFException(proxyRepository, e);
    }
  }
}
