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
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.file.FileBlobMetadata;
import org.sonatype.nexus.blobstore.file.FileBlobMetadataStore;
import org.sonatype.nexus.blobstore.file.FileBlobState;
import org.sonatype.nexus.common.collect.AutoClosableIterable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;

/**
 * Tests for {@link FileBlobMetadataStoreImpl}.
 */
public class FileBlobMetadataStoreImplTest
  extends TestSupport
{
  private FileBlobMetadataStore underTest;

  @Before
  public void setUp() throws Exception {
    File root = util.createTempDir("databases");
    File dir = new File(root, "test");
    this.underTest = FileBlobMetadataStoreImpl.create(dir);
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
  }

  /**
   * Helper to find states and close iterable.
   */
  private Iterable<BlobId> findWithState(final FileBlobState state) throws Exception {
    List<BlobId> results = Lists.newArrayList();
    try (AutoClosableIterable<BlobId> iter = underTest.findWithState(state)) {
      for (BlobId id : iter) {
        results.add(id);
      }
    }
    return results;
  }

  @Test
  public void stateTracking() throws Exception {
    FileBlobMetadata md = new FileBlobMetadata(FileBlobState.CREATING, ImmutableMap.of("foo", "bar"));
    BlobId id = underTest.add(md);
    log("Added: {} -> {}", id, md);

    // states should only contain CREATING
    assertThat(findWithState(FileBlobState.CREATING), contains(id));
    assertThat(findWithState(FileBlobState.ALIVE), emptyIterable());
    assertThat(findWithState(FileBlobState.MARKED_FOR_DELETION), emptyIterable());

    md.setBlobState(FileBlobState.ALIVE);
    underTest.update(id, md);
    log("Updated: {} -> {}", id, md);

    // states should only contain ALIVE
    assertThat(findWithState(FileBlobState.CREATING), emptyIterable());
    assertThat(findWithState(FileBlobState.ALIVE), contains(id));
    assertThat(findWithState(FileBlobState.MARKED_FOR_DELETION), emptyIterable());

    md.setBlobState(FileBlobState.MARKED_FOR_DELETION);
    underTest.update(id, md);
    log("Updated: {} -> {}", id, md);

    // states should only contain marked for MARKED_FOR_DELETION
    assertThat(findWithState(FileBlobState.CREATING), emptyIterable());
    assertThat(findWithState(FileBlobState.ALIVE), emptyIterable());
    assertThat(findWithState(FileBlobState.MARKED_FOR_DELETION), contains(id));

    underTest.delete(id);
    log("Deleted: {}", id);

    // states all be empty
    assertThat(findWithState(FileBlobState.CREATING), emptyIterable());
    assertThat(findWithState(FileBlobState.ALIVE), emptyIterable());
    assertThat(findWithState(FileBlobState.MARKED_FOR_DELETION), emptyIterable());
  }

  @Test
  public void basic() throws Exception {
    FileBlobMetadata md = new FileBlobMetadata(FileBlobState.CREATING, ImmutableMap.of("foo", "bar"));
    BlobMetrics test = new BlobMetrics(new DateTime(), "test", 1l);
    md.setMetrics(test);
    log(md);

    // add a record
    log("add");
    BlobId id = underTest.add(md);
    log(id);

    assertThat(underTest.getBlobSize(), is(1l));
    assertThat("Total should be size of metadata + size of blobs",
        underTest.getTotalSize() - underTest.getMetadataSize(), is(1l));

    dumpStates();

    // update a record
    log("update");
    md.setBlobState(FileBlobState.ALIVE);
    underTest.update(id, md);

    dumpStates();

    // fetch a record
    log("fetch");
    FileBlobMetadata md2 = underTest.get(id);
    log(md2);

    // delete a record
    log("delete");
    underTest.delete(id);

    dumpStates();

    // compact
    log("compact");
    underTest.compact();
  }

  private void dumpStates() throws Exception {
    for (FileBlobState state : FileBlobState.values()) {
      log(state);
      for (BlobId foundId : findWithState(state)) {
        log("  {}", foundId);
      }
    }
  }
}
