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
package org.sonatype.nexus.repository.sizeassetcount;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.types.GroupType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.sizeassetcount.SizeAssetCountAttributesFacetImpl.*;

public class SizeAssetCountAttributesFacetImplTest extends TestSupport{


    @Mock
    private RepositoryManager repositoryManager;

    @Mock
    private Configuration configuration;

    @Mock
    private StorageFacet storageFacet;

    @Mock
    private Bucket bucket;

    @Mock
    private StorageTx storageTx;

    @Mock
    private Supplier<StorageTx> supplier;

    @Mock
    private GroupType groupType;

    private Repository initRepository() throws Exception {
        Repository repository = mock(Repository.class);

        repository.attach(storageFacet);

        when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
        when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
        when(storageFacet.txSupplier()).thenReturn(supplier);
        when(storageFacet.txSupplier().get()).thenReturn(storageTx);
        when(repository.getName()).thenReturn("MY-MAVEN-REPO");


        return repository;
    }

    private void initConfiguration(Repository repository) throws Exception {
        Map<String, Map<String, Object>> attributes = new HashMap<>();
        Map<String, Object> valueOfFirstKey = new HashMap<>();
        valueOfFirstKey.put("blobStoreName", "blobstoreTest");
        attributes.put("storage", valueOfFirstKey);
        when(configuration.getRepositoryName()).thenReturn("MY-MAVEN-REPO");
        when(configuration.getRecipeName()).thenReturn("mavenRecipeTest");
        when(configuration.getAttributes()).thenReturn(attributes);

        when(repository.getConfiguration()).thenReturn(configuration);
    }

    private Repository initRepositoryWithoutStorageFacet() {
        Repository repository = mock(Repository.class);
        Map<String, Map<String, Object>> attributes = new HashMap<>();
        Map<String, Object> valueOfFirstKey = new HashMap<>();
        valueOfFirstKey.put("blobStoreName", "blobstoreTest");
        attributes.put("storage", valueOfFirstKey);
        when(configuration.getRepositoryName()).thenReturn("MY-MAVEN-REPO");
        when(configuration.getRecipeName()).thenReturn("mavenRecipeTest");
        when(configuration.getAttributes()).thenReturn(attributes);
        when(repository.getName()).thenReturn("MY-MAVEN-REPO");
        when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.empty());
        when(repository.getConfiguration()).thenReturn(configuration);


