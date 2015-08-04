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
package org.sonatype.timeline.internal;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.timeline.TimelineCallback;
import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineFilter;
import org.sonatype.timeline.TimelineRecord;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

public class DefaultTimelineIndexer
    extends ComponentSupport
{

  private static final String TIMESTAMP = "_t";

  private static final String TYPE = "_1";

  private static final String SUBTYPE = "_2";

  private static final Resolution TIMELINE_RESOLUTION = Resolution.SECOND;

  // ==

  private final String luceneFSDirectoryType;

  private Directory directory;

  private IndexWriter indexWriter;

  private SearcherManager searcherManager;

  private int generation = 0;

  /**
   * Constructor. The {@code luceneFSDirectoryType} is copied from nexus-indexer-lucene-plugin's
   * DefaultIndexerManager
   * as part of fix for NEXUS-5658:
   * <p>
   * As of 3.6.1, Lucene provides three FSDirectory implementations, all with there pros and cons.
   * <ul>
   * <li>mmap -- {@link MMapDirectory}</li>
   * <li>nio -- {@link NIOFSDirectory}</li>
   * <li>simple -- {@link SimpleFSDirectory}</li>
   * </ul>
   * By default, Lucene selects FSDirectory implementation based on specifics of the operating system and JRE used,
   * but this configuration parameter allows override.
   */
  public DefaultTimelineIndexer(final String luceneFSDirectoryType)
  {
    this.luceneFSDirectoryType = luceneFSDirectoryType;
  }

  // ==
  // Public API

  protected void start(final TimelineConfiguration configuration)
      throws IOException
  {
    closeIndexWriter();
    if (directory != null) {
      directory.close();
    }
    directory = openFSDirectory(configuration.getIndexDirectory());
    if (IndexReader.indexExists(directory)) {
      if (IndexWriter.isLocked(directory)) {
        IndexWriter.unlock(directory);
      }
    }

    final IndexWriterConfig config =
        new IndexWriterConfig(Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36));
    config.setMergeScheduler(new SerialMergeScheduler());
    config.setRAMBufferSizeMB(2.0);
    indexWriter = new IndexWriter(directory, config);
    indexWriter.commit();

    searcherManager = new SearcherManager(indexWriter, false, new SearcherFactory());
    generation = generation + 1;
  }

  private FSDirectory openFSDirectory(final File location)
      throws IOException
  {
    if (luceneFSDirectoryType == null) {
      // NEXUS-5752: default: nio
      return new NIOFSDirectory(location);
    }
    else if ("mmap".equals(luceneFSDirectoryType)) {
      return new MMapDirectory(location);
    }
    else if ("nio".equals(luceneFSDirectoryType)) {
      return new NIOFSDirectory(location);
    }
    else if ("simple".equals(luceneFSDirectoryType)) {
      return new SimpleFSDirectory(location);
    }
    else {
      throw new IllegalArgumentException(
          "''"
              + luceneFSDirectoryType
              + "'' is not valid/supported Lucene FSDirectory type. Only ''mmap'', ''nio'' and ''simple'' are allowed");
    }
  }

  protected void stop()
      throws IOException
  {
    closeIndexWriter();
    if (directory != null) {
      directory.close();
      directory = null;
    }
  }

  protected void add(final TimelineRecord record)
      throws IOException
  {
    addAll(record);
  }

  protected void addAll(final TimelineRecord... records)
      throws IOException
  {
    for (TimelineRecord rec : records) {
      indexWriter.addDocument(createDocument(rec));
    }
    indexWriter.commit();
  }

  protected void addBatch(final TimelineRecord record)
      throws IOException
  {
    indexWriter.addDocument(createDocument(record));
  }

  protected void finishBatch()
      throws IOException
  {
    indexWriter.commit();
    searcherManager.maybeRefresh();
  }

  protected void retrieve(final long fromTime, final long toTime, final Set<String> types,
                          final Set<String> subTypes, int from, int count, final TimelineFilter filter,
                          final TimelineCallback callback)
      throws IOException
  {
    if (count == 0) {
      // new in Lucene 3.5, it would bitch IllegalArgEx if we ask for "top 0" docs
      return;
    }
    searcherManager.maybeRefresh();
    final IndexSearcher searcher = searcherManager.acquire();
    try {
      if (searcher.maxDoc() == 0) {
        // index empty
        return;
      }
      final TopFieldDocs topDocs;
      if (filter == null) {
        // without filter is easy: we account for paging only
        topDocs =
            searcher.search(buildQuery(fromTime, toTime, types, subTypes), null, from + count, new Sort(
                new SortField(TIMESTAMP, SortField.LONG, true)));
      }
      else {
        // with filter is hard: we go for max val and let's see what will filter throw out to fulfil count to
        // return
        topDocs =
            searcher.search(buildQuery(fromTime, toTime, types, subTypes), null, Integer.MAX_VALUE,
                new Sort(new SortField(TIMESTAMP, SortField.LONG, true)));
      }
      if (topDocs.scoreDocs.length == 0) {
        // nothing found on index
        return;
      }

      // build result
      int i = 0;
      int returned = 0;
      while (true) {
        if (i >= topDocs.scoreDocs.length || returned >= count) {
          break;
        }

        Document doc = searcher.doc(topDocs.scoreDocs[i++].doc);
        TimelineRecord data = buildData(doc);
        if (filter != null && !filter.accept(data)) {
          data = null;
          continue;
        }
        // skip the unneeded stuff
        // Warning: this means we skip the needed FILTERED stuff out!
        if (from > 0) {
          from--;
          continue;
        }
        returned++;
        if (!callback.processNext(data)) {
          break;
        }
      }
    }
    finally {
      searcherManager.release(searcher);
    }
  }

  private static final int PURGE_BATCH_SIZE = 1_000_000;

  protected int purge(final long fromTime, final long toTime, final Set<String> types, final Set<String> subTypes)
      throws IOException
  {
    searcherManager.maybeRefresh();
    final IndexSearcher searcher = searcherManager.acquire();
    try {
      if (searcher.maxDoc() == 0) {
        // empty index, nothing to purge
        return 0;
      }

      final Query q = buildQuery(fromTime, toTime, types, subTypes);
      log.debug("purge query='{}'", q);
      int deletedCount = 0;
      TopDocs topDocs = searcher.search(q, PURGE_BATCH_SIZE);
      // NEXUS-7671: Purge in batches, to conserve heap memory and not OOM
      while (topDocs.scoreDocs.length > 0) {
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
          final Document doc = searcher.doc(scoreDoc.doc);
          // NEXUS-7671: This will delete by timestamp and will neglect the set of types/subtypes
          // But, except for UTs, production is NOT using this feature at all (see Timeline#purgeOlderThan method)
          // purge in production is invoked always with types == subTypes == null
          indexWriter.deleteDocuments(new Term(TIMESTAMP, doc.get(TIMESTAMP)));
          deletedCount++;
        }
        log.debug("purged so far: {}", deletedCount);
        topDocs = searcher.searchAfter(topDocs.scoreDocs[topDocs.scoreDocs.length - 1], q, PURGE_BATCH_SIZE);
      }

      // avoid potentially expensive operations below if nothing done
      if (deletedCount == 0) {
        return 0;
      }

      indexWriter.commit();
      indexWriter.optimize();
      indexWriter.deleteUnusedFiles();
      // Since NEXUS-7671 fix, the returned count is not EXACT document count anymore!
      return deletedCount;
    }
    finally {
      searcherManager.release(searcher);
    }
  }

  // ==

  protected void closeIndexWriter()
      throws IOException
  {
    if (searcherManager != null) {
      searcherManager.close();
      searcherManager = null;
    }
    if (indexWriter != null) {
      indexWriter.commit();
      indexWriter.close();
      indexWriter = null;
    }
  }

  protected Document createDocument(final TimelineRecord record) {
    final Document doc = new Document();
    doc.add(new Field(TIMESTAMP, DateTools.timeToString(record.getTimestamp(), TIMELINE_RESOLUTION),
        Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field(TYPE, record.getType(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field(SUBTYPE, record.getSubType(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    for (Map.Entry<String, String> dataEntry : record.getData().entrySet()) {
      doc.add(new Field(dataEntry.getKey(), dataEntry.getValue(), Field.Store.YES, Field.Index.ANALYZED));
    }
    return doc;
  }

  protected Query buildQuery(final long from, final long to, final Set<String> types, final Set<String> subTypes) {
    if (isEmptySet(types) && isEmptySet(subTypes)) {
      return new TermRangeQuery(TIMESTAMP, DateTools.timeToString(from, TIMELINE_RESOLUTION),
          DateTools.timeToString(to, TIMELINE_RESOLUTION), true, true);
    }
    else {
      final BooleanQuery result = new BooleanQuery();
      result.add(
          new TermRangeQuery(TIMESTAMP, DateTools.timeToString(from, TIMELINE_RESOLUTION),
              DateTools.timeToString(to, TIMELINE_RESOLUTION), true, true), Occur.MUST);
      if (!isEmptySet(types)) {
        final BooleanQuery typeQ = new BooleanQuery();
        for (String type : types) {
          typeQ.add(new TermQuery(new Term(TYPE, type)), Occur.SHOULD);
        }
        result.add(typeQ, Occur.MUST);
      }
      if (!isEmptySet(subTypes)) {
        final BooleanQuery subTypeQ = new BooleanQuery();
        for (String subType : subTypes) {
          subTypeQ.add(new TermQuery(new Term(SUBTYPE, subType)), Occur.SHOULD);
        }
        result.add(subTypeQ, Occur.MUST);
      }
      return result;
    }
  }

  protected boolean isEmptySet(final Set<String> set) {
    return set == null || set.size() == 0;
  }

  protected TimelineRecord buildData(Document doc) {
    long timestamp = -1;

    String tsString = doc.get(TIMESTAMP);

    if (tsString != null) {
      // legacy indexes will have nulls here
      try {
        timestamp = DateTools.stringToTime(tsString);
      }
      catch (ParseException e) {
        // leave it -1
      }
    }

    // legacy indexes will have nulls here
    String type = doc.get(TYPE);

    // legacy indexes will have nulls here
    String subType = doc.get(SUBTYPE);

    // legacy document: they had no type/subType designators
    // we are shoving them under "system", as they actually were
    // system-only back then
    if (StringUtils.isBlank(type) && StringUtils.isBlank(subType)) {
      type = "SYSTEM"; // legacy
      subType = "Legacy";
    }

    Map<String, String> data = new HashMap<String, String>();

    for (Fieldable field : doc.getFields()) {
      if (!field.name().startsWith("_")) {
        data.put(field.name(), field.stringValue());
      }
    }

    return new TimelineRecord(timestamp, type, subType, data);
  }
}
