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
package org.sonatype.nexus.repository.raw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandler;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
 */
@Named(RawFormat.NAME)
@Singleton
public class RawUploadHandler
    implements UploadHandler
{
  private static final String FILENAME = "filename";

  private static final String DIRECTORY = "directory";

  private UploadDefinition definition;

  public RawUploadHandler() {
    definition = new UploadDefinition(RawFormat.NAME, true,
        Collections.singletonList(new UploadFieldDefinition(DIRECTORY, false, Type.STRING)),
        Collections.singletonList(new UploadFieldDefinition(FILENAME, false, Type.STRING)));
  }

  @Override
  public Collection<String> handle(final Repository repository, final ComponentUpload upload) throws IOException {
    RawContentFacet facet = repository.facet(RawContentFacet.class);

    String basePath = normalize(upload.getFields().get(DIRECTORY));

    List<String> paths = new ArrayList<>();
    TransactionalStoreBlob.operation.withDb(repository.facet(StorageFacet.class).txSupplier())
        .throwing(IOException.class).run(() -> {
          for (AssetUpload asset : upload.getAssetUploads()) {
            String path = basePath + asset.getFields().get(FILENAME);

            facet.put(path, asset.getPayload());
            paths.add(path);
          }
        });
    return paths;
  }

  private String normalize(final String basePath) {
    String path = basePath;
    if (!path.endsWith("/")) {
      path += "/";
    }
    if (path.startsWith("/")) {
      path = basePath.substring(1);
    }
    return path;
  }

  @Override
  public UploadDefinition getDefinition() {
    return definition;
  }
}
