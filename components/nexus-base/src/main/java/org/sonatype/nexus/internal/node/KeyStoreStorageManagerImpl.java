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
package org.sonatype.nexus.internal.node;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;
import org.sonatype.nexus.ssl.spi.KeyStoreStorageManager;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link KeyStoreStorageManager} for the node identity. Uses local filesystem as backing storage so
 * that node identity is specific to each node.
 * 
 * @since 3.1
 */
@Named(KeyStoreManagerImpl.NAME)
@Singleton
public class KeyStoreStorageManagerImpl
    implements KeyStoreStorageManager
{
  private final File basedir;

  @Inject
  public KeyStoreStorageManagerImpl(final ApplicationDirectories directories) {
    this.basedir = new File(directories.getWorkDirectory("keystores"), KeyStoreManagerImpl.NAME);
  }

  @VisibleForTesting
  public KeyStoreStorageManagerImpl(final File basedir) {
    this.basedir = checkNotNull(basedir);
  }

  @Override
  public KeyStoreStorage createStorage(final String keyStoreName) {
    checkNotNull(keyStoreName);
    return new FileKeyStoreStorage(new File(basedir, keyStoreName));
  }
}
