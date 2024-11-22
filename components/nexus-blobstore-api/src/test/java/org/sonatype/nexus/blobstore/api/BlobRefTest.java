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
package org.sonatype.nexus.blobstore.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_FORMATTER;

/**
 * Tests for {@link BlobRef}.
 *
 * @since 3.0
 */
public class BlobRefTest
{
  private static final String STORE_NAME = "test-store";

  private static final String NODE_ID = "ab761d55-5d9c22b6-3f38315a-75b3db34-0922a4d5";

  private static final String BLOB_ID = "a8f3f56f-e895-4b6e-984a-1cf1f5107d36";

  private static final String[] STORES = {"store-@:@:@name", "@", ":", "abc/+xy&%$#", "store-:@:@:@name-for-testing"};

  private static final OffsetDateTime DATE_CREATED = OffsetDateTime.of(2024, 1, 1, 10, 30, 45, 0, ZoneOffset.UTC);

  private static final String DATE_BASED_REF = DATE_CREATED.format(DATE_TIME_FORMATTER);

  @Test
  public void testToString() {
    final BlobRef blobRef = new BlobRef(NODE_ID, STORE_NAME, BLOB_ID);
    final String spec = blobRef.toString();
    final BlobRef reconstituted = BlobRef.parse(spec);

    assertThat(reconstituted, is(equalTo(blobRef)));
  }

  @Test
  public void testParseCanonical() {
    String blobRefString = String.format("%s@%s", STORE_NAME, BLOB_ID);
    BlobRef parsed = BlobRef.parse(blobRefString);
    assertParsed(parsed, STORE_NAME);
  }

  @Test
  public void testParseLegacyOrient() {
    String blobRefString = String.format("%s@%s:%s", STORE_NAME, NODE_ID, BLOB_ID);
    BlobRef parsed = BlobRef.parse(blobRefString);
    assertParsed(parsed, STORE_NAME);
  }

  @Test
  public void testParseLegacySql() {
    String blobRefString = String.format("%s:%s@%s", STORE_NAME, BLOB_ID, NODE_ID);
    BlobRef parsed = BlobRef.parse(blobRefString);
    assertParsed(parsed, STORE_NAME);
  }

  @Test
  public void testParseCanonicalWithFuzzyChars() {
    for (String storeName : STORES) {
      String blobRefString = String.format("%s@%s", storeName, BLOB_ID);
      BlobRef parsed = BlobRef.parse(blobRefString);
      assertParsed(parsed, storeName);
    }
  }

  @Test
  public void testParseLegacyOrientWithFuzzyChars() {
    for (String storeName : STORES) {
      String blobRefString = String.format("%s@%s:%s", storeName, NODE_ID, BLOB_ID);
      BlobRef parsed = BlobRef.parse(blobRefString);
      assertParsed(parsed, storeName);
    }
  }

  @Test
  public void testParseLegacySqlWithFuzzyChars() {
    for (String storeName : STORES) {
      String blobRefString = String.format("%s:%s@%s", storeName, BLOB_ID, NODE_ID);
      BlobRef parsed = BlobRef.parse(blobRefString);
      assertParsed(parsed, storeName);
    }
  }

  @Test
  public void testDateBasedLayout() {
    String blobRefString = String.format("%s@%s@%s", STORE_NAME, BLOB_ID, DATE_BASED_REF);
    BlobRef parsed = BlobRef.parse(blobRefString);
    assertThat(parsed.getBlob(), is(BLOB_ID));
    assertThat(parsed.getStore(), is(STORE_NAME));
    assertThat(parsed.getNode(), isEmptyOrNullString());
    OffsetDateTime blobCreatedRef = parsed.getDateBasedRef();
    assertThat(blobCreatedRef, notNullValue());
    assertThat(blobCreatedRef.format(DATE_TIME_FORMATTER), is(DATE_CREATED.format(DATE_TIME_FORMATTER)));
  }

  private void assertParsed(final BlobRef parsed, final String storeName) {
    assertThat(parsed.getBlob(), is(equalTo(BLOB_ID)));
    assertThat(parsed.getStore(), is(equalTo(storeName)));
    assertThat(parsed.getNode(), isEmptyOrNullString());
    assertThat(parsed.getDateBasedRef(), nullValue());
  }

  @Test
  public void testBlobRefIllegalFormat() {
    assertParseFailure("wrong-blobref-format/string");
    // empty blobstorename
    assertParseFailure("@nodeid:blobid");
    // empty nodeid
    assertParseFailure("blobstore@:blobid");
    // empty blobid
    assertParseFailure("blobstore@nodeid:");
    // no nodeid or blobid
    assertParseFailure("blobstore@");
  }

  private static void assertParseFailure(final String blobref) {
    try {
      BlobRef.parse(blobref);
      fail("Expected exception");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Not a valid blob reference"));
    }
  }
}
