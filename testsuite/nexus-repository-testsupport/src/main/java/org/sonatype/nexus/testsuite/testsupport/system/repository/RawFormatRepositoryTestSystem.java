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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.RawGroupRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.RawHostedRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.RawProxyRepositoryConfig;

import static org.sonatype.nexus.testsuite.testsupport.system.RepositoryTestSystem.FORMAT_RAW;

@Named(FORMAT_RAW)
@Singleton
public class RawFormatRepositoryTestSystem
    extends SimpleFormatRepositoryTestSystemSupport
                <RawHostedRepositoryConfig,
                    RawProxyRepositoryConfig,
                    RawGroupRepositoryConfig>
    implements FormatRepositoryTestSystem
{
  @Inject
  public RawFormatRepositoryTestSystem(final RepositoryManager repositoryManager) {
    super(repositoryManager, RawHostedRepositoryConfig.class, RawProxyRepositoryConfig.class,
        RawGroupRepositoryConfig.class);
  }
}
