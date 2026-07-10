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
package org.sonatype.nexus.blobstore.api.metrics;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link BlobStoreMetricsEntity}.
 */
public class BlobStoreMetricsEntityTest
{
  private BlobStoreMetricsEntity underTest;

  @Before
  public void setUp() {
    underTest = new BlobStoreMetricsEntity();
  }

  @Test
  public void testDefaultValues() {
    assertThat(underTest.getBlobStoreName(), is(nullValue()));
    assertThat(underTest.getTotalSize(), is(0L));
    assertThat(underTest.getBlobCount(), is(0L));
    assertThat(underTest.getUploadBlobSize(), is(0L));
    assertThat(underTest.getUploadSuccessfulRequests(), is(0L));
    assertThat(underTest.getUploadTimeOnRequests(), is(0L));
    assertThat(underTest.getUploadErrorRequests(), is(0L));
    assertThat(underTest.getDownloadBlobSize(), is(0L));
    assertThat(underTest.getDownloadSuccessfulRequests(), is(0L));
    assertThat(underTest.getDownloadTimeOnRequests(), is(0L));
    assertThat(underTest.getDownloadErrorRequests(), is(0L));
  }

  @Test
  public void testSetBlobStoreName() {
    assertThat(underTest.setBlobStoreName("my-store"), is(sameInstance(underTest)));
    assertThat(underTest.getBlobStoreName(), is("my-store"));
    // setting blobStoreName must not bleed into any other field
    assertAllFieldsDefaultExcept("blobStoreName");
  }

  @Test
  public void testSetBlobStoreNameAcceptsNull() {
    underTest.setBlobStoreName("temp");

    assertThat(underTest.setBlobStoreName(null), is(sameInstance(underTest)));
    assertThat(underTest.getBlobStoreName(), is(nullValue()));
  }

  @Test
  public void testSetBlobStoreNameAcceptsEmptyString() {
    assertThat(underTest.setBlobStoreName(""), is(sameInstance(underTest)));
    assertThat(underTest.getBlobStoreName(), is(""));
    assertAllFieldsDefaultExcept("blobStoreName");
  }

  @Test
  public void testSetTotalSize() {
    assertThat(underTest.setTotalSize(1234L), is(sameInstance(underTest)));
    assertThat(underTest.getTotalSize(), is(1234L));
    assertAllFieldsDefaultExcept("totalSize");
  }

  @Test
  public void testSetBlobCount() {
    assertThat(underTest.setBlobCount(42L), is(sameInstance(underTest)));
    assertThat(underTest.getBlobCount(), is(42L));
    assertAllFieldsDefaultExcept("blobCount");
  }

  @Test
  public void testSetUploadBlobSize() {
    assertThat(underTest.setUploadBlobSize(555L), is(sameInstance(underTest)));
    assertThat(underTest.getUploadBlobSize(), is(555L));
    assertAllFieldsDefaultExcept("uploadBlobSize");
  }

  @Test
  public void testSetUploadSuccessfulRequests() {
    assertThat(underTest.setUploadSuccessfulRequests(7L), is(sameInstance(underTest)));
    assertThat(underTest.getUploadSuccessfulRequests(), is(7L));
    assertAllFieldsDefaultExcept("uploadSuccessfulRequests");
  }

  @Test
  public void testSetUploadTimeOnRequests() {
    assertThat(underTest.setUploadTimeOnRequests(900L), is(sameInstance(underTest)));
    assertThat(underTest.getUploadTimeOnRequests(), is(900L));
    assertAllFieldsDefaultExcept("uploadTimeOnRequests");
  }

  @Test
  public void testSetUploadErrorRequests() {
    assertThat(underTest.setUploadErrorRequests(3L), is(sameInstance(underTest)));
    assertThat(underTest.getUploadErrorRequests(), is(3L));
    assertAllFieldsDefaultExcept("uploadErrorRequests");
  }

  @Test
  public void testSetDownloadBlobSize() {
    assertThat(underTest.setDownloadBlobSize(888L), is(sameInstance(underTest)));
    assertThat(underTest.getDownloadBlobSize(), is(888L));
    assertAllFieldsDefaultExcept("downloadBlobSize");
  }

  @Test
  public void testSetDownloadSuccessfulRequests() {
    assertThat(underTest.setDownloadSuccessfulRequests(11L), is(sameInstance(underTest)));
    assertThat(underTest.getDownloadSuccessfulRequests(), is(11L));
    assertAllFieldsDefaultExcept("downloadSuccessfulRequests");
  }

  @Test
  public void testSetDownloadTimeOnRequests() {
    assertThat(underTest.setDownloadTimeOnRequests(1500L), is(sameInstance(underTest)));
    assertThat(underTest.getDownloadTimeOnRequests(), is(1500L));
    assertAllFieldsDefaultExcept("downloadTimeOnRequests");
  }

  @Test
  public void testSetDownloadErrorRequests() {
    assertThat(underTest.setDownloadErrorRequests(9L), is(sameInstance(underTest)));
    assertThat(underTest.getDownloadErrorRequests(), is(9L));
    assertAllFieldsDefaultExcept("downloadErrorRequests");
  }

  @Test
  public void testSettersOverwritePreviousValue() {
    underTest.setTotalSize(10L);
    underTest.setTotalSize(20L);
    assertThat(underTest.getTotalSize(), is(20L));

    underTest.setBlobStoreName("first");
    underTest.setBlobStoreName("second");
    assertThat(underTest.getBlobStoreName(), is("second"));
  }

