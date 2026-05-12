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

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FileKeyStoreStorageFactoryTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File keyStoreDir;

  @Mock
  private ApplicationDirectories appDirs;

  private FileKeyStoreStorageFactory storageManager;

  @Before
  public void setUp() throws Exception {
    keyStoreDir = temporaryFolder.newFolder("keystores");
    when(appDirs.getWorkDirectory("keystores")).thenReturn(keyStoreDir);
    storageManager = new FileKeyStoreStorageFactory(appDirs);
  }

  @Test
  public void testCreateStorage() {
    KeyStoreStorage storage = storageManager.create("test.ks");
    assertThat(storage, is(instanceOf(FileKeyStoreStorage.class)));
    assertThat(((FileKeyStoreStorage) storage).getKeyStoreFile(), is(new File(keyStoreDir, "node/test.ks")));
  }
}
