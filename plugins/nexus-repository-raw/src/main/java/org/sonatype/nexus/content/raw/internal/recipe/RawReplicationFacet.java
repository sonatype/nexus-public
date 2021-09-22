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
package org.sonatype.nexus.content.raw.internal.recipe;

import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.replication.ReplicationFacetSupport;
import org.sonatype.nexus.repository.content.replication.ReplicationFacet;
import org.sonatype.nexus.repository.raw.RawCoordinatesHelper;
import org.sonatype.nexus.repository.raw.internal.RawFormat;

import static org.sonatype.nexus.repository.content.AttributeOperation.SET;
import static org.sonatype.nexus.repository.replication.ReplicationUtils.getChecksumsFromProperties;

/**
 * A {@link ReplicationFacet} for raw content.
 *
 * @since 3.24
 */
@Named(RawFormat.NAME)
public class RawReplicationFacet
    extends ReplicationFacetSupport
{
  @Override
  public void doReplicate(final String path,
                        final Blob blob,
                        final Map<String, Object> assetAttributes,
                        final Map<String, Object> componentAttributes)
  {
    ContentFacet contentFacet = facet(ContentFacet.class);
    FluentAssets fluentAssets = contentFacet.assets();
    FluentComponents fluentComponents = contentFacet.components();

    FluentAsset fluentAsset = fluentAssets.path(path)
        .component(fluentComponents
            .name(path)
            .namespace(RawCoordinatesHelper.getGroup(path))
            .getOrCreate())
        .blob(blob, getChecksumsFromProperties(assetAttributes))
        .save();

    AttributeChangeSet changeSet = new AttributeChangeSet();
    for (Map.Entry<String, Object> entry : assetAttributes.entrySet()) {
      changeSet.attributes(SET, entry.getKey(), entry.getValue());
    }
    fluentAsset.attributes(changeSet);
  }
}
