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
package org.sonatype.nexus.content.maven.internal.snapshot;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.store.GAV;
import org.sonatype.nexus.content.maven.store.Maven2ComponentData;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsConfig;
import org.sonatype.nexus.repository.types.GroupType;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.content.maven.internal.recipe.MavenProxyFacet.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Maven2Format.NAME;

public class RemoveSnapshotsFacetImplTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private MavenContentFacet facet;

  private RemoveSnapshotsFacetImpl removeSnapshotsFacet;

  @Before
  public void setup() throws Exception {
    removeSnapshotsFacet = spy(new RemoveSnapshotsFacetImpl(new GroupType()));

    when(repository.getName()).thenReturn("test");
    when(repository.facet(MavenContentFacet.class)).thenReturn(facet);
    removeSnapshotsFacet.attach(repository);
  }

  @Test
  public void testProcessRepository_noCandidatesNoDeletes() {
    validateProcessRepository(config(), Collections.emptySet(), Collections.emptyList(), 0);
  }

  @Test
  public void testProcessRepository_candidatesNoComponentNoDeletes() {
    validateProcessRepository(config(), Collections.singleton(gav(1)), Collections.emptyList(), 0);
  }

  @Test
  public void testProcessRepository_candidatesWithOneComponent() {
    validateProcessRepository(config(), Collections.singleton(gav(1)), Collections.singletonList(component()), 1);
  }

  @Test
  public void testProcessRepository_candidatesWithTwoComponents() {
    validateProcessRepository(config(), Collections.singleton(gav(2)),
        Arrays.asList(component(), component("1.0-20161110.233023")), 1);
  }

  private void validateProcessRepository(
      final RemoveSnapshotsConfig config,
      final Set<GAV> candidates,
      final List<Maven2ComponentData> comps,
      final int dels)
  {
    // stubbing out Spy internal methods here to avoid need to overly mock data layer
    doReturn(candidates).when(removeSnapshotsFacet).findSnapshotCandidates(eq(repository), anyInt());

    doReturn(comps).when(removeSnapshotsFacet).findComponentsForGav(eq(repository), any());

    // return the same set of components for this test. getSnapshotsToDelete is another test.
    when(removeSnapshotsFacet.getSnapshotsToDelete(config, comps)).thenReturn(new HashSet<>(comps));

    removeSnapshotsFacet.processRepository(repository, config);

    verify(removeSnapshotsFacet).processRepository(eq(repository), any());
    verify(facet, times(dels)).deleteComponents((int[]) any());
  }


  // purposefully out of order to test sorting
  private List<Maven2ComponentData> testGavWithRelease = Arrays.asList(
                            component("1.0-20160301.000001", 1),
                            component("1.0-20160228.000002", 2), // 2nd artifact on 2016-02-28
                            component("1.0-20160228.000001", 2),
                            component("1.0-20160220.000001", 10),
                            component("1.0-20160201.000001", 30),
                            component("1.0-20160101.000001", 60));// release artifact

  @Test
  public void testGetSnapshotsToDelete_emptySetOK() {
    verifyGetSnapshotsToDelete(config(), Collections.emptyList(), Collections.emptyList());
  }

  @Test
  public void testGetSnapshotsToDelete_releaseNoSnapshotsOK() {
    verifyGetSnapshotsToDelete(config(), Collections.singletonList(component("1.0", 0, "1.0")), Collections.emptyList());
  }

  @Test
  public void testGetSnapshotsToDelete_minSnapshotCountOneDeletesFive() {
    verifyGetSnapshotsToDelete(config(1), testGavWithRelease, Arrays.asList("1.0-20160101.000001", "1.0-20160201.000001",
        "1.0-20160220.000001", "1.0-20160228.000001", "1.0-20160228.000002"));
  }

  @Test
  public void testGetSnapshotsToDelete_minSnapshotCountTwoDeletesFour() {
    verifyGetSnapshotsToDelete(config(2), testGavWithRelease, Arrays.asList("1.0-20160101.000001", "1.0-20160201.000001",
        "1.0-20160220.000001", "1.0-20160228.000001"));  }

  @Test
  public void testGetSnapshotsToDelete_negativeMinSnapshotCountRetainsAll() {
    verifyGetSnapshotsToDelete(config(-1), testGavWithRelease, Collections.emptyList());
  }

  @Test
  public void testGetSnapshotsToDelete_retentionThreeDeletesThree() {
    verifyGetSnapshotsToDelete(config(1, 3), testGavWithRelease,
        Arrays.asList("1.0-20160101.000001", "1.0-20160201.000001", "1.0-20160220.000001"));
  }

  @Test
  public void testGetSnapshotsToDelete_retentionThreeButKeepFive() {
    verifyGetSnapshotsToDelete(config(5, 3), testGavWithRelease, Arrays.asList("1.0-20160101.000001"));
  }

  @Test
  public void testGetSnapshotsToDelete_removeIfReleasedNoGrace() {
    verifyGetSnapshotsToDelete(config(3, 0, true), testGavWithRelease, Arrays.asList("1.0-20160101.000001", "1.0-20160201.000001", "1.0-20160220.000001"));
  }

  @Test
  public void testGetSnapshotsToDelete_removeIfReleasedGraceTwo() {
    verifyGetSnapshotsToDelete(config(3, 0, true, 2), testGavWithRelease, Arrays.asList("1.0-20160101.000001", "1.0-20160201.000001", "1.0-20160220.000001"));
  }

  @Test
  public void testGetSnapshotsToDelete_configScenarioOneDayIsThirtyDeleted() {
    verifyGetSnapshotsToDelete(config(2, 25, true, 40), testGavWithRelease,
        Arrays.asList("1.0-20160101.000001", "1.0-20160201.000001"));
  }

  @Test
  public void testGetSnapshotsToDelete_configScenarioTwoDayIsThirtyDeleted() {
    verifyGetSnapshotsToDelete(config(2, 40, true, 25), testGavWithRelease,
        Arrays.asList("1.0-20160101.000001"));
  }

  @Test
  public void testGetSnapshotsToDelete_removeIfReleasedOnlyOneDeleted() {
    verifyGetSnapshotsToDelete(config(-1, 0, true, 0),
        Collections.singletonList(component("1.0-20160101", 0, "1.0-SNAPSHOT")), Collections.emptyList());
  }

  @Test
  public void testCalculateLastUpdatedWhereAreNoAssets() {
    OffsetDateTime lastUpdated = OffsetDateTime.now();
    Maven2ComponentData componentData = new Maven2ComponentData();
    componentData.setAssets(null);
    componentData.setLastUpdated(lastUpdated);

    assertThat(removeSnapshotsFacet.calculateLastUpdated(componentData), is(lastUpdated));
    componentData.setAssets(Collections.emptyList());
    assertThat(removeSnapshotsFacet.calculateLastUpdated(componentData), is(lastUpdated));
  }

  @Test
  public void testCalculateLastUpdatedWithAssets() {
    OffsetDateTime blobCreated = OffsetDateTime.now();
    OffsetDateTime lastUpdated = OffsetDateTime.now().minusMonths(4);
    Maven2ComponentData componentData = new Maven2ComponentData();
    AssetData assetData1 = new AssetData(); // latest
    AssetData assetData2 = new AssetData(); // 5 days ago
    AssetData assetData3 = new AssetData(); // no blob
    AssetBlobData assetBlob1 = new AssetBlobData();
    AssetBlobData assetBlob2 = new AssetBlobData();
    assetBlob1.setBlobCreated(blobCreated);
    assetBlob1.setAssetBlobId(1);
    assetBlob2.setBlobCreated(blobCreated.minusDays(2));
    assetBlob2.setAssetBlobId(2);
    assetData1.setAssetBlob(assetBlob1);
    assetData2.setAssetBlob(assetBlob2);
    componentData.setAssets(Arrays.asList(assetData1,assetData2, assetData3));
    componentData.setLastUpdated(lastUpdated);
    assertThat(removeSnapshotsFacet.calculateLastUpdated(componentData), is(blobCreated));
  }


  @Test
  public void testGetSnapshotsToDelete_removeIfReleasedOnlyWithNoRelease() {
    verifyGetSnapshotsToDelete(config(-1, 0, true, 0),
        Arrays.asList(component("2.0-20160101", 0, "2.0-SNAPSHOT"), component("2.0-20160102", 0, "2.0-SNAPSHOT")),
        Collections.emptyList());
  }


  private void verifyGetSnapshotsToDelete(
      final RemoveSnapshotsConfig config,
      final List<Maven2ComponentData> components,
      final List<String> expectedDeletions)
  {
    List<String> snapshotsToDelete = removeSnapshotsFacet.getSnapshotsToDelete(config, components).stream()
        .map(Maven2ComponentData::version)
        .collect(Collectors.toList());

    if (expectedDeletions.isEmpty()) {
      assertThat(snapshotsToDelete, empty());
    }
    else {
      assertThat(snapshotsToDelete, containsInAnyOrder(expectedDeletions.toArray(new String[expectedDeletions.size()])));
    }
  }

  private static GAV gav(final int count) {
    return new GAV("a", "b", "1.0-SNAPSHOT", count);
  }

  private static Maven2ComponentData component() {
    return component("1.0-20160101.000000");
  }

  private static Maven2ComponentData component(final String version) {
    return component(version, 0);
  }

  private static Maven2ComponentData component(final String version, final int lastUpdateAge) {
    return component(version, lastUpdateAge, "1.0-SNAPSHOT");
  }

  private static Maven2ComponentData component(
      final String version,
      final int lastUpdateAge,
      final String baseVersion)
  {
    return component(version, lastUpdateAge, baseVersion, "a", "b");
  }

  private static Maven2ComponentData component(
      final String version,
      final int lastUpdatedAge,
      final String baseVersion,
      final String group,
      final String name)
  {
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object> newHashMap());
    attributes.child(NAME).set(P_BASE_VERSION, baseVersion);
    Maven2ComponentData component = new Maven2ComponentData();
    component.setComponentId(1);
    component.setName(name);
    component.setNamespace(group);
    component.setVersion(version);
    component.setAttributes(attributes);
    // add five minutes to avoid timing issues with fast test executions where the timestamp might end up being the same
    component.setLastUpdated(OffsetDateTime.now().minusDays(lastUpdatedAge).minusMinutes(5));
    return component;
  }

  private static RemoveSnapshotsConfig config() {
    return config(1);
  }

  private static RemoveSnapshotsConfig config(final int minimumRetained) {
    return config(minimumRetained, 0);
  }

  private static RemoveSnapshotsConfig config(final int minimumRetained, final int snapshotRetentionDays) {
    return config(minimumRetained, snapshotRetentionDays, false);
  }

  private static RemoveSnapshotsConfig config(
      final int minimumRetained,
      final int snapshotRetentionDays,
      final boolean removeIfReleased)
  {
    return new RemoveSnapshotsConfig(minimumRetained, snapshotRetentionDays, removeIfReleased, 0);
  }

  private static RemoveSnapshotsConfig config(
      final int minimumRetained,
      final int snapshotRetentionDays,
      final boolean removeIfReleased,
      final int gracePeriod)
  {
    return new RemoveSnapshotsConfig(minimumRetained, snapshotRetentionDays, removeIfReleased, gracePeriod);
  }
}
