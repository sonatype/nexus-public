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
package org.sonatype.nexus.testsuite.testsupport.raw;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.orient.raw.RawContentFacet;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.io.Files;
import org.apache.http.entity.ContentType;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

/**
 * Support class for raw ITs.
 */
@Category(RawTestGroup.class)
public class RawITSupport
    extends RepositoryITSupport
{
  @Inject
  protected LogManager logManager;

  public RawITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/raw"));
  }

  protected Content read(final Repository repository, final String path) throws IOException {
    RawContentFacet rawFacet = repository.facet(RawContentFacet.class);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      return rawFacet.get(path);
    }
    finally {
      UnitOfWork.end();
    }
  }

  protected void assertReadable(final Repository repository, final String... paths) throws IOException {
    for (String path : paths) {
      assertThat(path, read(repository, path), notNullValue());
    }
  }

  protected void assertNotReadable(final Repository repository, final String... paths) throws IOException {
    for (String path : paths) {
      assertThat(path, read(repository, path), nullValue());
    }
  }

  protected void uploadAndDownload(RawClient rawClient, String file) throws Exception {
    final File testFile = resolveTestFile(file);
    final int response = rawClient.put(file, ContentType.TEXT_PLAIN, testFile);
    assertThat(response, is(HttpStatus.CREATED));

    assertThat(bytes(rawClient.get(file)), is(Files.toByteArray(testFile)));

    assertThat(status(rawClient.delete(file)), is(HttpStatus.NO_CONTENT));

    assertThat("content should be deleted", status(rawClient.get(file)), is(HttpStatus.NOT_FOUND));
  }
}
