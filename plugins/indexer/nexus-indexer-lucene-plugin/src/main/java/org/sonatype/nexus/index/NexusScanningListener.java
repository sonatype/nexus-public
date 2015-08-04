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
package org.sonatype.nexus.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.scheduling.TaskUtil;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;

/**
 * Nexus specific ArtifactScanningListener implementation. Looks like the MI's DefaultScannerListener, but has
 * subtle but important differences. Most importantly, the "update" parameter is aligned with the meaning of
 * "fullReindex", that was before somewhat negation of it, but not fully. Lessen memory consumption by removal
 * of fields like uinfos and group related ones. The "deletion" detection is done inversely as in default
 * scanner listener: instead gather all the "present" uinfo's into a (potentially huge) set of  strings,
 * index is read and processedUinfos is used to check what is present. Redundant optimize call removed also.
 *
 * @since 2.3
 */
public class NexusScanningListener
    extends ComponentSupport
    implements ArtifactScanningListener
{
  private final IndexingContext context;

  private final IndexSearcher contextIndexSearcher;

  private final boolean fullReindex;

  private final boolean isProxy;

  // the UINFO set used to track processed artifacts (grows during scanning)
  private final Set<String> processedUinfos = new HashSet<String>();

  // exceptions detected and gathered during scanning
  private final List<Exception> exceptions = new ArrayList<Exception>();

  // total count of artifacts discovered
  private int discovered;

  // total count of artifacts added to index
  private int added;

  // total count of artifacts updated on index
  private int updated;

  // timestamp in millis when scanning started
  private long scanningStarted;

  public NexusScanningListener(final IndexingContext context,
                               final IndexSearcher contextIndexSearcher,
                               final boolean fullReindex,
                               final boolean isProxy)
      throws IOException
  {
    this.context = context;
    this.contextIndexSearcher = contextIndexSearcher;
    this.fullReindex = fullReindex;
    this.isProxy = isProxy;
    this.discovered = 0;
    this.added = 0;
    this.updated = 0;
  }

  @Override
  public void scanningStarted(final IndexingContext ctx) {
    log.info("Scanning of repositoryID=\"{}\" started.", context.getRepositoryId());
    scanningStarted = System.currentTimeMillis();
  }

  @Override
  public void artifactDiscovered(final ArtifactContext ac) {
    TaskUtil.checkInterruption();
    final String uinfo = ac.getArtifactInfo().getUinfo();
    if (!processedUinfos.add(uinfo)) {
      return; // skip individual snapshots, this skips like unique timestamped snapshots as indexer uses baseVersion
    }

    try {
      // hosted-full: just blindly add, no need for uniq check, as it happens against empty ctx
      // hosted-nonFull: do update, add when document changed (see update method)
      // proxy-full: do update, as record might be present from downloaded index. Usually is, but Central does not publish ClassNames so update will happen
      // proxy-non-full: do update, as record might be present from downloaded index or already indexed (by some prev scan or by being pulled from remote).

      // act accordingly what we do: hosted/proxy repair/update
      final IndexOp indexOp;
      if (fullReindex && !isProxy) {
        // HOSTED-full only -- in this case, work is done against empty temp ctx so it fine
        // is cheaper, does add, but
        // does not maintain uniqueness
        indexOp = index(ac);
      }
      else {
        // HOSTED-nonFull + PROXY-full/nonFull must go this path. In case of proxy, remote index was pulled, so ctx is not empty
        // is costly, does delete+add
        // maintains uniqueness
        indexOp = update(ac);
      }
      discovered++;
      if (IndexOp.ADDED == indexOp) {
        added++;
      }
      else if (IndexOp.UPDATED == indexOp) {
        updated++;
      }
      for (Exception e : ac.getErrors()) {
        artifactError(ac, e);
      }
    }
    catch (Exception ex) {
      artifactError(ac, ex);
    }
  }

  @Override
  public void scanningFinished(final IndexingContext ctx, final ScanningResult result) {
    TaskUtil.checkInterruption();
    int removed = 0;
    try {
      if (!fullReindex && !isProxy) {
        // HOSTED-nonFull only, perform delete detection too (remove stuff from index that is removed from repository
        removed = removeDeletedArtifacts(result.getRequest().getStartingPath());
      }
      // rebuild groups, as methods moved out from IndexerEngine does not maintain groups anymore
      // as it makes no sense to do it during batch invocation of update method
      context.rebuildGroups();
      context.commit();
    }
    catch (IOException ex) {
      result.addException(ex);
    }

    result.setTotalFiles(discovered);
    result.setDeletedFiles(removed);
    result.getExceptions().addAll(exceptions);

    if (result.getDeletedFiles() > 0 || result.getTotalFiles() > 0) {
      try {
        context.updateTimestamp(true);
        context.optimize();
      }
      catch (Exception ex) {
        result.addException(ex);
      }
    }
    log.info(
        "Scanning of repositoryID=\"{}\" finished: scanned={}, added={}, updated={}, removed={}, scanningDuration={}",
        context.getRepositoryId(), discovered, added, updated, removed,
        DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - scanningStarted)
    );
  }

  @Override
  public void artifactError(final ArtifactContext ac, final Exception e) {
    Exception exception = e;
    if (ac.getPom() != null || ac.getArtifact() != null) {
      final StringBuilder sb = new StringBuilder("Found a problem while indexing");
      if (ac.getArtifact() != null) {
        sb.append(" artifact '" + ac.getArtifact().getAbsolutePath() + "'");
      }
      if (ac.getPom() != null) {
        sb.append(" pom '" + ac.getPom().getAbsolutePath() + "'");
      }
      exception = new Exception(sb.toString(), e);
    }
    exceptions.add(exception);
  }

  /**
   * Used in {@code update} mode, deletes documents from index that are not found during scanning (means
   * they were deleted from the storage being scanned).
   */
  private int removeDeletedArtifacts(final String contextPath)
      throws IOException
  {
    int deleted = 0;
    final IndexReader r = contextIndexSearcher.getIndexReader();
    for (int i = 0; i < r.maxDoc(); i++) {
      if (!r.isDeleted(i)) {
        final Document d = r.document(i);
        final String uinfo = d.get(ArtifactInfo.UINFO);
        if (uinfo != null && !processedUinfos.contains(uinfo)) {
          // file is not present in storage but is on index, delete it from index
          final String[] ra = ArtifactInfo.FS_PATTERN.split(uinfo);
          final ArtifactInfo ai = new ArtifactInfo();
          ai.repository = context.getRepositoryId();
          ai.groupId = ra[0];
          ai.artifactId = ra[1];
          ai.version = ra[2];
          if (ra.length > 3) {
            ai.classifier = ArtifactInfo.renvl(ra[3]);
          }
          if (ra.length > 4) {
            ai.packaging = ArtifactInfo.renvl(ra[4]);
          }

          // minimal ArtifactContext for removal
          final ArtifactContext ac = new ArtifactContext(null, null, null, ai, ai.calculateGav());
          if (contextPath == null
              || context.getGavCalculator().gavToPath(ac.getGav()).startsWith(contextPath)) {
            if (IndexOp.DELETED == remove(ac)) {
              deleted++;
            }
          }
        }
      }
    }
    return deleted;
  }

  // == copied from
  // https://github.com/apache/maven-indexer/blob/maven-indexer-5.1.0/indexer-core/src/main/java/org/apache/maven/index/DefaultIndexerEngine.java
  // Changes made:
  // * none of the index/update/remove method does more that modifying index, timestamp is not set by either
  // * update does not maintains groups either (per invocation!), it happens once at scan finish

  public enum IndexOp
  {
    NOOP, ADDED, UPDATED, DELETED;
  }

  private IndexOp index(final ArtifactContext ac)
      throws IOException
  {
    if (ac != null && ac.getGav() != null) {
      final Document d = ac.createDocument(context);
      if (d != null) {
        context.getIndexWriter().addDocument(d);
        return IndexOp.ADDED;
      }
    }
    return IndexOp.NOOP;
  }

  private IndexOp update(final ArtifactContext ac)
      throws IOException
  {
    if (ac != null && ac.getGav() != null) {
      final Document d = ac.createDocument(context);
      if (d != null) {
        final Document old = getOldDocument(ac);
        if (old == null) {
          context.getIndexWriter().addDocument(d);
          return IndexOp.ADDED;
        }
        else if (!equals(d, old)) {
          context.getIndexWriter().updateDocument(
              new Term(ArtifactInfo.UINFO, ac.getArtifactInfo().getUinfo()), d);
          return IndexOp.UPDATED;
        }
      }
    }
    return IndexOp.NOOP;
  }

  private IndexOp remove(final ArtifactContext ac)
      throws IOException
  {
    if (ac != null) {
      final String uinfo = ac.getArtifactInfo().getUinfo();
      // add artifact deletion marker
      final Document doc = new Document();
      doc.add(new Field(ArtifactInfo.DELETED, uinfo, Field.Store.YES, Field.Index.NO));
      doc.add(new Field(ArtifactInfo.LAST_MODIFIED, //
          Long.toString(System.currentTimeMillis()), Field.Store.YES, Field.Index.NO));
      IndexWriter w = context.getIndexWriter();
      w.addDocument(doc);
      w.deleteDocuments(new Term(ArtifactInfo.UINFO, uinfo));
      return IndexOp.DELETED;
    }
    return IndexOp.NOOP;
  }

  private boolean equals(final Document d1, final Document d2) {
    // d1 is never null, check caller
    if (d1 == null && d2 == null) {
      return true;
    }
    // d2 is never null, check caller
    if (d1 == null || d2 == null) {
      return false;
    }
    final Map<String, String> m1 = toMap(d1);
    final Map<String, String> m2 = toMap(d2);
    m1.remove(MinimalArtifactInfoIndexCreator.FLD_LAST_MODIFIED.getKey());
    m2.remove(MinimalArtifactInfoIndexCreator.FLD_LAST_MODIFIED.getKey());

    final boolean result = m1.equals(m2);
    if (!result) {
      log.trace("d1={}, d2={}", m1, m2);
    }
    return result;
  }

  private Map<String, String> toMap(final Document d) {
    final HashMap<String, String> result = new HashMap<String, String>();
    for (Object o : d.getFields()) {
      final Fieldable f = (Fieldable) o;
      if (f.isStored()) {
        result.put(f.name(), f.stringValue());
      }
    }
    return result;
  }

  private Document getOldDocument(ArtifactContext ac)
      throws IOException
  {
    final TopDocs result =
        contextIndexSearcher.search(
            new TermQuery(new Term(ArtifactInfo.UINFO, ac.getArtifactInfo().getUinfo())), 2);

    if (result.totalHits == 1) {
      return contextIndexSearcher.doc(result.scoreDocs[0].doc);
    }
    return null;
  }
}
