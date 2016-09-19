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
package org.sonatype.nexus.upgrade.internal;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class ModelVersionStoreTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private UpgradeManager upgradeManager;

  @Mock
  private ApplicationDirectories appDirs;

  private ClusteredModelVersionsEntityAdapter entityAdapter;

  private ModelVersionStore store;

  private File dbFolder;

  @Before
  public void setUp() throws Exception {
    dbFolder = util.createTempDir();
    when(appDirs.getWorkDirectory("db")).thenReturn(dbFolder);
    when(upgradeManager.getLocalModels()).thenReturn(Collections.singleton("local"));
    when(upgradeManager.getClusteredModels()).thenReturn(Collections.singleton("clustered"));
    entityAdapter = new ClusteredModelVersionsEntityAdapter();
    store = new ModelVersionStore(upgradeManager, database.getInstanceProvider(), entityAdapter, appDirs);
  }

  @Test
  public void testLoad_PristineInstallation() throws Exception {
    store.start();
    Map<String, String> versions = store.load();
    assertThat(versions.entrySet(), hasSize(0));
  }

  @Test
  public void testLoad_ExistingInstallation() throws Exception {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ClusteredModelVersions versions = new ClusteredModelVersions();
      versions.put("clustered", "1.2");
      versions.put("local", "99");
      entityAdapter.singleton.set(db, versions);
    }
    PropertiesFile modelProperties = new PropertiesFile(new File(dbFolder, ModelVersionStore.MODEL_PROPERTIES));
    modelProperties.put("clustered", "99");
    modelProperties.put("local", "2.1");
    modelProperties.store();
    store.start();
    Map<String, String> versions = store.load();
    assertThat(versions, hasEntry("clustered", "1.2"));
    assertThat(versions, hasEntry("local", "2.1"));
    assertThat(versions.entrySet(), hasSize(2));
  }

  @Test
  public void testSave() throws Exception {
    store.start();
    store.save(ImmutableMap.of("clustered", "1.2", "local", "2.1"));
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ClusteredModelVersions versions = entityAdapter.singleton.get(db);
      assertThat(versions.getModelVersions(), hasEntry("clustered", "1.2"));
      assertThat(versions.getModelVersions().entrySet(), hasSize(1));
    }
    PropertiesFile modelProperties = new PropertiesFile(new File(dbFolder, ModelVersionStore.MODEL_PROPERTIES));
    assertThat(modelProperties.getFile().isFile(), is(true));
    modelProperties.load();
    assertThat(modelProperties, hasEntry("local", "2.1"));
    assertThat(modelProperties.entrySet(), hasSize(1));
  }
}
