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
package org.sonatype.nexus.testsuite.client.internal;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.client.UIDLocks;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

import static java.lang.String.format;

/**
 * Jersey based {@link UIDLocks} Nexus Client Subsystem implementation.
 *
 * @since 2.2
 */
public class JerseyUIDLocks
    extends SubsystemSupport<JerseyNexusClient>
    implements UIDLocks
{

  public JerseyUIDLocks(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public void lock(final String repository, final String path, final LockType lockType) {
    final String uri = format("nexus-it-helper-plugin/uid/lock/%s/%s/%s", repository, lockType.name(), path);
    try {
      getNexusClient()
          .serviceResource(uri)
          .get(ClientResponse.class)
          .close();
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public void unlock(final String repository, final String path) {
    final String uri = format("nexus-it-helper-plugin/uid/lock/%s/unlock/%s", repository, path);
    try {
      getNexusClient()
          .serviceResource(uri)
          .delete();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

}
