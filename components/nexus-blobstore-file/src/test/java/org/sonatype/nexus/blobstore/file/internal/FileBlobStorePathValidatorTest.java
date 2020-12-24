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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.rest.ValidationErrorsException;
import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.BASEDIR;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;

public class FileBlobStorePathValidatorTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private ApplicationDirectories applicationDirectories;

  List<BlobStore> blobStores;

  private FileBlobStorePathValidator underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new FileBlobStorePathValidator(() -> blobStoreManager, () -> applicationDirectories);

    File appDir = Mockito.mock(File.class);
    when(applicationDirectories.getWorkDirectory(BASEDIR)).thenReturn(appDir);
    Path path = Mockito.mock(Path.class);
    when(path.toRealPath()).thenReturn(path);
    when(path.normalize()).thenReturn(Paths.get("/nexus"));
    when(appDir.toPath()).thenReturn(path);

    blobStores = generateBlobStores();
    when(blobStoreManager.browse()).thenReturn(blobStores);
  }

  @Test
  public void checkHealthyIfPathNotDuplicate() throws Exception {
    assertThat(underTest.check().isHealthy(), is(true));
  }

  @Test
  public void checkUnhealthyIfPathDuplicate() throws Exception {
    blobStores.add(generateBlobStore(FileBlobStore.TYPE, "f_d", "/nexus/0"));
    assertThat(underTest.check().isHealthy(), is(false));
  }

  @Test
  public void checkUnhealthyIfNestedPathDuplicate() throws Exception {
    blobStores.add(generateBlobStore(FileBlobStore.TYPE, "f_d", "/nexus/0/1"));
    Result result = underTest.check();
    assertThat(result.getMessage().contains("f_d"), is(true));
    assertThat(result.getMessage().contains("f0"), is(true));
    assertThat(result.isHealthy(), is(false));
  }

  @Test
  public void checkHealthyIfPathDuplicateInNorFileBlobStore() throws Exception {
    blobStores.add(generateBlobStore("other_type", "f_d", "/nexus/0"));
    assertThat(underTest.check().isHealthy(), is(true));
  }

  @Test
  public void notThrowsIfPathIsUnique() {
    BlobStoreConfiguration configuration =
        generateBlobStore(FileBlobStore.TYPE, "f_d", "/nexus/unique").getBlobStoreConfiguration();
    underTest.validatePathUniqueConstraint(configuration);
  }

  @Test(expected = ValidationErrorsException.class)
  public void throwsIfPathIsNotUnique() {
    BlobStoreConfiguration configuration =
        generateBlobStore(FileBlobStore.TYPE, "f_d", "/nexus/0").getBlobStoreConfiguration();
    underTest.validatePathUniqueConstraint(configuration);
  }

  @Test
  public void notThrowsIfPathIsNotUniqueInOtherBlobStoreType() {
    BlobStoreConfiguration configuration =
        generateBlobStore(FileBlobStore.TYPE, "f_d", "/nexus/10").getBlobStoreConfiguration();
    underTest.validatePathUniqueConstraint(configuration);
  }

  @Test(expected = ValidationErrorsException.class)
  public void throwsIfNotAbsolutePathIsNotUnique() {
    BlobStoreConfiguration configuration =
        generateBlobStore(FileBlobStore.TYPE, "f_d", "0").getBlobStoreConfiguration();
    underTest.validatePathUniqueConstraint(configuration);
  }

  private List<BlobStore> generateBlobStores() {
    blobStores = new ArrayList<>();

    blobStores.add(generateBlobStore("other_type", "s1", "/nexus/0"));
    blobStores.add(generateBlobStore("other_type", "s2", "/nexus/1"));
    blobStores.add(generateBlobStore("other_type", "s3", "/nexus/10"));

    for (int i = 0; i < 3; i++) {
      blobStores.add(generateBlobStore(FileBlobStore.TYPE, "f" + i, "/nexus/" + i));
    }

    blobStores.add(generateBlobStore(FileBlobStore.TYPE, "f00", "/nexus/00"));

    return blobStores;
  }

  private BlobStore generateBlobStore(final String type, final String name, final String path) {
    BlobStoreConfiguration configuration = Mockito.mock(BlobStoreConfiguration.class);
    when(configuration.getType()).thenReturn(type);
    when(configuration.getName()).thenReturn(name);

    Map<String, Object> map = new HashMap<>();
    map.put(PATH_KEY, path);
    NestedAttributesMap attributesMap = new NestedAttributesMap(CONFIG_KEY, map);
    when(configuration.attributes(CONFIG_KEY)).thenReturn(attributesMap);

    BlobStore blobStore = Mockito.mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    return blobStore;
  }
}
