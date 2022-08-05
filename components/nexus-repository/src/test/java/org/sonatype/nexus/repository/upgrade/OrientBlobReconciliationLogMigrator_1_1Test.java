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
package org.sonatype.nexus.repository.upgrade;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.io.DirectoryHelper;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.BASEDIR;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.ROOT_KEY;
import static org.sonatype.nexus.repository.upgrade.OrientBlobReconciliationLogMigrator_1_1.ATTRIBUTES;
import static org.sonatype.nexus.repository.upgrade.OrientBlobReconciliationLogMigrator_1_1.BLOBSTORE;
import static org.sonatype.nexus.repository.upgrade.OrientBlobReconciliationLogMigrator_1_1.BLOBSTORE_LOG_PATH;
import static org.sonatype.nexus.repository.upgrade.OrientBlobReconciliationLogMigrator_1_1.RECONCILIATION_DIRECTORY_NAME;

public class OrientBlobReconciliationLogMigrator_1_1Test
    extends TestSupport
{
  private static final String BLOBSTORE_1 = "blobstore-1";

  private static final String BLOBSTORE_2 = "blobstore-2";

  private static final String DB_CLASS_TYPE = "repository_blobstore";

  public static final String NAME = "name";

  private static final String REPOSITORY_BLOBSTORE_CONNECTION = new OClassNameBuilder().type(DB_CLASS_TYPE).build();

  private Path currentReconciliationLogBaseDir;

  @Mock
  private ApplicationDirectories appDirs;

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config");

  @Rule
  public TemporaryFolder sourceReconciliationLogFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder destReconciliationLogFolder = new TemporaryFolder();

  private OrientBlobReconciliationLogMigrator_1_1 underTest;

  @Before
  public void setup() throws Exception {
    currentReconciliationLogBaseDir = Paths.get(sourceReconciliationLogFolder.getRoot().toString(), BLOBSTORE);
    DirectoryHelper.mkdir(currentReconciliationLogBaseDir.toFile());

    underTest = new OrientBlobReconciliationLogMigrator_1_1(appDirs, configDatabase.getInstanceProvider());
  }

  @Test
  public void shouldDoNothingWhenNoReconciliationLogs() throws Exception {
    underTest.apply();

    verify(appDirs).getWorkDirectory(BLOBSTORE_LOG_PATH);
  }

  @Test
  public void shouldNotCopyFilesWhenBlobStoreConfigurationNotFoundInDb() throws Exception {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      createTable(db);
    }

    when(appDirs.getWorkDirectory(BLOBSTORE_LOG_PATH)).thenReturn(currentReconciliationLogBaseDir.toFile());
    createExistingReconciliationLogFiles();

    underTest.apply();

    assertThat(getReconciliationDirectoryPath(destReconciliationLogFolder, BLOBSTORE_1).toFile().listFiles(),
        is(nullValue()));
    assertThat(getReconciliationDirectoryPath(destReconciliationLogFolder, BLOBSTORE_2).toFile().listFiles(),
        is(nullValue()));
  }

  @Test
  public void shouldMoveLogsToAppropriateBlobStore() throws Exception {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      createTable(db);
      createBlobStoreConfiguration(BLOBSTORE_1);
      createBlobStoreConfiguration(BLOBSTORE_2);
    }

    when(appDirs.getWorkDirectory(BLOBSTORE_LOG_PATH)).thenReturn(currentReconciliationLogBaseDir.toFile());
    when(appDirs.getWorkDirectory(BASEDIR)).thenReturn(destReconciliationLogFolder.getRoot());
    createExistingReconciliationLogFiles();

    underTest.apply();

    Path blobStoreReconciliationDirPath = getReconciliationDirectoryPath(destReconciliationLogFolder, BLOBSTORE_1);

    assertThat(blobStoreReconciliationDirPath.toFile().listFiles(), arrayWithSize(3));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-04-07").toFile().exists(), is(true));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-04-08").toFile().exists(), is(true));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-05-21").toFile().exists(), is(true));

    blobStoreReconciliationDirPath = getReconciliationDirectoryPath(destReconciliationLogFolder, BLOBSTORE_2);
    assertThat(blobStoreReconciliationDirPath.toFile().listFiles(), arrayWithSize(4));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-01-08").toFile().exists(), is(true));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-03-18").toFile().exists(), is(true));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-07-10").toFile().exists(), is(true));
    assertThat(blobStoreReconciliationDirPath.resolve("2022-09-21").toFile().exists(), is(true));

    assertThat(currentReconciliationLogBaseDir.toFile().exists(), is(false));
  }

  @Test
  public void shouldNotFailWhenPathMissing() throws Exception {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      createTable(db);
      createBlobStoreConfiguration(BLOBSTORE_1, Collections.emptyMap());
      createBlobStoreConfiguration(BLOBSTORE_2, ImmutableMap.of(CONFIG_KEY, Collections.emptyMap()));
    }

    when(appDirs.getWorkDirectory(BLOBSTORE_LOG_PATH)).thenReturn(currentReconciliationLogBaseDir.toFile());
    when(appDirs.getWorkDirectory(BASEDIR)).thenReturn(destReconciliationLogFolder.getRoot());
    createExistingReconciliationLogFiles();

    underTest.apply();

    assertThat(getReconciliationDirectoryPath(destReconciliationLogFolder, BLOBSTORE_1).toFile().listFiles(),
        is(nullValue()));
    assertThat(getReconciliationDirectoryPath(destReconciliationLogFolder, BLOBSTORE_2).toFile().listFiles(),
        is(nullValue()));
  }

  private void createTable(final ODatabaseDocumentTx db) {
    OSchema schema = db.getMetadata().getSchema();
    OClass replicationConnection = schema.createClass(REPOSITORY_BLOBSTORE_CONNECTION);
    replicationConnection.createProperty(ATTRIBUTES, OType.EMBEDDEDMAP).setMandatory(true).setNotNull(true);
    replicationConnection.createProperty(NAME, OType.STRING).setMandatory(true).setNotNull(true);
  }

  private void createBlobStoreConfiguration(final String blobstoreName) {
    createBlobStoreConfiguration(blobstoreName, aBlobStoreConfiguration(blobstoreName));
  }

  private void createBlobStoreConfiguration(final String blobstoreName, final Map<String, Map<String, Object>> attributes) {
    ODocument blobStoreConfig = new ODocument(REPOSITORY_BLOBSTORE_CONNECTION);
    blobStoreConfig.field(NAME, blobstoreName);
    blobStoreConfig.field(ATTRIBUTES, attributes);
    blobStoreConfig.save();
  }

  private Map<String, Map<String, Object>> aBlobStoreConfiguration(final String blobstoreName) {
    return ImmutableMap.of(CONFIG_KEY, ImmutableMap.of(PATH_KEY, blobstoreName), ROOT_KEY, ImmutableMap.of());
  }

  private Path getReconciliationDirectoryPath(final TemporaryFolder folder, final String blobStoreName) {
    return folder.getRoot().toPath().resolve(Paths.get(blobStoreName, RECONCILIATION_DIRECTORY_NAME));
  }

  private void createExistingReconciliationLogFiles() throws IOException {
    makeBlobStoreDirectory(BLOBSTORE_1);
    makeBlobStoreDirectory(BLOBSTORE_2);
    writeReconciliationLogFile(BLOBSTORE_1, "2022-04-07");
    writeReconciliationLogFile(BLOBSTORE_1, "2022-04-08");
    writeReconciliationLogFile(BLOBSTORE_1, "2022-05-21");
    writeReconciliationLogFile(BLOBSTORE_2, "2022-01-08");
    writeReconciliationLogFile(BLOBSTORE_2, "2022-03-18");
    writeReconciliationLogFile(BLOBSTORE_2, "2022-07-10");
    writeReconciliationLogFile(BLOBSTORE_2, "2022-09-21");
  }

  private void writeReconciliationLogFile(final String blobStoreName, final String fileName) throws IOException {
    String filePath = Paths.get(BLOBSTORE, blobStoreName, fileName).toString();
    String sampleContent = fileName + " 15:00:01,00000000-0000-0000-0000-000000000001";
    File newFile = sourceReconciliationLogFolder.newFile(filePath);
    Files.write(newFile.toPath(), sampleContent.getBytes(StandardCharsets.UTF_8), CREATE);
  }

  private void makeBlobStoreDirectory(final String blobStoreName) throws IOException {
    DirectoryHelper.mkdir(Paths.get(currentReconciliationLogBaseDir.toString(), blobStoreName).toFile());
  }
}
