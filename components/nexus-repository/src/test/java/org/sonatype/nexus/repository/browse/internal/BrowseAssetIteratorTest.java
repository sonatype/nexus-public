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
package org.sonatype.nexus.repository.browse.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.selector.internal.ContentAuthHelper;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static com.google.common.collect.Iterables.size;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.browse.internal.BrowseAssetIterator.QUERY_TEMPLATE;

@SuppressWarnings("unchecked")
public class BrowseAssetIteratorTest {
  private static final ORecordId NEW_RECORD = new ORecordId();

  private static final String NEW_RID = NEW_RECORD.getIdentity().toString();

  private static final int ASSET_CLUSTER_ID = 1;

  private static final String FOO = "foo";

  private long assetPositionId = 1;

  private ContentAuthHelper contentAuthHelper = mock(ContentAuthHelper.class);

  private ODatabaseDocumentTx db;

  private BrowseAssetIterator underTest;

  @Before
  public void init() {
    when(contentAuthHelper.checkAssetPermissions(any(), eq(FOO))).thenReturn(true);
  }

  @Test
  public void testNullRid() {
    db = mockDbBrowse(page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, null, FOO, singletonList("#9:54"), 4, 100);

    assertThat(underTest.getRid(), nullValue());
    assertThat(underTest.getBucketIds(), contains("#9:54"));
    assertThat(underTest.getLimit(), equalTo(4));
    assertThat(underTest.getStartRid(), nullValue());
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getQuery(), equalTo("<<browse class asset>>"));
    assertThat(underTest.hasNext(), equalTo(true));
    assertThat(underTest.getCount(), equalTo(0));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(1));
    assertThat(underTest.getStartRid(), equalTo("#1:3"));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(2));
    assertThat(underTest.getStartRid(), equalTo("#1:3"));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(3));
    assertThat(underTest.getStartRid(), equalTo("#1:3"));

    assertThat(underTest.hasNext(), equalTo(false));
  }

  @Test
  public void testNullRidNoBuckets() {
    db = mockDbBrowse(page(3, "#9:54"), page(3, "#9:54"), page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, null, FOO, emptyList(), 10, 100);

    assertThat(underTest.getRid(), nullValue());
    assertThat(underTest.getBucketIds(), hasSize(0));
    assertThat(underTest.getLimit(), equalTo(10));
    assertThat(underTest.getStartRid(), nullValue());
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getQuery(), equalTo("<<browse class asset>>"));

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(9));
    assertThat(underTest.isHasBrowsedClass(), is(true));
  }

  @Test
  public void testNullRidMultipleBuckets() {
    db = mockDbBrowse(page(3, "#9:54"), page(3, "#9:54"), page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, null, FOO, asList("#9:54", "#9:51", "#11:12"), 10, 100);

    assertThat(underTest.getRid(), nullValue());
    assertThat(underTest.getBucketIds(), contains("#9:54", "#9:51", "#11:12"));
    assertThat(underTest.getLimit(), equalTo(10));
    assertThat(underTest.getStartRid(), nullValue());
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getQuery(), equalTo("<<browse class asset>>"));

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(9));
    assertThat(underTest.isHasBrowsedClass(), is(true));
  }

  @Test
  public void testRid() {
    db = mockDbQuery("#11:924448", 50, page(3, "#9:54"), page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, "#11:924448", FOO, singletonList("#9:54"), 10, 50);

    assertThat(underTest.getRid(), equalTo("#11:924448"));
    assertThat(underTest.getBucketIds(), contains("#9:54"));
    assertThat(underTest.getLimit(), equalTo(10));
    assertThat(underTest.getStartRid(), equalTo("#11:924448"));
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getQuery(), equalTo("select from asset where @rid > #11:924448"));

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(6));
    assertThat(underTest.isHasBrowsedClass(), is(false));
  }

  @Test
  public void testRidMultipleBuckets() {
    db = mockDbQuery("#11:924448", 100,
        page(3, "#9:51"),
        page(3, "#9:54"),
        page(3, "#9:51"),
        page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, "#11:924448", FOO, asList("#9:54", "#9:51", "#11:12"),
        10, 100);

    assertThat(underTest.getRid(), equalTo("#11:924448"));
    assertThat(underTest.getBucketIds(), contains("#9:54", "#9:51", "#11:12"));
    assertThat(underTest.getLimit(), equalTo(10));
    assertThat(underTest.getStartRid(), equalTo("#11:924448"));
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getQuery(), equalTo("select from asset where @rid > #11:924448"));

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(10));
    assertThat(underTest.isHasBrowsedClass(), is(false));
  }

  @Test
  public void testSkipEmptyPages() {
    db = mockDbQuery(NEW_RID, 100,
        page(3, "#9:51"),
        page(3, "#9:51"),
        page(3, "#9:51"),
        page(3, "#9:54"),
        page(3, "#9:51"),
        page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, NEW_RID, FOO, singletonList("#9:54"), 4, 100);

    assertThat(underTest.getRid(), equalTo(NEW_RID));
    assertThat(underTest.getBucketIds(), contains("#9:54"));
    assertThat(underTest.getLimit(), equalTo(4));
    assertThat(underTest.getStartRid(), equalTo(NEW_RID));
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getQuery(), equalTo("select from asset where @rid > #-1:-1"));
    assertThat(underTest.hasNext(), equalTo(true));
    assertThat(underTest.getCount(), equalTo(0));
    assertThat(underTest.getStartRid(), equalTo("#1:12"));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(1));
    assertThat(underTest.getStartRid(), equalTo("#1:12"));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(2));
    assertThat(underTest.getStartRid(), equalTo("#1:12"));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(3));
    assertThat(underTest.getStartRid(), equalTo("#1:12"));

    underTest.next();
    assertThat(underTest.getCount(), equalTo(4));
    assertThat(underTest.getStartRid(), equalTo("#1:18"));

    assertThat(underTest.hasNext(), equalTo(false));

    assertThat(underTest.isHasBrowsedClass(), is(false));
  }

  @Test
  public void testIteratorCount() {
    db = mockDbQuery(NEW_RID, 100, page(3, "#9:54"), page(3, "#9:54"));
    underTest = new BrowseAssetIterator(contentAuthHelper, db, NEW_RID, FOO, singletonList("#9:54"), 4, 100);

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(4));
    assertThat(underTest.isHasBrowsedClass(), is(false));
  }

  @Test
  public void testIteratorCountWithSkips() {
    db = mockDbQuery(NEW_RID, 100,
        page(2, "#9:51"),
        page(4, "#9:51"),
        page(6, "#9:51"),
        page(3, "#9:54"),
        page(7, "#9:51"),
        page(3, "#9:54"),
        page(3, "#9:54"),
        page(3, "#9:54"));

    underTest = new BrowseAssetIterator(contentAuthHelper, db, NEW_RID, FOO, singletonList("#9:54"), 10, 100);

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(10));
    assertThat(underTest.isHasBrowsedClass(), is(false));
  }

  @Test
  public void testQueryReturnsFewerThanLimitResults() {
    db = mockDbQuery(NEW_RID, 100, page(3, "#9:54"), page(3, "#9:51"));

    underTest = new BrowseAssetIterator(contentAuthHelper, db, NEW_RID, FOO, singletonList("#9:54"), 10, 100);

    assertThat(size((Iterable<ODocument>)() -> underTest), equalTo(3));
    assertThat(underTest.isHasBrowsedClass(), is(false));
  }

  private List<ODocument> page(final int pageCount, final String... buckets) {
    int bSize = buckets.length;
    List<ODocument> docs = new ArrayList<>(pageCount);
    for (int i = 0; i < pageCount; i++) {
      docs.add(assetDocument(buckets[i % bSize]));
    }
    return docs;
  }

  private static <T> List<T> concat(List<T>... lists) {
    return Stream.of(lists)
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private ODatabaseDocumentTx mockDbBrowse(final List<ODocument>... pages) {
    ODatabaseDocumentTx db = mock(ODatabaseDocumentTx.class);
    ORecordIteratorClass<ODocument> oIter = mock(ORecordIteratorClass.class);
    when(db.browseClass("asset")).thenReturn(oIter);
    when(oIter.spliterator()).thenReturn(concat(pages).spliterator());
    return db;
  }

  private ODatabaseDocumentTx mockDbQuery(final String startRid, final int pageSize, final List<ODocument>... pages) {
    ODatabaseDocumentTx db = mock(ODatabaseDocumentTx.class);
    String rid = startRid;
    for (List<ODocument> l : pages) {
      String query = String.format(QUERY_TEMPLATE, rid);
      when(db.query(query(query, pageSize))).thenReturn(l);
      rid = l.get(l.size() - 1).getIdentity().toString();
    }
    return db;
  }

  private ODocument assetDocument(final String bucket) {
    ODocument doc = new ODocument(new ORecordId(ASSET_CLUSTER_ID, assetPositionId++));
    doc.getIdentity();
    doc.field("bucket", new ORecordId(bucket));
    return doc;
  }

  private static OSQLSynchQuery<ODocument> query(final String query, final int pageSize) {
    return argThat(new QueryMatcher(new OSQLSynchQuery<>(query, pageSize)));
  }

  private static class QueryMatcher
      extends ArgumentMatcher<OSQLSynchQuery<ODocument>>
  {
    private final OSQLSynchQuery<ODocument> expected;

    QueryMatcher(final OSQLSynchQuery<ODocument> expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(final Object argument) {
      if (!(argument instanceof OSQLSynchQuery)) {
        return false;
      }

      OSQLSynchQuery<ODocument> actual = (OSQLSynchQuery<ODocument>) argument;

      return expected.getText().equals(actual.getText()) && expected.getLimit() == actual.getLimit();
    }
  }
}
