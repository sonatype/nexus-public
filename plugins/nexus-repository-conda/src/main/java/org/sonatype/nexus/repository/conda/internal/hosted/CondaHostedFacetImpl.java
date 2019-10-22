/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.conda.internal.hosted;

import com.google.common.collect.Streams;
import groovy.json.JsonOutput;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.conda.internal.hosted.metadata.PackageDesc;
import org.sonatype.nexus.repository.conda.internal.hosted.metadata.RepoData;
import org.sonatype.nexus.repository.conda.internal.util.CondaDataAccess;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.conda.internal.hosted.Utils.toAttributes;
import static org.sonatype.nexus.repository.conda.internal.hosted.metadata.MetaData.asIndex;
import static org.sonatype.nexus.repository.conda.internal.hosted.metadata.MetaData.readIndexJson;
import static org.sonatype.nexus.repository.conda.internal.util.CondaDataAccess.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.conda.internal.util.CondaDataAccess.toContent;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * {@link CondaHostedFacetImpl implementation}
 */
@Named
public class CondaHostedFacetImpl
        extends FacetSupport
        implements CondaHostedFacet {
    private final CondaDataAccess condaDataAccess;

    @Inject
    public CondaHostedFacetImpl(final CondaDataAccess condaDataAccess) {
        this.condaDataAccess = checkNotNull(condaDataAccess);
    }

    @Override
    protected void doInit(Configuration configuration) throws Exception {
        super.doInit(configuration);
        getRepository().facet(StorageFacet.class).registerWritePolicySelector(new CondaWritePolicySelector());
    }

    @Override
    @TransactionalTouchBlob
    public Optional<Content> fetch(final String path) {
        checkNotNull(path);

        StorageTx tx = UnitOfWork.currentTx();
        Asset maybeAsset = condaDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);

        return Optional.ofNullable(maybeAsset)
                .map(asset -> {
                    if (asset.markAsDownloaded()) {
                        tx.saveAsset(asset);
                    }
                    return asset;
                })
                .map(asset -> toContent(asset, tx.requireBlob(asset.requireBlobRef())));
    }

    @Override
    @TransactionalStoreBlob
    public Content upload(final String path, final Payload payload) throws IOException {
        checkNotNull(path);
        checkNotNull(payload);
        StorageFacet storageFacet = facet(StorageFacet.class);
        try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HASH_ALGORITHMS)) {
            return doPutContent(path, tempBlob, payload);
        }
    }

    @Override
    @TransactionalDeleteBlob
    public boolean delete(final String path) {
        checkNotNull(path);

        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(getRepository());

        Asset asset = condaDataAccess.findAsset(tx, bucket, path);
        if (asset == null) {
            return false;
        }
        tx.deleteAsset(asset);
        return true;
    }

    protected Content doPutContent(final String path, final TempBlob tempBlob, final Payload payload) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        CondaPath condaPath = CondaPath.build(path);
        Asset asset = getOrCreateAsset(condaPath, getRepository());

        AttributesMap contentAttributes = null;
        if (payload instanceof Content) {
            contentAttributes = ((Content) payload).getAttributes();
        }

        Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));

        AssetBlob assetBlob = tx.setBlob(asset, path, tempBlob, null, payload.getContentType(), false);

        Content content = toContent(asset, assetBlob.getBlob());
        if (!path.contains("repodata.json")) {
            String json = readIndexJson(content.openInputStream());
            toAttributes(asIndex(json), asset.formatAttributes());
        }
        tx.saveAsset(asset);
        return content;
    }

    @Override
    @Transactional
    public void rebuildRepoDataJson() throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        final Bucket bucket = tx.findBucket(getRepository());

        Map<String, RepoData> architectures = new HashMap<>();
        StreamSupport
                .stream(tx.browseAssets(bucket).spliterator(), false)
                .filter(asset -> !asset.name().endsWith("repodata.json"))
                .filter(asset -> tx.findComponent(asset.componentId()) != null)
                .map(Utils::toPackageDesc)
                .forEach(tuple -> {
                    PackageDesc index = tuple.getSecond();
                    String fileName = tuple.getFirst();
                    takeOrCreate(architectures, index.getArch())
                            .getPackages()
                            .put(fileName, index);
                });

        for (Map.Entry<String, RepoData> entry : architectures.entrySet()) {
            String payload = JsonOutput.toJson(entry.getValue());

            log.debug("Building repodata.json for " + entry.getKey() + " json:\n" + payload);

            Content content = new Content(new StringPayload(payload, ContentTypes.TEXT_PLAIN));
            upload(entry.getKey() + "/repodata.json", content);
        }
    }

    private RepoData takeOrCreate(Map<String, RepoData> architectures, String group) {
        if (!architectures.containsKey(group)) {
            architectures.put(group, createRepoData(group));
        }
        return architectures.get(group);
    }

    private RepoData createRepoData(String arch) {
        RepoData repoData = new RepoData();
        repoData.getInfo().setSubdir(arch);
        return repoData;
    }

    @TransactionalStoreMetadata
    private Asset getOrCreateAsset(final CondaPath condaPath, final Repository repository) {
        final String componentName = condaPath.getCoordinates()
                .map(coordinates -> coordinates.getPackageName())
                .orElse(condaPath.getFileName());

        final String componentGroup = getGroup(condaPath.getPath());
        final StorageTx tx = UnitOfWork.currentTx();
        final Bucket bucket = tx.findBucket(getRepository());
        String path = condaPath.getPath();

        log.debug("Find component with name " + componentName + " and path: " + path);
        Asset asset = findComponent(tx, repository, condaPath)
                .map(component -> {
                            // UPDATE
                            log.debug("Component exists: " + component.group() + " - " + component.name());
                            log.debug("Find asset " + path);
                            return Optional
                                    .ofNullable(tx.findAssetWithProperty(P_NAME, path, component))
                                    .orElseGet(() -> {
                                        log.info("Asset doesn't exist.  Create it");
                                        return tx.createAsset(bucket, component);
                                    });
                        }
                )
                .orElseGet(() -> {
                    // CREATE
                    log.debug("Create new component and asset");
                    Component component = condaPath.getCoordinates()
                            .map(coordinates -> tx.createComponent(bucket, getRepository().getFormat())
                                    .group(componentGroup)
                                    .name(componentName)
                                    .version(coordinates.getVersion())
                            ).orElse(tx.createComponent(bucket, getRepository().getFormat())
                                    .group(componentGroup)
                                    .name(componentName));
                    tx.saveComponent(component);
                    return tx.createAsset(bucket, component);
                });

        asset.name(condaPath.getPath());
        asset.markAsDownloaded();
        return asset;
    }


    private static Optional<Component> findComponent(final StorageTx tx,
                                                     final Repository repository,
                                                     final CondaPath condaPath) {
        Query query = condaPath.getCoordinates()
                .map(coordinates ->
                        Query.builder()
                                .where(P_GROUP).eq(getGroup(condaPath.getPath()))
                                .and(P_NAME).eq(coordinates.getPackageName())
                                .and(P_VERSION).eq(coordinates.getVersion())
                                .build())
                .orElse(Query.builder()
                        .where(P_GROUP).eq(getGroup(condaPath.getPath()))
                        .and(P_NAME).eq(condaPath.getFileName())
                        .build());

        return Streams
                .stream(tx.findComponents(query, singletonList(repository)))
                .findFirst();
    }

    private static String getGroup(String path) {
        StringBuilder group = new StringBuilder();
        int i = path.lastIndexOf("/");
        if (i != -1) {
            group.append(path.substring(0, i));
        }
        return group.toString();
    }

}
