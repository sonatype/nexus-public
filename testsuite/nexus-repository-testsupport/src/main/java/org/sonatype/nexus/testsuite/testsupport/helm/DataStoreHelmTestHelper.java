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
package org.sonatype.nexus.testsuite.testsupport.helm;

import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

@Named
@Singleton
public class DataStoreHelmTestHelper
    extends HelmTestHelper
{
  @Override
  public Blob getBlob(final Repository repository, final BlobRef blobRef) {
    ContentFacet contentFacet = repository.facet(ContentFacet.class);
    return contentFacet.blobs().blob(blobRef).orElseThrow(NullPointerException::new);
  }

  @Override
  public Properties getRestoreAssetProperties(final Repository repository, final String path) {
    ContentFacet contentFacet = repository.facet(ContentFacet.class);
    FluentAsset asset =
        contentFacet.assets().path(prependIfMissing(path, "/")).find().orElseThrow(NullPointerException::new);
    Properties properties = new Properties();
    properties.setProperty(HEADER_PREFIX + REPO_NAME_HEADER, repository.getName());
    properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, asset.path());
    properties.setProperty(HEADER_PREFIX + CONTENT_TYPE_HEADER, asset.download().getContentType());
    return properties;
  }
}
