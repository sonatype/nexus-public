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
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.conda.internal.CondaFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.upload.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Named(CondaFormat.NAME)
@Singleton
public class CondaUploadHandler extends UploadHandlerSupport {

    private static final String PATH = "path";

    private final VariableResolverAdapter variableResolverAdapter;
    private final ContentPermissionChecker contentPermissionChecker;


    @Inject
    public CondaUploadHandler(
            final Set<UploadDefinitionExtension> uploadDefinitionExtensions,
            VariableResolverAdapter variableResolverAdapter,
            ContentPermissionChecker contentPermissionChecker) {
        super(uploadDefinitionExtensions);
        this.variableResolverAdapter = variableResolverAdapter;
        this.contentPermissionChecker = contentPermissionChecker;
    }

    @Override
    public UploadResponse handle(Repository repository, ComponentUpload componentUpload) throws IOException {

        CondaHostedFacet hostedFacet = repository.facet(CondaHostedFacet.class);
        StorageFacet storageFacet = repository.facet(StorageFacet.class);
        List<ContentAndAssetPath> responseData;

        UnitOfWork.begin(storageFacet.txSupplier());
        try {
            responseData = uploadAsset(hostedFacet, componentUpload.getAssetUploads());
        } finally {
            UnitOfWork.end();
        }

        List<Content> contents = responseData
                .stream()
                .map(ContentAndAssetPath::getContent)
                .collect(Collectors.toList());
        List<String> paths = responseData
                .stream()
                .map(ContentAndAssetPath::getPath)
                .collect(Collectors.toList());

        return new UploadResponse(contents, paths);
    }


    private List<ContentAndAssetPath> uploadAsset(final CondaHostedFacet repository, final List<AssetUpload> assetUploads) throws IOException {

        List<ContentAndAssetPath> responseData = new ArrayList<>();
        for (AssetUpload asset : assetUploads) {
            PartPayload payload = asset.getPayload();

            String path = asset.getFields().get(PATH);
            Content content = repository.upload(path, payload);
            responseData.add(new ContentAndAssetPath(content, path));
            repository.rebuildRepoDataJson();
        }

        return responseData;
    }

    @Override
    public UploadDefinition getDefinition() {
        List<UploadFieldDefinition> assetFields = Arrays.asList(new UploadFieldDefinition(PATH, false, UploadFieldDefinition.Type.STRING));
        return getDefinition(CondaFormat.NAME, false, new ArrayList<>(), assetFields, null);
    }

    @Override
    public VariableResolverAdapter getVariableResolverAdapter() {
        return variableResolverAdapter;
    }

    @Override
    public ContentPermissionChecker contentPermissionChecker() {
        return contentPermissionChecker;
    }

    private static class ContentAndAssetPath {
        private final Content content;
        private final String path;

        private ContentAndAssetPath(Content content, String path) {
            this.content = content;
            this.path = path;
        }

        public Content getContent() {
            return content;
        }

        public String getPath() {
            return path;
        }
    }
}
