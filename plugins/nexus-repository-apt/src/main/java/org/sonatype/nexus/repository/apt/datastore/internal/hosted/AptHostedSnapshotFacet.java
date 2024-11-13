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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.snapshot.AptSnapshotFacetSupport;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;

/**
 * Implementation of snapshots for apt hosted repository.
 *
 * @since 3.31
 */
@Facet.Exposed
@Named
public class AptHostedSnapshotFacet
    extends AptSnapshotFacetSupport
{
  @Override
  protected List<SnapshotItem> fetchSnapshotItems(final List<ContentSpecifier> specs) {
    AptContentFacet apt = getRepository().facet(AptContentFacet.class);
    List<SnapshotItem> list = new ArrayList<>();
    for (ContentSpecifier spec : specs) {
      apt.get(spec.path).map(value -> new SnapshotItem(spec, value)).ifPresent(list::add);
    }
    return list;
  }
}
