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
package org.sonatype.nexus.repository.maven.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentFactory;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.repository.maven.internal.PurgeUnusedSnapshotsFacetImplTest.TestData.testData;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_FORMAT;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class PurgeUnusedSnapshotsFacetImplTest
    extends TestSupport
{
  static final int FIND_USED_LIMIT = 10;

  static final Long NUMBER_OF_COMPONENTS = 35L;

  static final LocalDate taskOlderThan = LocalDate.now().minusDays(10);

  final BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();

  final ComponentFactory componentFactory = new ComponentFactory(emptySet());

  final ComponentEntityAdapter componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter,
      componentFactory, emptySet());

  @Mock
  MetadataRebuilder metaRebuilder;

  @Mock
  Type groupType;

  @Mock
  Type hostedType;

  PurgeUnusedSnapshotsFacetImpl purgeUnusedSnapshotsFacet;

  @Mock
  MavenFacet mavenFacet;

  @Mock
  Repository repository;

  @Mock
  StorageTx storageTx;

  @Mock
  Bucket bucket;

  @Mock
  ODatabaseDocumentTx oDatabaseDocumentTx;

  ORID bucketId = new ORecordId(1, 1);

  int clusterPosition;

  @Before
  public void setUp() throws Exception {
    clusterPosition = 1;
    purgeUnusedSnapshotsFacet = new PurgeUnusedSnapshotsFacetImpl(componentEntityAdapter,
        metaRebuilder, groupType, hostedType, FIND_USED_LIMIT);
    purgeUnusedSnapshotsFacet.attach(repository);

    when(repository.getName()).thenReturn("test-repo");
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.countComponents(any(Query.class), any())).thenReturn(NUMBER_OF_COMPONENTS);
    when(storageTx.getDb()).thenReturn(oDatabaseDocumentTx);

    UnitOfWork.beginBatch(storageTx);

    mockBucket();
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void deleteUnusedSnapshotComponents() throws Exception {
    mockPagesOfComponents();

    Set<String> set = purgeUnusedSnapshotsFacet.deleteUnusedSnapshotComponents(taskOlderThan);
    assertThat(set.size(), equalTo(3));
    assertThat(set, not(hasItem("that.company")));
    assertThat(set, containsInAnyOrder("my.company", "your.company", "this.company"));

    assertComponentDeletions();

    assertQueries();
  }

  // Mock the four pages of components. Each OCommandRequest is a page.
  // Each page needs to be full. First 3 pages have 10, last has 5. For 35 total test components in set.
  // Test data below is part of the page, the rest is padded with non-matching data (i.e. non-snapshots or not older)
  private void mockPagesOfComponents() {
    OCommandRequest oCommandRequest1 = getCommandRequest(10,
        testData(taskOlderThan.minusDays(1), "my.company", "foo", "1.0-SNAPSHOT"),
        testData(taskOlderThan.minusDays(2), "my.company", "bar", "2.0-SNAPSHOT"));
    OCommandRequest oCommandRequest2 = getCommandRequest(10,
        // non-match, not a snapshot
        testData(taskOlderThan.minusDays(1), "your.company", "biz", "1.0")
    );
    OCommandRequest oCommandRequest3 = getCommandRequest(10,
        testData(taskOlderThan.minusDays(2), "this.company", "baz", "3.0-SNAPSHOT")
    );
    OCommandRequest oCommandRequest4 = getCommandRequest(5,
        testData(taskOlderThan.minusDays(3), "my.company", "foo", "0.1-SNAPSHOT"),
        testData(taskOlderThan.minusDays(6), "your.company", "biz", "1.0-SNAPSHOT"),
        // non-match, this is same day
        testData(taskOlderThan, "that.company", "fizz", "1.2.3-SNAPSHOT")
    );

    when(oDatabaseDocumentTx.command(any(OCommandScript.class)))
        .thenReturn(oCommandRequest1, oCommandRequest2, oCommandRequest3, oCommandRequest4);
  }

  private void assertComponentDeletions() {
    // assertions for the five component deletions
    ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);
    verify(storageTx, times(5)).deleteComponent(componentCaptor.capture());
    List<Component> components = componentCaptor.getAllValues();
    assertThat(components.size(), equalTo(5));
    assertThat(components.get(0).name(), equalTo("foo"));
    assertThat(components.get(1).name(), equalTo("bar"));
    assertThat(components.get(2).name(), equalTo("baz"));
    assertThat(components.get(3).name(), equalTo("foo"));
    assertThat(components.get(4).name(), equalTo("biz"));
  }

  private void assertQueries() {
    // assertions for the four queries run for the four 'pages'
    ArgumentCaptor<OCommandScript> argumentCaptor = ArgumentCaptor.forClass(OCommandScript.class);
    verify(oDatabaseDocumentTx, times(4)).command(argumentCaptor.capture());
    List<OCommandScript> args = argumentCaptor.getAllValues();
    assertThat(args.size(), equalTo(4));

    // finally, assert the actual queries
    String query = "sql.LET $a = (SELECT FROM component WHERE bucket = %s AND @RID > %s ORDER BY @RID LIMIT %d); " +
        "LET $b = (SELECT component, max(ifnull(last_downloaded, blob_created)) as lastdownloaded " +
        "FROM asset WHERE ((bucket = #1:1 AND component = $a[0]) OR " +
        "(bucket = #1:1 AND component = $a[1]) OR " +
        "(bucket = #1:1 AND component = $a[2]) OR " +
        "(bucket = #1:1 AND component = $a[3]) OR " +
        "(bucket = #1:1 AND component = $a[4]) OR " +
        "(bucket = #1:1 AND component = $a[5]) OR " +
        "(bucket = #1:1 AND component = $a[6]) OR " +
        "(bucket = #1:1 AND component = $a[7]) OR " +
        "(bucket = #1:1 AND component = $a[8]) OR " +
        "(bucket = #1:1 AND component = $a[9])) " +
        "GROUP BY component ORDER BY component); " +
        "SELECT FROM $b WHERE (component.attributes.maven2.baseVersion LIKE '%%SNAPSHOT' " +
        "AND lastdownloaded < '%s') OR component = $a[9];";
    String date = makeIso8601(taskOlderThan);
    assertThat(args.get(0).toString(), equalTo(format(query, bucketId, "#-1:-1", 10, date)));
    assertThat(args.get(1).toString(), equalTo(format(query, bucketId, "#1:10", 10, date)));
    assertThat(args.get(2).toString(), equalTo(format(query, bucketId, "#1:20", 10, date)));
    assertThat(args.get(3).toString(), equalTo(format(query, bucketId, "#1:30", 10, date)));
  }

  private String makeIso8601(LocalDate date) {
    return date.format(ISO_LOCAL_DATE);
  }

  @Test
  public void getUnusedWhere() throws Exception {
    String query = purgeUnusedSnapshotsFacet.getUnusedWhere(bucketId);
    assertThat(query, equalTo(
        "(bucket = #1:1 AND component = $a[0]) OR (bucket = #1:1 AND component = $a[1]) OR (bucket = #1:1 AND component = $a[2]) OR (bucket = #1:1 AND component = $a[3]) OR (bucket = #1:1 AND component = $a[4]) OR (bucket = #1:1 AND component = $a[5]) OR (bucket = #1:1 AND component = $a[6]) OR (bucket = #1:1 AND component = $a[7]) OR (bucket = #1:1 AND component = $a[8]) OR (bucket = #1:1 AND component = $a[9])"));
  }

  private void mockBucket() {
    EntityAdapter owner = mock(EntityAdapter.class);
    ODocument document = mock(ODocument.class);
    ORID orID = new ORecordId(1, 1);
    when(document.getIdentity()).thenReturn(orID);
    EntityMetadata entityMetadata = new AttachedEntityMetadata(owner, document);
    when(bucket.getEntityMetadata()).thenReturn(entityMetadata);
  }

  private OCommandRequest getCommandRequest(final int total, final TestData... documents) {
    OCommandRequest oCommandRequest = mock(OCommandRequest.class);
    List<ODocument> resultSet = new ArrayList<>();
    when(oCommandRequest.execute()).thenReturn(resultSet);

    for (TestData testData : documents) {
      ODocument resultSetRow = new ODocument();
      resultSetRow.field(P_COMPONENT, newComponentDoc(testData));
      resultSetRow.field("lastdownloaded", java.sql.Date.valueOf(testData.lastDownloadDate));
      resultSet.add(resultSetRow);
    }

    while (resultSet.size() < total) {
      ODocument resultSetRow = new ODocument();
      TestData testData = testData(taskOlderThan.plusDays(resultSet.size()), "groupId", "artifactId", "1.0");
      resultSetRow.field(P_COMPONENT, newComponentDoc(testData));
      resultSetRow.field("lastdownloaded", java.sql.Date.valueOf(testData.lastDownloadDate));
      resultSet.add(resultSetRow);
    }

    return oCommandRequest;
  }

  private ODocument newComponentDoc(final TestData testData) {
    ORID id = new ORecordId(1, clusterPosition++);
    ODocument componentDoc = new ODocument(id);
    componentDoc.field(P_NAME, testData.artifactId);
    componentDoc.field(P_BUCKET, bucketEntityAdapter.recordIdentity(bucket));
    componentDoc.field(P_FORMAT, "maven2");

    Map<String, Object> mavenAttributes = new HashMap<>();
    mavenAttributes.put(P_GROUP_ID, testData.groupId);
    mavenAttributes.put(P_ARTIFACT_ID, testData.artifactId);
    mavenAttributes.put(P_BASE_VERSION, testData.baseVersion);

    Map<String, Object> map = new HashMap<>();
    map.put("maven2", mavenAttributes);
    componentDoc.field(P_ATTRIBUTES, map);

    return componentDoc;
  }

  static class TestData
  {
    final LocalDate lastDownloadDate;

    final String groupId;

    final String artifactId;

    final String baseVersion;

    private TestData(LocalDate lastDownloadDate, String groupId, String artifactId, String baseVersion) {
      this.lastDownloadDate = lastDownloadDate;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.baseVersion = baseVersion;
    }

    static TestData testData(LocalDate lastDownloadDate,
                             String groupId,
                             String artifactId,
                             String baseVersion)
    {
      return new TestData(lastDownloadDate, groupId, artifactId, baseVersion);
    }
  }
}
