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
package org.sonatype.nexus.repository.group;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.collect.Iterables;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.net.HttpHeaders;
import org.joda.time.format.ISODateTimeFormat;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>
 * Merges responses from member repositories. Generally {@link SingleMetadataMergingGroupHandlerSupport} should be used
 * unless the merged response must create multiple files (e.g. Yum).
 * </p>
 *
 * <p>
 * To determine whether the rebuilding metadata is necessary this class hashes the content etags of members that return
 * 200. This hash of etags is compared against the etag of the Content already cached in this repository, if they do not
 * match then the metadata will be rebuilt. It is expected that when subclasses store merged metadata that they use the
 * computed etag.
 * </p>
 *
 * <p>
 * If only one member responds to the request this handler will return the member's content directly rather
 * than "merging" one response. This behaviour may not be appropriate for all formats if the content of the metadata
 * includes the repository's name in some fashion.
 * </p>
 */
public abstract class MergingGroupHandlerSupport
    extends GroupHandler
{
  private final HashFunction etagDigester = Hashing.sha256();

  private Cooperation2 cooperation;

  protected void configureCooperation(
      final Cooperation2Factory cooperationFactory,
      final boolean cooperationEnabled,
      final Duration majorTimeout,
      final Duration minorTimeout,
      final int threadsPerKey)
  {
    this.cooperation = checkNotNull(cooperationFactory).configure()
        .enabled(cooperationEnabled)
        .majorTimeout(majorTimeout)
        .minorTimeout(minorTimeout)
        .threadsPerKey(threadsPerKey)
        .build(getClass());
  }

  @Override
  protected Response doGet(@Nonnull final Context context, @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);

    Headers headers = context.getRequest().getHeaders();
    // Don't pass the range header onto the repositories, that is only relevant to the group
    String rangeheader = headers.get(HttpHeaders.RANGE);
    headers.remove(HttpHeaders.RANGE);

    Map<Repository, Response> successfulResponses;
    try {
      Map<Repository, Response> responses = getAll(context, groupFacet.members(), dispatched);
      successfulResponses = responses.entrySet().stream()
          .filter(entry -> entry.getValue().getStatus().getCode() == HttpStatus.OK)
          .filter(entry -> entry.getValue().getPayload() != null)
          // LinkedHashMap so that we maintain the order of the member responses.
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue, throwingMerger(), LinkedHashMap::new));
    }
    finally {
      // Restore range headers for processing by earlier handlers.
      headers.set(HttpHeaders.RANGE, rangeheader);
    }

    if (successfulResponses.size() == 1 && shouldReturnOnlyRespondingMember(context.getRepository())) {
      // When only a single member responds successfully we return its value and don't bother to persist.
      return Iterables.getOnlyElement(successfulResponses.values());
    }

    return merge(context, successfulResponses)
        .map(HttpResponses::ok)
        .orElseGet(HttpResponses::notFound);
  }

  private Optional<Content> merge(
      final Context context,
      final Map<Repository, Response> successfulResponses) throws IOException
  {
    Optional<String> optEtag = computeEtag(successfulResponses.values());
    Optional<Content> existing = getCached(context)
        .filter(unchanged(optEtag));

    if (existing.isPresent()) {
      // The etag we generate from the responses matches that of the stored value, so we skip merging and return the
      // previously computed content
      return existing;
    }

    return cooperation.on(() -> merge(context, successfulResponses, optEtag))
      .checkFunction(() -> Optional.of(getCached(context)))
      .cooperate(context.getRepository().getName(), cooperationKey(context));
  }

  /**
   * Create keys that should be used for cooperation in the context
   *
   * @param context the context of the request
   * @return an array of strings used for the cooperation key for the context
   */
  protected abstract String[] cooperationKey(Context context);

  /**
   * Consume & merge the responses provided to generate the merged metadata.
   *
   * @param context the context of the request
   * @param successfulResponses a list of successful responses to be merged
   * @param etag an etag which should be used to set as CONTENT_ETAG when present
   *
   * @return a constructed payload, implementors should not persist the value
   */
  protected abstract Optional<Content> merge(
      Context context,
      Map<Repository, Response> successfulResponses,
      Optional<String> etag) throws IOException;

  /**
   * Retrieve the cached content, it is expected that the repository's stale evaluation has already occurred.
   *
   * @param context the context of the request
   * @return an optional of the content if it exists, and is not stale
   */
  protected abstract Optional<Content> getCached(Context context);

  /*
   * Attempts to create a consistent hash derived from the responses of member repositories, we use this to identify
   * whether any members have changed their responses which allows us to avoid recomputing the values.
   */
  private Optional<String> computeEtag(final Collection<Response> successfulResponses) {
    Hasher etagDigest = etagDigester.newHasher();

    // If we're missing one etag the overall result isn't valid as we can't compute it
    AtomicBoolean valid = new AtomicBoolean(true);

    successfulResponses.stream()
        .map(Response::getPayload)
        .map(MergingGroupHandlerSupport::etag)
        .peek(optEtag -> valid.compareAndSet(true, optEtag.isPresent()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(etag -> etagDigest.putString(etag, StandardCharsets.UTF_8));

    if (valid.get()) {
      return Optional.of(etagDigest.hash().toString());
    }
    log.warn("Missing etag on response from member repository when computing metadata");
    return Optional.empty();
  }

  /*
   * Return an Optional of the Payload's etag
   */
  private static Optional<String> etag(final Payload payload) {
    if (!(payload instanceof Content)) {
      return Optional.empty();
    }
    AttributesMap attributes = ((Content) payload).getAttributes();

    CacheInfo cacheInfo = attributes.get(CacheInfo.class);
    if (cacheInfo != null && cacheInfo.getCacheToken() != null) {
      if (cacheInfo.getCacheToken() != null) {
        return Optional.of(cacheInfo.getCacheToken());
      }
      else {
        return Optional.of(ISODateTimeFormat.dateTime().print(cacheInfo.getLastVerified()));
      }
    }
    String etag = attributes.get(Content.CONTENT_ETAG, String.class);
    if (etag != null) {
      return Optional.of(etag);
    }
    return Optional.empty();
  }

  /*
   * Creates a Predicate which returns true
   */
  private Predicate<Content> unchanged(final Optional<String> etag) {
    if (!etag.isPresent()) {
      return __ -> false;
    }
    return content -> Optional.ofNullable(content.getAttributes().get(Content.CONTENT_ETAG))
        .map(etag.get()::equals)
        .orElse(false);
  }

  /*
   * Creates a predicate that will skip return false until the provided Predicate returns true once at which point the
   * created predicate will always return true.
   *
   * Use to skip lines until the header terminator is found.
   *
   */
  protected static Predicate<String> skipUntil(final Predicate<String> matcher) {
    final boolean[] done = new boolean[] {false};
    return line -> {
      if (done[0]) {
        return true;
      }
      done[0] = matcher.test(line);
      return false;
    };
  }

  /**
   * When {@code true} is returned if only a single member returns a successful response we return that member's
   * {@link Content}.
   */
  protected boolean shouldReturnOnlyRespondingMember(final Repository repository) {
    return true;
  }

  protected static <T> BinaryOperator<T> throwingMerger() {
    return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
  }
}
