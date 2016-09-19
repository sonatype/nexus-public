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
package org.sonatype.nexus.repository.attributes.internal;

import javax.inject.Named;

import org.sonatype.nexus.common.collect.ImmutableNestedAttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.AttributeChange;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.Operations;
import org.sonatype.nexus.transaction.TransactionalBuilder;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.common.concur.ONeedRetryException;

/**
 * Persists repository attributes in the repository's corresponding {@link Bucket}.
 *
 * @since 3.0
 */
@Named
public class AttributesFacetImpl
    extends FacetSupport
    implements AttributesFacet
{
  @Override
  public ImmutableNestedAttributesMap getAttributes() {
    return inTransaction().call(() -> {
      final StorageTx tx = UnitOfWork.currentTx();
      final NestedAttributesMap attributes = tx.findBucket(getRepository()).attributes();
      return new ImmutableNestedAttributesMap(null, attributes.getKey(), attributes.backing());
    });
  }

  @Override
  public void modifyAttributes(final AttributeChange change) {
    inTransaction().call(() -> {
      final StorageTx tx = UnitOfWork.currentTx();

      final Bucket bucket = tx.findBucket(getRepository());
      change.apply(bucket.attributes());
      tx.saveBucket(bucket);

      return null;
    });
  }

  private TransactionalBuilder<RuntimeException> inTransaction() {
    return Operations
        .transactional(facet(StorageFacet.class).txSupplier())
        .retryOn(ONeedRetryException.class);
  }
}
