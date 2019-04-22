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
package org.sonatype.nexus.repository.browse.internal;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.selector.internal.ContentAuthHelper;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * An {@link Iterator<ODocument>} that will either browse the {@code asset} class if {@code rid} is {@code null},
 * or iteratively query the {@code asset} class for records after {@code rid}. The number of records
 * returned will be less than or equal to {@code limit}.
 *
 * When iteratively querying the {@code asset} class, {@code pageSize} controls the number of records
 * that will be returned during each iteration before filtering by {@code bucketId} and content
 * authorization.
 *
 * @since 3.next
 */
public class BrowseAssetIterator
    extends ComponentSupport
    implements Iterator<ODocument>
{
  private static final String ASSET = "asset";

  private static final String BUCKET = "bucket";

  static final String QUERY_TEMPLATE = "select from asset where @rid > %s";

  private int count = 0;

  private String startRid;

  private final Queue<ODocument> queue;

  private final ContentAuthHelper contentAuthHelper;

  private final ODatabaseDocumentTx db;

  private final String rid;

  private final String repositoryName;

  private final List<String> bucketIds;

  private final int limit;

  private final int pageSize;

  private boolean hasBrowsedClass = false;

  BrowseAssetIterator(final ContentAuthHelper contentAuthHelper,
                      final ODatabaseDocumentTx db,
                      @Nullable final String rid,
                      final String repositoryName,
                      final List<String> bucketIds,
                      final int limit,
                      final int pageSize)
  {
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
    this.db = checkNotNull(db);
    this.rid = rid;
    this.repositoryName = checkNotNull(repositoryName);
    this.bucketIds = checkNotNull(bucketIds);
    this.limit = limit;
    this.pageSize = pageSize;
    this.queue = new ArrayDeque<>(pageSize);
    this.startRid = rid;
  }

  @Override
  public boolean hasNext() {
    fillQueueIfEmpty();
    return count < limit && !queue.isEmpty();
  }

  @Override
  public ODocument next() {
    fillQueueIfEmpty();
    ODocument doc = queue.poll();
    if (doc != null) {
      count++;
      return doc;
    }
    else {
      throw new NoSuchElementException();
    }
  }

  String getRid() {
    return rid;
  }

  String getStartRid() {
    return startRid;
  }

  List<String> getBucketIds() {
    return bucketIds;
  }

  int getLimit() {
    return limit;
  }

  String getQuery() {
    if (getRid() == null) {
      return "<<browse class asset>>";
    }
    return String.format(QUERY_TEMPLATE, getStartRid());
  }

  int getCount() {
    return count;
  }

  int getPageSize() {
    return pageSize;
  }

  boolean isHasBrowsedClass() {
    return hasBrowsedClass;
  }

  private List<ODocument> getPage() {
    if (log.isDebugEnabled()) {
      log.debug(toString());
    }
    if (rid == null) {
      return browseClass();
    }
    else {
      return db.query(new OSQLSynchQuery<ODocument>(getQuery(), pageSize));
    }
  }

  private List<ODocument> browseClass() {
    if (hasBrowsedClass) {
      return emptyList();
    }
    else {
      hasBrowsedClass = true;
      return StreamSupport.stream(db.browseClass(ASSET).spliterator(), false)
          .filter(bucketFilter())
          .filter(contentAuthFilter())
          .limit(limit)
          .collect(toList());
    }
  }

  private void fillQueueIfEmpty() {
    while (queue.isEmpty()) {
      List<ODocument> docs = getPage();
      if (docs.isEmpty()) {
        break;
      }
      Stream<ODocument> docStream = docs.stream();
      if (!hasBrowsedClass) {
        docStream = docStream.filter(bucketFilter()).filter(contentAuthFilter());
      }
      docStream.forEach(queue::add);
      startRid = docs.get(docs.size() - 1).getIdentity().toString();
    }
  }

  private Predicate<ODocument> bucketFilter() {
    if (bucketIds.isEmpty()) {
      return doc -> true;
    }
    else {
      return doc -> bucketIds.contains(toBucketId(doc.field(BUCKET)));
    }
  }

  private Predicate<ODocument> contentAuthFilter() {
    return doc -> contentAuthHelper.checkAssetPermissions(doc, repositoryName);
  }

  private static String toBucketId(final Object obj) {
    if (obj instanceof ODocument) {
      return ((ODocument) obj).getIdentity().toString();
    }
    else {
      return obj.toString();
    }
  }

  @Override
  public String toString() {
    return "BrowseAssetIterator{" +
        "query=" + getQuery() +
        ", rid=" + rid +
        ", startRid=" + startRid +
        ", bucketIds=[" + join(", ", bucketIds) + "]" +
        ", limit=" + limit +
        ", count=" + count +
        ", pageSize= " + pageSize +
        ", hasBrowsedClass= " + hasBrowsedClass +
        '}';
  }
}
