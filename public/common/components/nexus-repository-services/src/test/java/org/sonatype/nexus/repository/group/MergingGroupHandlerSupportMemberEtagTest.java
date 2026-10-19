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
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Verifies the fallback order of {@link MergingGroupHandlerSupport#memberEtag}, extracted
 * unchanged from the pre-existing private {@code etag(Payload)} method (NEXUS-55727 - this
 * extraction is a pure refactor so format-specific handlers, like PyPI's P7 fold, can override
 * {@code memberEtag} without duplicating the fallback chain).
 */
public class MergingGroupHandlerSupportMemberEtagTest
{
  private final TestableMergingGroupHandler underTest = new TestableMergingGroupHandler();

  @Test
  public void memberEtag_prefersCacheTokenThenEtag() {
    Content content = new Content(new BytesPayload(new byte[0], "text/plain"));
    content.getAttributes().set(CacheInfo.class, new CacheInfo(DateTime.now(), "cache-token-value"));
    content.getAttributes().set(Content.CONTENT_ETAG, "etag-value");

    assertThat(underTest.memberEtag(content), is(Optional.of("cache-token-value")));
  }

  @Test
  public void memberEtag_fallsBackToLastVerifiedWhenNoCacheToken() {
    Content content = new Content(new BytesPayload(new byte[0], "text/plain"));
    DateTime lastVerified = DateTime.now();
    content.getAttributes().set(CacheInfo.class, new CacheInfo(lastVerified, null));
    content.getAttributes().set(Content.CONTENT_ETAG, "etag-value");

    // CacheInfo.getLastVerified() wins over CONTENT_ETAG, confirming the cache-token-then-etag
    // order even when the cache token itself is absent but CacheInfo is present.
    assertThat(underTest.memberEtag(content), is(not(Optional.of("etag-value"))));
    assertThat(underTest.memberEtag(content).isPresent(), is(true));
  }

  @Test
  public void memberEtag_fallsBackToEtagWhenNoCacheInfo() {
    Content content = new Content(new BytesPayload(new byte[0], "text/plain"));
    content.getAttributes().set(Content.CONTENT_ETAG, "etag-value");

    assertThat(underTest.memberEtag(content), is(Optional.of("etag-value")));
  }

  @Test
  public void memberEtag_fallsBackToLastModifiedWhenNothingElse() {
    Content content = new Content(new BytesPayload(new byte[0], "text/plain"));
    content.getAttributes().set(Content.CONTENT_LAST_MODIFIED, DateTime.now());

    assertThat(underTest.memberEtag(content).isPresent(), is(true));
  }

  @Test
  public void memberEtag_absentWhenNothingPresent() {
    Content content = new Content(new BytesPayload(new byte[0], "text/plain"));

    assertThat(underTest.memberEtag(content), is(Optional.empty()));
  }

  /**
   * Minimal concrete subclass to exercise the protected {@code memberEtag} hook directly;
   * {@link MergingGroupHandlerSupport}'s other abstract methods are not exercised by these tests.
   */
  private static class TestableMergingGroupHandler
      extends MergingGroupHandlerSupport
  {
    @Override
    protected String[] cooperationKey(final Context context) {
      return new String[0];
    }

    @Override
    protected Optional<Content> merge(
        final Context context,
        final Map<Repository, Response> successfulResponses,
        final Optional<String> etag) throws IOException
    {
      return Optional.empty();
    }

    @Override
    protected Optional<Content> getCached(final Context context) {
      return Optional.empty();
    }
  }
}
