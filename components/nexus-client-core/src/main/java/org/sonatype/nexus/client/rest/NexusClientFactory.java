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

import org.sonatype.nexus.client.core.NexusClient;

/**
 * @since 2.1
 */
public interface NexusClientFactory
{

  /**
   * Creates a {@link NexusClient} against given baseUrl.
   */
  NexusClient createFor(BaseUrl baseUrl);

  /**
   * Creates a {@link NexusClient} against given baseUrl and authentication info (Nexus authentication).
   */
  NexusClient createFor(BaseUrl baseUrl, AuthenticationInfo authenticationInfo);

  /**
   * Creates a {@link NexusClient} against given data in the provided {@link ConnectionInfo}.
   */
  NexusClient createFor(ConnectionInfo connectionInfo);

}
