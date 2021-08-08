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
package org.sonatype.nexus.testsuite.testsupport.system.repository;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.GroupRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.HostedRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.ProxyRepositoryConfig;

public abstract class SimpleFormatRepositoryTestSystemSupport
    <HOSTED extends HostedRepositoryConfig<?>,
        PROXY extends ProxyRepositoryConfig<?>,
        GROUP extends GroupRepositoryConfig<?>>
    extends FormatRepositoryTestSystemSupport<HOSTED, PROXY, GROUP>
{
  protected SimpleFormatRepositoryTestSystemSupport(final RepositoryManager repositoryManager) {
    super(repositoryManager);
  }

  public Repository createHosted(final HOSTED config) throws Exception {
    return doCreate(createHostedConfiguration(config));
  }

  public Repository createProxy(final PROXY config) throws Exception {
    return doCreate(createProxyConfiguration(config));
  }

  public Repository createGroup(final GROUP config) throws Exception {
    return doCreate(createGroupConfiguration(config));
  }
}
