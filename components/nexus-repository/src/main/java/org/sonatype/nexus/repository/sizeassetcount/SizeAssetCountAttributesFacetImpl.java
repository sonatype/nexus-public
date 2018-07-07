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

import com.google.common.collect.Streams;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @since 3.7.0
 */
@Named
public class SizeAssetCountAttributesFacetImpl extends FacetSupport implements SizeAssetCountAttributesFacet {

    @Inject
    private RepositoryManager repositoryManager;



    // Key for the attributes for Size and  Blob count
    public static final String SIZE_ASSET_COUNT_KEY_ATTRIBUTES = "sizeAssetCount";
    // Key for the data assetcount
    public static final String ASSET_COUNT_KEY = "assetCount";
    // Key for the data size
    public static final String SIZE_KEY = "size";


    public void calculateSizeAssetCount() {
        String repositoryName = getRepository().getName();
        log.debug("Repository name {} ", repositoryName);
        Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put(ASSET_COUNT_KEY,0L);
        attributesMap.put(SIZE_KEY,0L);
        if (optionalFacet(StorageFacet.class).isPresent()) {
            TransactionalStoreMetadata.operation.withDb(facet(StorageFacet.class).txSupplier()).call(() -> {
                StorageTx storageTx = UnitOfWork.currentTx();

                //First Get the bucket
                Bucket bucket = storageTx.findBucket(getRepository());

                //Assets of the bucket
                Iterable<Asset> assets = storageTx.browseAssets(bucket);
                if (assets != null) {
                    attributesMap.put(ASSET_COUNT_KEY, storageTx.countAssets(Query.builder().where("1").eq(1).build(), Collections.singletonList(getRepository())));
                    attributesMap.put(SIZE_KEY, Streams.stream(assets).mapToLong(Asset::size).sum());
                }

                return  null;
            });
        }
        Configuration configuration = getRepository().getConfiguration();
        Objects.requireNonNull(configuration.getAttributes())
                .put(SIZE_ASSET_COUNT_KEY_ATTRIBUTES, attributesMap);
        try {
            repositoryManager.update(configuration);
            log.debug("The attributes sizeAssetCount of the repository {} have been updated", repositoryName);
        } catch (Exception e) {
            log.error("Error occuring during the update of rhe repository {} ", repositoryName, e);
        }


    }
}