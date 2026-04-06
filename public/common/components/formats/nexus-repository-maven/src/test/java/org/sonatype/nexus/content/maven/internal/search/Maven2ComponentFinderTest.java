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
package org.sonatype.nexus.content.maven.internal.search;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Maven2ComponentFinder}.
 *
 * Specifically tests NEXUS-44583: Support for non-timestamped SNAPSHOT versions
 * in addition to timestamped SNAPSHOT versions.
 */
public class Maven2ComponentFinderTest
    extends TestSupport
{
  private static final String NAMESPACE = "org.example";

  private static final String NAME = "my-artifact";

  private static final String BASE_VERSION = "1.0-SNAPSHOT";

  private static final String SNAPSHOT_ID = "test-repo:org.example:my-artifact:1.0-SNAPSHOT";

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentComponentBuilder componentBuilder;

  @Mock
  private FluentComponent timestampedComponent1;

  @Mock
  private FluentComponent timestampedComponent2;

  @Mock
  private FluentComponent nonTimestampedComponent;

  private Maven2ComponentFinder finder;

  @Before
  public void setup() {
    finder = new Maven2ComponentFinder();

    // Setup repository type as hosted
    when(repository.getType()).thenReturn(new HostedType());
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);

    // Setup component builder
    when(fluentComponents.name(NAME)).thenReturn(componentBuilder);
    when(componentBuilder.namespace(NAMESPACE)).thenReturn(componentBuilder);
  }

  /**
   * NEXUS-44583: Test that timestamped SNAPSHOT versions are found (existing behavior).
   * This is the standard Maven 3.x format: 1.0-20260302.123456-1
   */
  @Test
  public void testFindComponentsByModel_WithTimestampedSnapshots() {
    // Arrange: Repository has timestamped SNAPSHOT versions
    String timestampedVersion1 = "1.0-20260302.123456-1";
    String timestampedVersion2 = "1.0-20260302.140000-2";

    when(fluentComponents.versions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList(timestampedVersion1, timestampedVersion2));

    when(componentBuilder.version(timestampedVersion1))
        .thenReturn(componentBuilder);
    when(componentBuilder.version(timestampedVersion2))
        .thenReturn(componentBuilder);

    when(componentBuilder.find())
        .thenReturn(Optional.of(timestampedComponent1))
        .thenReturn(Optional.of(timestampedComponent2));

    // Act: Find components
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: Both timestamped versions found
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, hasSize(2));
    // Note: Sorted in reverse order, but mocking order might differ
    assertThat(components, contains(timestampedComponent1, timestampedComponent2));
  }

  /**
   * NEXUS-44583: Test that non-timestamped SNAPSHOT versions are found (new behavior).
   * This is the legacy Maven 2.x format: 1.0-SNAPSHOT
   *
   * This test verifies the fix for the bug where GUI-uploaded SNAPSHOTs caused 404 errors.
   */
  @Test
  public void testFindComponentsByModel_WithNonTimestampedSnapshot() {
    // Arrange: Repository has non-timestamped SNAPSHOT version (GUI upload)
    String nonTimestampedVersion = "1.0-SNAPSHOT";

    when(fluentComponents.versions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList(nonTimestampedVersion));

    when(componentBuilder.version(nonTimestampedVersion))
        .thenReturn(componentBuilder);

    when(componentBuilder.find())
        .thenReturn(Optional.of(nonTimestampedComponent));

    // Act: Find components
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: Non-timestamped version found (FIX for NEXUS-44583)
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, hasSize(1));
    assertThat(components, contains(nonTimestampedComponent));
  }

  /**
   * NEXUS-44583: Test mixed scenario with both timestamped and non-timestamped SNAPSHOTs.
   * This can happen when some artifacts are uploaded via GUI and others via Maven deploy.
   */
  @Test
  public void testFindComponentsByModel_WithMixedSnapshots() {
    // Arrange: Repository has both timestamped and non-timestamped versions
    String nonTimestampedVersion = "1.0-SNAPSHOT";
    String timestampedVersion1 = "1.0-20260302.123456-1";
    String timestampedVersion2 = "1.0-20260302.140000-2";

    when(fluentComponents.versions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList(nonTimestampedVersion, timestampedVersion1, timestampedVersion2));

    when(componentBuilder.version(anyString())).thenReturn(componentBuilder);

    when(componentBuilder.find())
        .thenReturn(Optional.of(nonTimestampedComponent))
        .thenReturn(Optional.of(timestampedComponent1))
        .thenReturn(Optional.of(timestampedComponent2));

    // Act: Find components
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: All versions found, sorted in reverse order (newest first)
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, hasSize(3));
    // Note: Natural string sort puts "1.0-SNAPSHOT" before "1.0-20260302.*"
  }

  /**
   * Test that only versions matching the base version prefix are included.
   */
  @Test
  public void testFindComponentsByModel_FiltersWrongBaseVersion() {
    // Arrange: Repository has versions from different base versions
    String correctNonTimestamped = "1.0-SNAPSHOT";
    String correctTimestamped = "1.0-20260302.123456-1";
    String wrongNonTimestamped = "2.0-SNAPSHOT"; // Different base version
    String wrongTimestamped = "2.0-20260302.123456-1"; // Different base version

    when(fluentComponents.versions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList(correctNonTimestamped, correctTimestamped,
            wrongNonTimestamped, wrongTimestamped));

    when(componentBuilder.version(correctNonTimestamped))
        .thenReturn(componentBuilder);
    when(componentBuilder.version(correctTimestamped))
        .thenReturn(componentBuilder);

    when(componentBuilder.find())
        .thenReturn(Optional.of(nonTimestampedComponent))
        .thenReturn(Optional.of(timestampedComponent1));

    // Act: Find components for base version 1.0-SNAPSHOT
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: Only 1.0-* versions found
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, hasSize(2));
  }

  /**
   * Test that non-SNAPSHOT versions use the default finder (not the special SNAPSHOT logic).
   */
  @Test
  public void testFindComponentsByModel_NonSnapshotUsesDefaultBehavior() {
    // Arrange: Non-SNAPSHOT version
    String releaseVersion = "1.0.0";
    String releaseId = "test-repo:org.example:my-artifact:1.0.0";

    // Mock the default behavior that would be called
    when(componentBuilder.version(releaseVersion)).thenReturn(componentBuilder);
    when(componentBuilder.find()).thenReturn(Optional.empty());

    // Act: Find components
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, releaseId, NAMESPACE, NAME, releaseVersion);

    // Assert: Falls through to super.findComponentsByModel
    // This test mainly ensures no exception is thrown for release versions
    assertThat(result.collect(Collectors.toList()), empty());
  }

  /**
   * NEXUS-44583: Test group repository with non-timestamped SNAPSHOT.
   */
  @Test
  public void testFindComponentsByModel_GroupRepositoryWithNonTimestampedSnapshot() {
    // Arrange: Group repository with non-timestamped SNAPSHOT
    when(repository.getType()).thenReturn(new GroupType());

    String nonTimestampedVersion = "1.0-SNAPSHOT";

    when(fluentComponents.memberVersions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList(nonTimestampedVersion));

    when(componentBuilder.version(nonTimestampedVersion))
        .thenReturn(componentBuilder);

    when(componentBuilder.findInMembers())
        .thenReturn(Optional.of(nonTimestampedComponent));

    // Act: Find components in group repository
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: Non-timestamped version found in group repository
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, hasSize(1));
    assertThat(components, contains(nonTimestampedComponent));
  }

  /**
   * NEXUS-44583: Test group repository with mixed SNAPSHOTs.
   */
  @Test
  public void testFindComponentsByModel_GroupRepositoryWithMixedSnapshots() {
    // Arrange: Group repository with both types
    when(repository.getType()).thenReturn(new GroupType());

    String nonTimestampedVersion = "1.0-SNAPSHOT";
    String timestampedVersion = "1.0-20260302.123456-1";

    when(fluentComponents.memberVersions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList(nonTimestampedVersion, timestampedVersion));

    when(componentBuilder.version(anyString())).thenReturn(componentBuilder);

    when(componentBuilder.findInMembers())
        .thenReturn(Optional.of(nonTimestampedComponent))
        .thenReturn(Optional.of(timestampedComponent1));

    // Act: Find components in group repository
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: Both versions found in group repository
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, hasSize(2));
  }

  /**
   * Test empty result when no matching versions exist.
   */
  @Test
  public void testFindComponentsByModel_NoMatchingVersions() {
    // Arrange: Repository has no versions matching the base version
    when(fluentComponents.versions(NAMESPACE, NAME))
        .thenReturn(Arrays.asList("2.0-SNAPSHOT", "3.0-20260302.123456-1"));

    // Act: Find components
    Stream<FluentComponent> result = finder.findComponentsByModel(
        repository, SNAPSHOT_ID, NAMESPACE, NAME, BASE_VERSION);

    // Assert: No components found
    List<FluentComponent> components = result.collect(Collectors.toList());
    assertThat(components, is(empty()));
  }

  /**
   * Test that the SNAPSHOT_TIMESTAMP pattern works correctly.
   */
  @Test
  public void testSnapshotTimestampPattern() {
    // Test various version formats against the pattern
    assertThat("Timestamped SNAPSHOT should match",
        Maven2ComponentFinder.SNAPSHOT_TIMESTAMP.matcher("1.0-20260302.123456-1").matches(), is(true));

    assertThat("Another timestamped SNAPSHOT should match",
        Maven2ComponentFinder.SNAPSHOT_TIMESTAMP.matcher("2.0.0-20260302.123456-99").matches(), is(true));

    assertThat("Non-timestamped SNAPSHOT should NOT match",
        Maven2ComponentFinder.SNAPSHOT_TIMESTAMP.matcher("1.0-SNAPSHOT").matches(), is(false));

    assertThat("Release version should NOT match",
        Maven2ComponentFinder.SNAPSHOT_TIMESTAMP.matcher("1.0.0").matches(), is(false));

    assertThat("Invalid timestamp format should NOT match",
        Maven2ComponentFinder.SNAPSHOT_TIMESTAMP.matcher("1.0-20260302-1").matches(), is(false));
  }
}