  @Test
  public void testSettersStoreLongBoundaryValuesExactly() {
    underTest.setTotalSize(Long.MAX_VALUE);
    underTest.setBlobCount(Long.MIN_VALUE);

    assertThat(underTest.getTotalSize(), is(Long.MAX_VALUE));
    assertThat(underTest.getBlobCount(), is(Long.MIN_VALUE));
  }

  @Test
  public void testFluentChaining() {
    BlobStoreMetricsEntity result = underTest
        .setBlobStoreName("chained-store")
        .setTotalSize(100L)
        .setBlobCount(2L)
        .setUploadBlobSize(200L)
        .setUploadSuccessfulRequests(3L)
        .setUploadTimeOnRequests(300L)
        .setUploadErrorRequests(4L)
        .setDownloadBlobSize(400L)
        .setDownloadSuccessfulRequests(5L)
        .setDownloadTimeOnRequests(500L)
        .setDownloadErrorRequests(6L);

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getBlobStoreName(), is("chained-store"));
    assertThat(underTest.getTotalSize(), is(100L));
    assertThat(underTest.getBlobCount(), is(2L));
    assertThat(underTest.getUploadBlobSize(), is(200L));
    assertThat(underTest.getUploadSuccessfulRequests(), is(3L));
    assertThat(underTest.getUploadTimeOnRequests(), is(300L));
    assertThat(underTest.getUploadErrorRequests(), is(4L));
    assertThat(underTest.getDownloadBlobSize(), is(400L));
    assertThat(underTest.getDownloadSuccessfulRequests(), is(5L));
    assertThat(underTest.getDownloadTimeOnRequests(), is(500L));
    assertThat(underTest.getDownloadErrorRequests(), is(6L));
  }

  @Test
  public void testToStringWithDefaults() {
    String expected = "BlobStoreMetricsEntity{" +
        "id='null'" +
        ", blobStoreName='null'" +
        ", totalSize=0" +
        ", blobCount=0" +
        ", uploadBlobSize=0" +
        ", uploadSuccessfulRequests=0" +
        ", uploadTimeOnRequests=0" +
        ", uploadErrorRequests=0" +
        ", downloadBlobSize=0" +
        ", downloadSuccessfulRequests=0" +
        ", downloadTimeOnRequests=0" +
        ", downloadErrorRequests=0" +
        "}";

    assertThat(underTest.toString(), is(expected));
  }

  @Test
  public void testToStringWithValues() {
    underTest
        .setBlobStoreName("metrics-store")
        .setTotalSize(1000L)
        .setBlobCount(10L)
        .setUploadBlobSize(2000L)
        .setUploadSuccessfulRequests(20L)
        .setUploadTimeOnRequests(3000L)
        .setUploadErrorRequests(30L)
        .setDownloadBlobSize(4000L)
        .setDownloadSuccessfulRequests(40L)
        .setDownloadTimeOnRequests(5000L)
        .setDownloadErrorRequests(50L);

    // id has no setter, so it remains null even after every other field is populated
    String expected = "BlobStoreMetricsEntity{" +
        "id='null'" +
        ", blobStoreName='metrics-store'" +
        ", totalSize=1000" +
        ", blobCount=10" +
        ", uploadBlobSize=2000" +
        ", uploadSuccessfulRequests=20" +
        ", uploadTimeOnRequests=3000" +
        ", uploadErrorRequests=30" +
        ", downloadBlobSize=4000" +
        ", downloadSuccessfulRequests=40" +
        ", downloadTimeOnRequests=5000" +
        ", downloadErrorRequests=50" +
        "}";

    assertThat(underTest.toString(), is(expected));
  }

  /**
   * Asserts that every field of {@link #underTest} still holds its default value (null for {@code blobStoreName},
   * {@code 0L} for the longs) except the explicitly named fields. Used to prove that a setter mutates ONLY its own
   * field and does not route its argument into a sibling field.
   */
  private void assertAllFieldsDefaultExcept(final String... changedFields) {
    List<String> changed = Arrays.asList(changedFields);
    if (!changed.contains("blobStoreName")) {
      assertThat(underTest.getBlobStoreName(), is(nullValue()));
    }
    if (!changed.contains("totalSize")) {
      assertThat(underTest.getTotalSize(), is(0L));
    }
    if (!changed.contains("blobCount")) {
      assertThat(underTest.getBlobCount(), is(0L));
    }
    if (!changed.contains("uploadBlobSize")) {
      assertThat(underTest.getUploadBlobSize(), is(0L));
    }
    if (!changed.contains("uploadSuccessfulRequests")) {
      assertThat(underTest.getUploadSuccessfulRequests(), is(0L));
    }
    if (!changed.contains("uploadTimeOnRequests")) {
      assertThat(underTest.getUploadTimeOnRequests(), is(0L));
    }
    if (!changed.contains("uploadErrorRequests")) {
      assertThat(underTest.getUploadErrorRequests(), is(0L));
    }
    if (!changed.contains("downloadBlobSize")) {
      assertThat(underTest.getDownloadBlobSize(), is(0L));
    }
    if (!changed.contains("downloadSuccessfulRequests")) {
      assertThat(underTest.getDownloadSuccessfulRequests(), is(0L));
    }
    if (!changed.contains("downloadTimeOnRequests")) {
      assertThat(underTest.getDownloadTimeOnRequests(), is(0L));
    }
    if (!changed.contains("downloadErrorRequests")) {
      assertThat(underTest.getDownloadErrorRequests(), is(0L));
    }
  }
}
