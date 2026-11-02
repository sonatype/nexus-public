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
package org.sonatype.nexus.coreui.internal.content;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.coreui.ComponentXO;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.maintenance.ContentDeletionService;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.search.ComponentFinder;
import org.sonatype.nexus.repository.content.security.AssetPermissionChecker;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.selector.SelectorFactory;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentComponentHelperTest
{
  @Mock
  MaintenanceService maintenanceService;

  @Mock
  ContentDeletionService contentDeletionService;

  @Mock
  ComponentFinder componentFinder;

  @Mock
  AssetPermissionChecker assetPermissionChecker;

  @Mock
  SelectorFactory selectorFactory;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  NestedAttributesMap assetAttributes;

  @Mock
  AssetBlob blob;

  @Mock
  Repository repository;

  @Mock
  Subject subject;

  private MockedStatic<QualifierUtil> mockedStatic;

  private MockedStatic<SecurityUtils> mockedSecurityUtils;

  @Before
  public void setUp() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(Map.of("default", componentFinder));

    mockedSecurityUtils = Mockito.mockStatic(SecurityUtils.class);
    mockedSecurityUtils.when(SecurityUtils::getSubject).thenReturn(subject);

    // Default pass-through: every collected repository is permitted. Use doAnswer so stub overrides in
    // individual tests don't re-invoke this lambda with a null argument during Mockito's `when()` recording.
    doAnswer(inv -> StreamSupport.stream(inv.<Iterable<Repository>>getArgument(0).spliterator(), false)
        .collect(Collectors.toCollection(ArrayList::new)))
            .when(repositoryPermissionChecker)
            .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());
  }

  @After
  public void tearDown() {
    mockedStatic.close();
    mockedSecurityUtils.close();
  }

  @Test
  public void toAssetXOTestHosted() {

    when(repositoryManager.get("maven-hosted")).thenReturn(repository);
    when(repository.getType()).thenReturn(new HostedType());

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    AssetXO assetXO = underTest.toAssetXO(
        "maven-hosted",
        "maven-hosted",
        "maven-hosted",
        "maven2",
        createAsset(),
        true);
    assertThat(((Map) assetXO.getAttributes().get("content")).containsKey("last_modified"), is(false));
  }

  @Test
  public void toAssetXOTestProxy() {

    when(repositoryManager.get("maven-hosted")).thenReturn(repository);
    when(repository.getType()).thenReturn(new ProxyType());

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    AssetXO assetXO = underTest.toAssetXO(
        "maven-hosted",
        "maven-hosted",
        "maven-hosted",
        "maven2",
        createAsset(),
        true);
    Map<String, Object> contentMap = (Map<String, Object>) assetXO.getAttributes().get("content");
    assertThat(contentMap.containsKey("last_modified"), is(true));
    assertThat(contentMap.get("last_modified"), is("2023-11-13T16:00:20.450+02:00"));
  }

  @Test
  public void readComponentAssets_usesPermittingRepositoryForAssetUrl() {
    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("maven-public");
    when(groupRepo.getFormat()).thenReturn(new Format("maven2")
    {
    });
    when(groupRepo.getType()).thenReturn(new GroupType());
    when(repositoryManager.get("maven-public")).thenReturn(groupRepo);

    // Mock source repository (maven-hosted) for HostedType check
    Repository sourceRepo = mock(Repository.class);
    when(sourceRepo.getName()).thenReturn("maven-hosted");
    when(sourceRepo.getType()).thenReturn(new HostedType());
    when(repositoryManager.get("maven-hosted")).thenReturn(sourceRepo);

    FluentComponentImpl component = mock(FluentComponentImpl.class);
    Asset asset = createAsset();

    when(component.assets()).thenReturn((Collection) List.of(asset));
    when(componentFinder.findComponentsByModel(any(), any(), any(), any(), any()))
        .thenAnswer(inv -> java.util.stream.Stream.of(component));

    when(assetPermissionChecker.findPermittedAssets(any(Collection.class), anyString(), any()))
        .thenAnswer(inv -> {
          Collection<Asset> assets = inv.getArgument(0);
          return assets.stream()
              .map(a -> new AbstractMap.SimpleEntry<Asset, String>(a, "maven-public"));
        });

    ComponentXO model = new ComponentXO();
    model.setId("test-id");
    model.setGroup("test");
    model.setName("test-artifact");
    model.setVersion("1.0");

    List<AssetXO> result = underTest.readComponentAssets(groupRepo, model);

    assertThat(result.size(), is(1));
    AssetXO assetXO = result.get(0);
    assertThat(assetXO.getRepositoryName(), is("maven-public"));
    assertThat(assetXO.getContainingRepositoryName(), is("maven-public"));
    // Verify CONTENT_LAST_MODIFIED stripped because source repo (maven-hosted) is HostedType
    assertThat(((Map) assetXO.getAttributes().get("content")).containsKey("last_modified"), is(false));
  }

  @Test
  public void toAssetXO_usesSourceRepositoryForHostedTypeCheck() {
    // When accessing via group repo (maven-public), the HostedType check should use
    // the source repository (maven-hosted) to determine whether to strip CONTENT_LAST_MODIFIED

    Repository sourceRepo = mock(Repository.class);
    when(sourceRepo.getName()).thenReturn("maven-hosted");
    when(sourceRepo.getType()).thenReturn(new HostedType());
    when(repositoryManager.get("maven-hosted")).thenReturn(sourceRepo);

    // Group repo is checked by HostedType validation in toAssetXO but returns proxy type
    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("maven-public");
    when(groupRepo.getType()).thenReturn(new ProxyType());
    when(repositoryManager.get("maven-public")).thenReturn(groupRepo);

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    // sourceRepository is maven-hosted (HostedType), so CONTENT_LAST_MODIFIED should be stripped
    AssetXO assetXO = underTest.toAssetXO(
        "maven-public", // repositoryName (display name)
        "maven-public", // containingRepositoryName
        "maven-hosted", // sourceRepository (actual location)
        "maven2",
        createAsset("maven-hosted"),
        true);

    // CONTENT_LAST_MODIFIED should be removed because source repo is HostedType
    assertThat(((Map) assetXO.getAttributes().get("content")).containsKey("last_modified"), is(false));
  }

  @Test
  public void toAssetXO_preservesLastModifiedWhenSourceIsProxy() {
    // When source repository is proxy, CONTENT_LAST_MODIFIED should be preserved
    // even when accessing via group repo

    Repository sourceRepo = mock(Repository.class);
    when(sourceRepo.getName()).thenReturn("maven-proxy");
    when(sourceRepo.getType()).thenReturn(new ProxyType());
    when(repositoryManager.get("maven-proxy")).thenReturn(sourceRepo);

    Repository groupRepo = mock(Repository.class);
    when(groupRepo.getName()).thenReturn("maven-public");
    when(groupRepo.getType()).thenReturn(new ProxyType());
    when(repositoryManager.get("maven-public")).thenReturn(groupRepo);

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    AssetXO assetXO = underTest.toAssetXO(
        "maven-public", // repositoryName (display name)
        "maven-public", // containingRepositoryName
        "maven-proxy", // sourceRepository (actual location - proxy)
        "maven2",
        createAsset("maven-proxy"),
        true);

    // CONTENT_LAST_MODIFIED should be preserved because source repo is ProxyType
    Map<String, Object> contentMap = (Map<String, Object>) assetXO.getAttributes().get("content");
    assertThat(contentMap.containsKey("last_modified"), is(true));
    assertThat(contentMap.get("last_modified"), is("2023-11-13T16:00:20.450+02:00"));
  }

  @Test
  public void previewAssets_emptyPermittedSet_noContentFacetInteractions() {
    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    // Caller has no browse permission on the sole selected repository. No stubs on the repo
    // beyond identity — the filter drops it before expansion, so production must never touch
    // its type, facet, or format.
    Repository leaf = mock(Repository.class);
    doReturn(Collections.emptyList()).when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());

    RepositorySelector selector = RepositorySelector.fromSelector("*");
    var result = underTest.previewAssets(selector, List.of(leaf), "path =^ \"/x\"",
        new QueryOptions(null, null, null, 0, 10));

    assertThat(result.getTotal(), is(0L));
    assertThat(result.getResults().isEmpty(), is(true));
    verifyNoInteractions(selectorFactory);
    // Filter runs pre-expansion — an unauthorised input is dropped without any inspection.
    verify(leaf, never()).getType();
    verify(leaf, never()).getFormat();
  }

  @Test
  public void previewAssets_dropsUnpermittedInputBeforeGroupExpansion() {
    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    // Caller passes a permitted hosted plus an unpermitted group. Only the hosted survives
    // the browse filter — its leaves must never be queried. The group is filtered out entirely,
    // so its leaves are never expanded.
    Repository permittedHosted = mock(Repository.class);
    when(permittedHosted.getType()).thenReturn(new HostedType());

    Repository forbiddenLeaf = mock(Repository.class);
    Repository forbiddenGroup = mock(Repository.class);
    // No when(forbiddenGroup.getType()) stub — production must never reach getType() on a
    // filtered-out repository. Same reasoning for the group facet and its leaves.

    doReturn(List.of(permittedHosted)).when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());

    RepositorySelector selector = RepositorySelector.fromSelector("*");
    // The permitted hosted is a bare mock without a ContentFacet stack, so iterating it into the
    // preview body will NPE on r.getFormat().getValue(). Asserting on that exact type keeps the
    // test tight: a bug in the permission-filter code that throws something else won't sneak past.
    assertThrows(NullPointerException.class,
        () -> underTest.previewAssets(selector, List.of(permittedHosted, forbiddenGroup), "path =^ \"/y\"",
            new QueryOptions(null, null, null, 0, 10)));

    // The auth check ran on the caller's input set (pre-expansion), so a forbidden group never
    // gets its leaves enumerated — expansion happens only for permitted inputs.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<Repository>> checkerInput = ArgumentCaptor.forClass(Iterable.class);
    verify(repositoryPermissionChecker).userCanBrowseRepositories(checkerInput.capture());
    assertThat(checkerInput.getValue(), containsInAnyOrder(permittedHosted, forbiddenGroup));
    // The forbidden group's facet was never touched — the security property under test. If the
    // filter ever regressed to post-expansion, we'd have called forbiddenGroup.getType() first.
    verify(forbiddenGroup, never()).getType();
    verify(forbiddenLeaf, never()).getType();
    // Permitted hosted was iterated at all (sanity check).
    verify(permittedHosted).getFormat();
  }

  @Test
  public void previewAssets_groupAccessCarriesThroughToLeaves() {
    // The caller's browse permission on the group entitles preview of every leaf under it —
    // Nexus's group permission model. RepositoryPermissionChecker.userCanBrowseRepositories
    // only checks direct RepositoryViewPermission, so if we ran that filter AFTER group
    // expansion the leaves would be dropped even when the group is authorised. The helper
    // therefore filters BEFORE expansion; this test pins that invariant.
    Repository leaf1 = mock(Repository.class);
    when(leaf1.getType()).thenReturn(new HostedType());
    Repository leaf2 = mock(Repository.class);
    when(leaf2.getType()).thenReturn(new HostedType());
    Repository group = mock(Repository.class);
    when(group.getType()).thenReturn(new GroupType());
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(group.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(groupFacet.leafMembers()).thenReturn(List.of(leaf1, leaf2));

    // Realistic checker: only inputs containing the group survive (direct permission).
    // Leaves handed to the checker directly would be dropped — group mediation is not
    // recognised by userCanBrowseRepositories. This differentiates pre- vs post-expansion:
    // pre-expansion the checker sees [group] and passes; post-expansion it would see
    // [leaf1, leaf2] and return empty — reproducing the reported bug.
    doAnswer(inv -> {
      java.util.List<Repository> permitted = new java.util.ArrayList<>();
      for (Repository r : inv.<Iterable<Repository>>getArgument(0)) {
        if (r == group) {
          permitted.add(r);
        }
      }
      return permitted;
    }).when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    RepositorySelector selector = RepositorySelector.fromSelector("*");
    // The first leaf is a bare mock without a Format stack, so iterating it into the preview
    // body will NPE on r.getFormat().getValue(). Asserting on that exact type keeps the test
    // tight: a bug in the permission-filter code that throws something else won't sneak past.
    // (LinkedHashSet iteration order is insertion order, so leaf1 is visited first.)
    assertThrows(NullPointerException.class, () -> underTest.previewAssets(selector, List.of(group), "path =^ \"/z\"",
        new QueryOptions(null, null, null, 0, 10)));

    // The filter received the caller's input set (not the expanded leaves), so group-mediated
    // access survives — Layer A already validated the group, and the group entitles all leaves.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<Repository>> checkerInput = ArgumentCaptor.forClass(Iterable.class);
    verify(repositoryPermissionChecker).userCanBrowseRepositories(checkerInput.capture());
    assertThat(checkerInput.getValue(), containsInAnyOrder(group));
    // Expansion happened after the Layer-B pass — leaves came out of the group.
    verify(groupFacet).leafMembers();
    // The first leaf reached the preview body: proves the loop iterated the expanded set.
    // With the pre-fix (post-expansion filter), the realistic checker above would have dropped
    // both leaves and the loop would never have run — no getFormat() call at all.
    verify(leaf1).getFormat();
  }

  /**
   * NEXUS-55062 regression test. The npm Attributes panel lost description/author/license/keywords in 3.93.0 when the
   * REST allow-list from {@link AssetXODescriptor#listExposedAttributeKeys()} started gating the UI response. The UI
   * must show every stored format attribute unless the descriptor explicitly opts into filtering.
   */
  @Test
  public void toAssetXO_withDescriptor_retainsAllStoredFormatAttributes() {
    AssetXODescriptor npmDescriptor = descriptorFor(Set.of("name", "version"), false);
    ContentComponentHelper underTest = helperWithDescriptor("npm", npmDescriptor);

    AssetXO assetXO = underTest.toAssetXO("npm-hosted", "npm-hosted", "npm-hosted", "npm", createNpmAsset(), true);

    Map<String, Object> npmAttributes = (Map<String, Object>) assetXO.getAttributes().get("npm");
    assertThat(npmAttributes.keySet(),
        containsInAnyOrder("name", "version", "description", "author", "license", "keywords", "asset_kind"));
    assertThat(npmAttributes.get("description"), is("Configuration file loader"));
  }

  @Test
  public void toAssetXO_withDescriptorOptingIntoFiltering_restrictsToExposedKeys() {
    AssetXODescriptor filtering = descriptorFor(Set.of("name", "version"), true);
    ContentComponentHelper underTest = helperWithDescriptor("npm", filtering);

    AssetXO assetXO = underTest.toAssetXO("npm-hosted", "npm-hosted", "npm-hosted", "npm", createNpmAsset(), true);

    Map<String, Object> npmAttributes = (Map<String, Object>) assetXO.getAttributes().get("npm");
    assertThat(npmAttributes.keySet(), containsInAnyOrder("name", "version", "asset_kind"));
  }

  /**
   * Docker relies on transformAttributeValue to render totalSize human-readably. It must run on every retained key,
   * not only allow-listed ones, now that the UI is unfiltered.
   */
  @Test
  public void toAssetXO_appliesTransformAttributeValueToNonExposedKeys() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name"), false);
    when(descriptor.transformAttributeValue(anyString(), any()))
        .thenAnswer(inv -> "transformed:" + inv.getArgument(1));
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);

    AssetXO assetXO = underTest.toAssetXO("npm-hosted", "npm-hosted", "npm-hosted", "npm", createNpmAsset(), true);

    Map<String, Object> npmAttributes = (Map<String, Object>) assetXO.getAttributes().get("npm");
    // "description" is outside the allow-list, so this only passes if the transform runs on non-exposed keys too
    assertThat(npmAttributes.get("description"), is("transformed:Configuration file loader"));
  }

  @Test
  public void toAssetXO_asset_kind_isPresentAndNotTransformed() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name", "version"), false);
    when(descriptor.transformAttributeValue(anyString(), any())).thenAnswer(inv -> {
      if ("asset_kind".equals(inv.getArgument(0))) {
        throw new IllegalStateException("asset_kind must not be passed to transformAttributeValue");
      }
      return inv.getArgument(1);
    });
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);

    AssetXO assetXO = underTest.toAssetXO("npm-hosted", "npm-hosted", "npm-hosted", "npm", createNpmAsset(), true);

    Map<String, Object> npmAttributes = (Map<String, Object>) assetXO.getAttributes().get("npm");
    assertThat(npmAttributes.get("asset_kind"), is("TARBALL"));
  }

  /**
   * Guards the immutable-map path: when an asset has no stored format attributes the helper builds a
   * {@link Collections#singletonMap}. The filter block must skip it rather than try to iterate or mutate it.
   */
  @Test
  public void toAssetXO_withoutStoredFormatAttributes_producesSingletonAssetKindMap() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name", "version"), false);
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);

    AssetXO assetXO =
        underTest.toAssetXO("npm-hosted", "npm-hosted", "npm-hosted", "npm", createAssetWithoutFormatAttributes(),
            true);

    Map<String, Object> npmAttributes = (Map<String, Object>) assetXO.getAttributes().get("npm");
    assertThat(npmAttributes.keySet(), containsInAnyOrder("asset_kind"));
  }

  /**
   * AttributesMap.backing() hands back the live entity map and the helper only shallow-copies it, so filtering must
   * never write through to the stored attributes.
   */
  @Test
  public void toAssetXO_withDescriptor_doesNotMutateStoredFormatAttributesBeyondAssetKind() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name", "version"), true);
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);
    Asset asset = createNpmAsset();
    Map<String, Object> storedNpm = (Map<String, Object>) asset.attributes().backing().get("npm");

    underTest.toAssetXO("npm-hosted", "npm-hosted", "npm-hosted", "npm", asset, true);

    // asset_kind is injected into the live map by the helper (pre-existing behaviour); nothing may be dropped
    assertThat(storedNpm.keySet(),
        containsInAnyOrder("name", "version", "description", "author", "license", "keywords", "asset_kind"));
  }

  @Test
  public void toComponentXO_withDescriptor_retainsAllStoredFormatAttributes() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name", "version"), false);
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);

    ComponentXO componentXO = underTest.toComponentXO("npm-hosted", "npm", createNpmComponent());

    Map<String, Object> npmAttributes = (Map<String, Object>) componentXO.getAttributes().get("npm");
    assertThat(npmAttributes.keySet(),
        containsInAnyOrder("name", "version", "description", "license", "component_kind"));
  }

  @Test
  public void toComponentXO_appliesTransformAttributeValue() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name"), false);
    when(descriptor.transformAttributeValue(anyString(), any()))
        .thenAnswer(inv -> "transformed:" + inv.getArgument(1));
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);

    ComponentXO componentXO = underTest.toComponentXO("npm-hosted", "npm", createNpmComponent());

    Map<String, Object> npmAttributes = (Map<String, Object>) componentXO.getAttributes().get("npm");
    assertThat(npmAttributes.get("description"), is("transformed:Configuration file loader"));
  }

  /**
   * Completes the component-path matrix: the opt-in filtering branch must restrict to the allow-list, mirroring the
   * asset path.
   */
  @Test
  public void toComponentXO_withDescriptorOptingIntoFiltering_restrictsToExposedKeys() {
    AssetXODescriptor descriptor = descriptorFor(Set.of("name", "version"), true);
    ContentComponentHelper underTest = helperWithDescriptor("npm", descriptor);

    ComponentXO componentXO = underTest.toComponentXO("npm-hosted", "npm", createNpmComponent());

    Map<String, Object> npmAttributes = (Map<String, Object>) componentXO.getAttributes().get("npm");
    assertThat(npmAttributes.keySet(), containsInAnyOrder("name", "version", "component_kind"));
  }

  private AssetXODescriptor descriptorFor(final Set<String> exposedKeys, final boolean filterUi) {
    AssetXODescriptor descriptor = mock(AssetXODescriptor.class);
    when(descriptor.listExposedAttributeKeys()).thenReturn(exposedKeys);
    when(descriptor.filterUiAttributes()).thenReturn(filterUi);
    when(descriptor.transformAttributeValue(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));
    return descriptor;
  }

  private ContentComponentHelper helperWithDescriptor(final String format, final AssetXODescriptor descriptor) {
    when(repositoryManager.get(ArgumentMatchers.anyString())).thenReturn(repository);
    when(repository.getType()).thenReturn(new ProxyType());
    // buildQualifierBeanMap is statically mocked in setUp; route the descriptor list to the format key
    when(QualifierUtil.buildQualifierBeanMap(anyList()))
        .thenReturn(Map.of("default", componentFinder))
        .thenReturn(Map.of(format, descriptor));

    return new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        List.of(descriptor));
  }

  private Asset createNpmAsset() {
    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    when(asset.path()).thenReturn("/config-loader/-/config-loader-1.5.0.tgz");
    Map<String, Object> npmMap = new HashMap<>();
    npmMap.put("name", "config-loader");
    npmMap.put("version", "1.5.0");
    npmMap.put("description", "Configuration file loader");
    npmMap.put("author", "Test User");
    npmMap.put("license", "MIT");
    npmMap.put("keywords", "config json env");
    Map<String, Object> backingMap = new HashMap<>();
    backingMap.put("npm", npmMap);
    NestedAttributesMap attrs = mock(NestedAttributesMap.class);
    when(attrs.backing()).thenReturn(backingMap);
    when(asset.attributes()).thenReturn(attrs);
    when(asset.kind()).thenReturn("TARBALL");
    when(asset.created()).thenReturn(OffsetDateTime.now());
    when(asset.lastUpdated()).thenReturn(OffsetDateTime.now());
    AssetData assetData = new AssetData();
    assetData.setAssetId(1);
    when(asset.unwrap()).thenReturn(assetData);
    Repository assetRepository = mock(Repository.class);
    when(assetRepository.getName()).thenReturn("npm-hosted");
    when(asset.repository()).thenReturn(assetRepository);
    return asset;
  }

  private Asset createAssetWithoutFormatAttributes() {
    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    when(asset.path()).thenReturn("/config-loader");
    NestedAttributesMap attrs = mock(NestedAttributesMap.class);
    when(attrs.backing()).thenReturn(new HashMap<>());
    when(asset.attributes()).thenReturn(attrs);
    when(asset.kind()).thenReturn("PACKAGE_ROOT");
    when(asset.created()).thenReturn(OffsetDateTime.now());
    when(asset.lastUpdated()).thenReturn(OffsetDateTime.now());
    AssetData assetData = new AssetData();
    assetData.setAssetId(2);
    when(asset.unwrap()).thenReturn(assetData);
    Repository assetRepository = mock(Repository.class);
    when(assetRepository.getName()).thenReturn("npm-hosted");
    when(asset.repository()).thenReturn(assetRepository);
    return asset;
  }

  private FluentComponentImpl createNpmComponent() {
    FluentComponentImpl component = mock(FluentComponentImpl.class);
    when(component.name()).thenReturn("config-loader");
    when(component.version()).thenReturn("1.5.0");
    when(component.namespace()).thenReturn("");
    when(component.kind()).thenReturn("NPM_PACKAGE");
    Map<String, Object> npmMap = new HashMap<>();
    npmMap.put("name", "config-loader");
    npmMap.put("version", "1.5.0");
    npmMap.put("description", "Configuration file loader");
    npmMap.put("license", "MIT");
    Map<String, Object> backingMap = new HashMap<>();
    backingMap.put("npm", npmMap);
    NestedAttributesMap attrs = mock(NestedAttributesMap.class);
    when(attrs.backing()).thenReturn(backingMap);
    when(component.attributes()).thenReturn(attrs);
    ComponentData componentData = new ComponentData();
    componentData.setComponentId(1);
    when(component.unwrap()).thenReturn(componentData);
    return component;
  }

  private Asset createAsset() {
    return createAsset("maven-hosted");
  }

  private Asset createAsset(final String repositoryName) {

    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    when(asset.path()).thenReturn("/org/apache/logging/log4j/log4j-core/maven-metadata.xml");
    Map<String, String> contentMap = new HashMap<>();
    contentMap.put("last_modified", "2023-11-13T16:00:20.450+02:00");
    Map<String, Object> backingMap = new HashMap<>();
    backingMap.put("content", contentMap);
    when(assetAttributes.backing()).thenReturn(backingMap);
    when(asset.attributes()).thenReturn(assetAttributes);
    when(asset.kind()).thenReturn("REPOSITORY_METADATA");
    OffsetDateTime blobCreated = OffsetDateTime.now();
    when(blob.blobCreated()).thenReturn(blobCreated);
    OffsetDateTime assetCreated = OffsetDateTime.now();
    when(asset.created()).thenReturn(assetCreated);
    when(asset.lastUpdated()).thenReturn(assetCreated);
    AssetData assetData = new AssetData();
    assetData.setAssetId(1);
    when(asset.unwrap()).thenReturn(assetData);

    // Mock repository() for sourceRepository lookup
    Repository assetRepository = mock(Repository.class);
    when(assetRepository.getName()).thenReturn(repositoryName);
    when(asset.repository()).thenReturn(assetRepository);

    return asset;
  }

  @Test
  public void toAssetXO_blobUpdatedIsNullWhenBlobMissing() {
    // When the asset has no blob (e.g. a proxy asset whose content has not been fetched, or a
    // metadata-only asset), blobUpdated is null and the UI renders the dash placeholder. This
    // mirrors AssetSearchResultImpl.getBinaryUpdated, which returns null with no fallback. When a
    // blob IS present, blobUpdated tracks blobCreated — see
    // toAssetXO_blobUpdatedReflectsBlobCreatedWhenBlobPresent.
    when(repositoryManager.get("raw-hosted")).thenReturn(repository);
    when(repository.getType()).thenReturn(new HostedType());

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    when(asset.path()).thenReturn("/file.txt");
    NestedAttributesMap attrs = mock(NestedAttributesMap.class);
    when(attrs.backing()).thenReturn(new HashMap<>());
    when(asset.attributes()).thenReturn(attrs);
    when(asset.kind()).thenReturn("");
    when(asset.created()).thenReturn(OffsetDateTime.now());
    AssetData assetData = new AssetData();
    assetData.setAssetId(99);
    when(asset.unwrap()).thenReturn(assetData);
    Repository assetRepository = mock(Repository.class);
    when(assetRepository.getName()).thenReturn("raw-hosted");
    when(asset.repository()).thenReturn(assetRepository);

    AssetXO assetXO = underTest.toAssetXO(
        "raw-hosted", "raw-hosted", "raw-hosted", "raw", asset, true);

    assertThat(assetXO.getBlobUpdated(), is(nullValue()));
  }

  @Test
  public void toAssetXO_blobUpdatedReflectsBlobCreatedWhenBlobPresent() {
    // With a blob present, blobUpdated must come from the (immutable) blob's creation time, not
    // asset.lastUpdated(). asset.lastUpdated() is bumped by metadata writes such as proxy cache
    // re-verification, which must not move the "Updated" value when the binary is unchanged.
    when(repositoryManager.get("raw-hosted")).thenReturn(repository);
    when(repository.getType()).thenReturn(new HostedType());

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        contentDeletionService,
        List.of(componentFinder),
        assetPermissionChecker,
        selectorFactory,
        repositoryManager,
        repositoryPermissionChecker,
        Collections.emptyList());

    OffsetDateTime blobCreated = OffsetDateTime.now().minusHours(2);
    // lastUpdated is much later than blobCreated, as it would be after a cache re-verification.
    OffsetDateTime lastUpdated = OffsetDateTime.now();
    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    when(asset.path()).thenReturn("/file.txt");
    NestedAttributesMap attrs = mock(NestedAttributesMap.class);
    when(attrs.backing()).thenReturn(new HashMap<>());
    when(asset.attributes()).thenReturn(attrs);
    when(asset.kind()).thenReturn("");
    when(asset.created()).thenReturn(blobCreated);
    when(asset.lastUpdated()).thenReturn(lastUpdated);
    when(asset.blob()).thenReturn(Optional.of(blob));
    when(blob.blobCreated()).thenReturn(blobCreated);
    when(blob.blobRef()).thenReturn(mock(BlobRef.class));
    when(blob.blobSize()).thenReturn(1024L);
    when(blob.contentType()).thenReturn("text/plain");
    when(blob.checksums()).thenReturn(Collections.emptyMap());
    AssetData assetData = new AssetData();
    assetData.setAssetId(100);
    when(asset.unwrap()).thenReturn(assetData);
    Repository assetRepository = mock(Repository.class);
    when(assetRepository.getName()).thenReturn("raw-hosted");
    when(asset.repository()).thenReturn(assetRepository);

    AssetXO assetXO = underTest.toAssetXO(
        "raw-hosted", "raw-hosted", "raw-hosted", "raw", asset, false);

    assertThat(assetXO.getBlobUpdated(), is(notNullValue()));
    assertThat(assetXO.getBlobUpdated().toInstant().toEpochMilli(),
        is(blobCreated.toInstant().toEpochMilli()));
  }
}
