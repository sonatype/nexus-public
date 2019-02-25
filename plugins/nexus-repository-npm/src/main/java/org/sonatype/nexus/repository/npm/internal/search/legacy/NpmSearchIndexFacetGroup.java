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
package org.sonatype.nexus.repository.npm.internal.search.legacy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.storage.AssetManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closer;

import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.mapper;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.rawMapJsonTypeRef;

/**
 * npm search index facet for group repositories: it creates the index blob and caches. It also invalidates it if
 * any member received any change, rebuilding the blob as needed.
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
@Named
public class NpmSearchIndexFacetGroup
    extends NpmSearchIndexFacetCaching
    implements Asynchronous
{
  @Inject
  public NpmSearchIndexFacetGroup(final EventManager eventManager, final AssetManager assetManager) {
    super(eventManager, assetManager);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final NpmSearchIndexInvalidatedEvent event) {
    if (facet(GroupFacet.class).member(event.getRepository())) {
      invalidateCachedSearchIndex();
    }
  }

  /**
   * Builds the index by merging member indexes with low memory footprint, as JSON indexes might get huge.
   */
  @Nonnull
  @Override
  protected Content buildIndex(final StorageTx tx, final Path path) throws IOException {
    final List<Repository> members = facet(GroupFacet.class).leafMembers();
    final Closer closer = Closer.create();
    try {
      final ArrayList<JsonParser> parsers = new ArrayList<>(members.size());
      pauseTransactionAndProcessMemberRepositories(tx, members, closer, parsers);
      final JsonGenerator generator = closer.register(
          mapper.getFactory().createGenerator(new BufferedOutputStream(Files.newOutputStream(path)))
      );
      generator.writeStartObject();
      generator.writeNumberField(NpmMetadataUtils.META_UPDATED, System.currentTimeMillis());
      final PackageMerger packageMerger = new PackageMerger(parsers);
      while (!packageMerger.isDepleted()) {
        NestedAttributesMap packageRoot = packageMerger.next();
        generator.writeObjectField(packageRoot.getKey(), packageRoot.backing());
      }
      generator.writeEndObject();
      generator.flush();
    }
    catch (Throwable t) {
      throw closer.rethrow(t);
    }
    finally {
      closer.close();
    }

    return new Content(new StreamPayload(
        new InputStreamSupplier()
        {
          @Nonnull
          @Override
          public InputStream get() throws IOException {
            return new BufferedInputStream(Files.newInputStream(path));
          }
        },
        Files.size(path),
        ContentTypes.APPLICATION_JSON)
    );
  }

  private void pauseTransactionAndProcessMemberRepositories(final StorageTx tx,
                                                            final List<Repository> members,
                                                            final Closer closer,
                                                            final ArrayList<JsonParser> parsers)
      throws IOException
  {
    UnitOfWork groupWork = UnitOfWork.pause();
    try {
      tx.commit();
      for (Repository repository : Lists.reverse(members)) {
        processMember(closer, parsers, repository);
      }
    }
    finally {
      UnitOfWork.resume(groupWork);
      tx.begin();
    }
  }

  private void processMember(final Closer closer, final ArrayList<JsonParser> parsers, final Repository repository)
      throws IOException
  {
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      // do not pass since upstream, ask for full index to cache full index, and searchIndex() will filter it
      final Content index = repository.facet(NpmSearchIndexFacet.class).searchIndex(null);
      // managing the content stream, since JsonParser is configured to NOT close the stream, see NpmJsonUtils.mapper
      final JsonParser jsonParser = mapper.getFactory().createParser(closer.register(index.openInputStream()));
      closer.register(jsonParser);
      // adjust parser position, skip first field of NpmMetadataUtils.META_UPDATED as rest are all value objects
      if (jsonParser.nextToken() == JsonToken.START_OBJECT &&
          NpmMetadataUtils.META_UPDATED.equals(jsonParser.nextFieldName())) {
        jsonParser.nextValue(); // skip value
        parsers.add(jsonParser);
      }
    }
    finally {
      UnitOfWork.end();
    }
  }

  /**
   * Class that accepts several properly positioned (_updated field skipped, current token is next package name or
   * {@link JsonToken#END_OBJECT} {@link JsonParser}s, and gets the "next" from one of the parser. The "next" is the
   * smallest when sorted all the current names. By getting next, same parser is tried for next package too, as it may
   * again provide package that is before other packages coming from other parsers. When parser is depleted, it is
   * getting removed from list of parsers to use.
   * <p/>
   * Ultimately, all the parsers should be depleted. The input is expected to be sorted alphabetically, as npm index
   * usually is. This class does not manages parsers, ie. does not closes them, just stops using them.
   */
  private static class PackageMerger
  {
    private final List<JsonParser> parsers;

    private final TreeMap<String, NestedAttributesMap> nextPackages;

    private final Multimap<String, JsonParser> origins;

    public PackageMerger(final List<JsonParser> parsers) throws IOException {
      this.parsers = parsers;
      this.nextPackages = new TreeMap<>();
      this.origins = ArrayListMultimap.create();
      for (Iterator<JsonParser> parserIterator = parsers.iterator(); parserIterator.hasNext(); ) {
        JsonParser parser = parserIterator.next();
        if (!tryNext(parser)) {
          parserIterator.remove();
        }
      }
    }

    /**
     * Returns {@code true} if no more parsers have any input.
     */
    public boolean isDepleted() {
      return nextPackages.isEmpty();
    }

    /**
     * Returns the next incomplete package root document in order.
     */
    public NestedAttributesMap next() throws IOException {
      final String nextKey = nextPackages.firstKey();
      final NestedAttributesMap next = nextPackages.remove(nextKey);
      final Collection<JsonParser> nextKeyOrigins = origins.removeAll(nextKey);
      for (JsonParser origin : nextKeyOrigins) {
        if (!tryNext(origin)) {
          parsers.remove(origin);
        }
      }
      return next;
    }

    /**
     * Method tries to load up next field (holding package name) and associated JSON Object (holding incomplete package
     * document) using given parser. If loaded up, the {@link #nextPackages} and {@link #origins} maps gets the new
     * document and this parser added under the key of the just loaded up package. If not loaded up (as parser got
     * depleted), the given parser is removed from the {@link #parsers} list. Returns {@false} if next was not found
     * in given parser, and parser should be removed from the list of parsers.
     */
    private boolean tryNext(final JsonParser origin) throws IOException {
      String packageName = origin.nextFieldName();
      if (packageName == null) {
        // depleted
        return false;
      }
      else {
        origin.nextToken();
        NestedAttributesMap packageRoot = new NestedAttributesMap(packageName, origin.readValueAs(rawMapJsonTypeRef));
        if (nextPackages.containsKey(packageName)) {
          packageRoot = NpmMetadataUtils.merge(
              packageName,
              ImmutableList.of(nextPackages.get(packageName), packageRoot)
          );
          NpmMetadataUtils.shrink(packageRoot);
        }
        nextPackages.put(packageName, packageRoot);
        origins.put(packageName, origin);
        return true;
      }
    }
  }
}
