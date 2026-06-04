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
package org.sonatype.nexus.self.hosted.node;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.ssl.KeyStoreStorageFactory;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link KeyStoreStorageFactory} for the node identity. Uses local filesystem as backing storage so
 * that node identity is specific to each node.
 *
 * @since 3.1
 */
@Component
@Qualifier(NodeKeyStoreManagerImpl.NAME)
public class FileKeyStoreStorageFactory
    implements KeyStoreStorageFactory
{
  private final File basedir;

  @Autowired
  public FileKeyStoreStorageFactory(final ApplicationDirectories directories) {
    this.basedir = new File(directories.getWorkDirectory("keystores"), NodeKeyStoreManagerImpl.NAME);
  }

  @Override
  public KeyStoreStorage create(final String keyStoreName) {
    checkNotNull(keyStoreName);
    return new FileKeyStoreStorage(new File(basedir, keyStoreName));
  }
}
