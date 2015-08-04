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
package org.sonatype.nexus.proxy.walker;

import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * A walker filter that is based on Google Guava Predicates, checking for item paths only. What path to use (the
 * mutable
 * {@link StorageItem#getPath()} or immutable UID {@link StorageItem#getRepositoryItemUid()} depends on
 * {@link PathExtractor} passed in.
 *
 * @author cstamas
 * @since 2.1
 */
public class PredicatePathWalkerFilter
    implements WalkerFilter
{
  private final Predicate<String> itemPredicate;

  private final Predicate<String> collectionPredicate;

  private final PathExtractor pathExtractor;

  public PredicatePathWalkerFilter(final PathExtractor pathExtractor, final Predicate<String> itemPredicate) {
    this(pathExtractor, itemPredicate, Predicates.<String>alwaysTrue());
  }

  public PredicatePathWalkerFilter(final PathExtractor pathExtractor, final Predicate<String> itemPredicate,
                                   final Predicate<String> collectionPredicate)
  {
    this.pathExtractor = Preconditions.checkNotNull(pathExtractor, "PathExtractor is null!");
    this.itemPredicate = Preconditions.checkNotNull(itemPredicate, "Item predicate is null!");
    this.collectionPredicate = Preconditions.checkNotNull(collectionPredicate, "Collection predicate is null!");
  }

  @Override
  public boolean shouldProcess(WalkerContext context, StorageItem item) {
    return itemPredicate.apply(pathExtractor.extractItemPath(item));
  }

  @Override
  public boolean shouldProcessRecursively(WalkerContext context, StorageCollectionItem coll) {
    return collectionPredicate.apply(pathExtractor.extractCollectionItemPath(coll));
  }

  // ==

  public static final PathExtractor ITEM_PATH_EXTRACTOR = new PathExtractor()
  {
    @Override
    public String extractItemPath(StorageItem item) {
      return item.getPath();
    }

    @Override
    public String extractCollectionItemPath(StorageCollectionItem coll) {
      return extractItemPath(coll);
    }
  };

  public interface PathExtractor
  {
    String extractItemPath(StorageItem item);

    String extractCollectionItemPath(StorageCollectionItem coll);
  }
}