        return repository;
    }

    private Repository initRepositoryWithAssets(Asset... assets) throws Exception {
        Repository repository = mock(Repository.class);
        Map<String, Map<String, Object>> attributes = new HashMap<>();
        Map<String, Object> valueOfFirstKey = new HashMap<>();
        valueOfFirstKey.put("blobStoreName", "blobstoreTest");
        attributes.put("storage", valueOfFirstKey);
        when(configuration.getRepositoryName()).thenReturn("MY-MAVEN-REPO");
        when(configuration.getRecipeName()).thenReturn("mavenRecipeTest");
        when(configuration.getAttributes()).thenReturn(attributes);

        when(repository.getName()).thenReturn("MY-MAVEN-REPO");
        when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
        when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
        when(storageFacet.txSupplier()).thenReturn(supplier);
        when(storageFacet.txSupplier().get()).thenReturn(storageTx);

        repository.init(configuration);
        repository.attach(storageFacet);

        when(storageTx.findBucket(repository)).thenReturn(bucket);
        when(storageTx.browseAssets(bucket)).thenReturn(Lists.newArrayList(assets));

        when(storageTx.countAssets(Matchers.any(Query.class), Matchers.any(Iterable.class))).thenReturn(2L);



        return repository;
    }

    private Repository groupRepository(final String name, final Repository... repositories) {
        Repository groupRepository = mock(Repository.class);
        when(groupRepository.getType()).thenReturn(groupType);
        when(groupRepository.getName()).thenReturn(name);
        when(repositoryManager.get(name)).thenReturn(groupRepository);
        GroupFacet groupFacet = mock(GroupFacet.class);
        when(groupRepository.facet(GroupFacet.class)).thenReturn(groupFacet);
        when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
        when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
        when(groupRepository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));

        when(groupFacet.members()).thenReturn(copyOf(repositories));
        return groupRepository;
    }

    private Asset mockAsset(String name, long size) {
        Asset asset = mock(Asset.class);
        when(asset.name()).thenReturn(name);
        when(asset.size()).thenReturn(size);
        return asset;
    }

    @Before
    public void setUp() {
    }

    @Test
    public void should_return_0_for_an_empty_repository() throws Exception {
        //Given
        /**
         * Un repository vide
         */
        Repository repository = initRepository();
        initConfiguration(repository);

        //When
        /**
         * Appel de la méthode de comptage de la taille et du blob count
         */
        SizeAssetCountAttributesFacet sizeAssetCountAttributesFacet = new SizeAssetCountAttributesFacetImpl();
        sizeAssetCountAttributesFacet.attach(repository);
        sizeAssetCountAttributesFacet.calculateSizeAssetCount();

        //Then
        /**
         * Return 0 for size and 0 for assetcount
         */
        assertThat(repository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.SIZE_KEY)).isEqualTo(0L);
        assertThat(repository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.ASSET_COUNT_KEY)).isEqualTo(0L);

    }

    @Test
    public void should_return_0_for_a_repository_with_no_storage_facet() throws Exception {
        //Given
        /**
         * Un repository vide
         */
        Repository repository = initRepositoryWithoutStorageFacet();

        //When
        /**
         * Appel de la méthode de comptage de la taille et du blob count
         */
        SizeAssetCountAttributesFacet sizeAssetCountAttributesFacet = new SizeAssetCountAttributesFacetImpl();
        sizeAssetCountAttributesFacet.attach(repository);
        sizeAssetCountAttributesFacet.calculateSizeAssetCount();
        //Then
        /**
         * Return 0 for size and 0 for assetcount
         */
        assertThat(repository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.SIZE_KEY)).isEqualTo(0L);
        assertThat(repository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.ASSET_COUNT_KEY)).isEqualTo(0L);
    }

    @Test
    public void should_return_the_size_and_the_asset_count_of_the_assets_for_a_repository_which_contains_just_those_assets() throws Exception {

        //Given
        /**
         * Un repository avec des assets
         */
        Asset asset1 = mockAsset("org.edf.test:1.0", 1500);
        Asset asset2 = mockAsset("org.edf.openam:1.0", 2500);
        Repository repository = initRepositoryWithAssets(asset1, asset2);
        initConfiguration(repository);
        Map<String, Object> backing = new HashMap<>();
        backing.put(SIZE_KEY, 4000L);
        backing.put(ASSET_COUNT_KEY, 2);
        configuration.getAttributes().put(SIZE_ASSET_COUNT_KEY_ATTRIBUTES, backing);

        //When
        /**
         * Appel de la méthode de comptage de la taille et du blob count
         */
        SizeAssetCountAttributesFacet sizeAssetCountAttributesFacet = new SizeAssetCountAttributesFacetImpl();
        sizeAssetCountAttributesFacet.attach(repository);
        sizeAssetCountAttributesFacet.calculateSizeAssetCount();

        //Then
        /**
         * Return the size and the blob count of the two assets
         */
        assertThat(repository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.SIZE_KEY)).isEqualTo(4000L);
        assertThat(repository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.ASSET_COUNT_KEY)).isEqualTo(2L);
    }

    @Test
    public void should_return_the_repository_attributes_of_a_group_repository() throws Exception {
        //Given
        Asset asset1 = mockAsset("org.edf.test:1.0", 1500);
        Asset asset2 = mockAsset("org.edf.openam:1.0", 2500);
        Repository repository = initRepositoryWithAssets(asset1, asset2);
        Asset asset3 = mockAsset("org.edf.test:2.0", 15000);
        Asset asset4 = mockAsset("org.edf.openam:2.0", 2500);
        Repository repository2 = initRepositoryWithAssets(asset3, asset4);
        Repository groupRepository = groupRepository("MY-REMO-MAVEN-GROUP", repository, repository2);
        Bucket groupBucket = mock(Bucket.class);
        when(storageTx.findBucket(groupRepository)).thenReturn(groupBucket);
        Map<String, Object> backing = new HashMap<>();
        backing.put(SIZE_KEY, 0L);
        backing.put(ASSET_COUNT_KEY, 0);
        NestedAttributesMap attributesMap = new NestedAttributesMap(SIZE_ASSET_COUNT_KEY_ATTRIBUTES, backing);
        when(groupBucket.attributes()).thenReturn(attributesMap);
        initConfiguration(groupRepository);


        //When
        /**
         * Appel de la méthode de comptage de la taille et du blob count
         */
        SizeAssetCountAttributesFacet sizeAssetCountAttributesFacet = new SizeAssetCountAttributesFacetImpl();
        sizeAssetCountAttributesFacet.attach(groupRepository);
        sizeAssetCountAttributesFacet.calculateSizeAssetCount();

        //Then
        /**
         * Return the size and the blob count of the group repository
         */
        assertThat(groupRepository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.SIZE_KEY)).isEqualTo(0L);
        assertThat(groupRepository.getConfiguration().getAttributes().
                get(SizeAssetCountAttributesFacetImpl.SIZE_ASSET_COUNT_KEY_ATTRIBUTES)
                .get(SizeAssetCountAttributesFacetImpl.ASSET_COUNT_KEY)).isEqualTo(0L);
    }
}
