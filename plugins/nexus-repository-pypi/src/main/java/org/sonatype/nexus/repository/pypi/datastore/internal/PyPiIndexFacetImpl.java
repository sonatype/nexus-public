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
package org.sonatype.nexus.repository.pypi.datastore.internal;

import javax.inject.Named;

import org.sonatype.nexus.repository.pypi.datastore.PypiContentFacet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.pypi.internal.PyPiIndexFacet;

import static org.sonatype.nexus.repository.pypi.datastore.internal.ContentPypiPathUtils.indexPath;
import static org.sonatype.nexus.repository.pypi.PyPiPathUtils.normalizeName;

/**
 * {@link PyPiIndexFacet} implementation.
 *
 * @since 3.29
 */
@Named
public class PyPiIndexFacetImpl
    extends FacetSupport
    implements PyPiIndexFacet
{
  @Override
  public void deleteRootIndex() {
    content().delete(indexPath());
  }

  @Override
  public void deleteIndex(String packageName) {
    String indexPath = indexPath(normalizeName(packageName));
    PypiContentFacet contentFacet = content();
    FluentAsset cachedIndex = contentFacet.assets().path(indexPath).find().orElse(null);

    /*
      There is a chance that the index wasn't found because of package name normalization. For example '.'
      characters are normalized to '-' so jts.python would have an index at /simple/jts-python/. It is possible that
      we could just check for the normalized name but we check for both just in case. Searching for an index with a
      normalized name first means that most, if not all, index deletions will only perform a single search.

      See https://issues.sonatype.org/browse/NEXUS-19303 for additional context.
     */
    if (cachedIndex == null) {
      indexPath = indexPath(packageName);
      cachedIndex = contentFacet.assets().path(indexPath).find().orElse(null);
    }

    if (cachedIndex != null) {
      cachedIndex.delete();
    }
  }

  private PypiContentFacet content() {
    return getRepository().facet(PypiContentFacet.class);
  }
}
