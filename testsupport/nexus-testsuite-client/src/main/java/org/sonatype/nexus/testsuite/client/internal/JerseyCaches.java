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
import org.sonatype.nexus.testsuite.client.Caches;

/**
 * Jersey implementation of {@link Caches}.
 *
 * @since 2.4
 */
public class JerseyCaches
    extends SubsystemSupport<JerseyNexusClient>
    implements Caches
{
  public JerseyCaches(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public void expireCaches(final String repositoryId) {
    getNexusClient().serviceResource("data_cache/repositories/" + repositoryId + "/content").delete();
  }
}
