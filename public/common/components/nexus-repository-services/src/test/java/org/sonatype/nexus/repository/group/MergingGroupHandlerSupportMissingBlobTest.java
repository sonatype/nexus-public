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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.IOCall;
import org.sonatype.nexus.common.cooperation2.IOCheck;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import org.junit.Before;
import org.junit.Test;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A cached merged asset whose blob is gone from the blob store must be a cache miss, so the group
 * rebuilds it from its members, instead of failing the request.
 */
public class MergingGroupHandlerSupportMissingBlobTest
{
  private final AtomicInteger merges = new AtomicInteger();

  private final AtomicInteger cachedReads = new AtomicInteger();

  private TestHandler underTest;

  private Context context;

  @Before
  public void setUp() throws Exception {
    underTest = new TestHandler();
    underTest.configureCooperation(factoryRunningWorkInline(), false, ofSeconds(1), ofSeconds(1), 1);

    Request request = mock(Request.class);
    when(request.getHeaders()).thenReturn(new Headers());

    Repository member1 = member("member1");
    Repository member2 = member("member2");

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.members()).thenReturn(List.of(member1, member2));
    Repository group = mock(Repository.class);
    when(group.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(group.getName()).thenReturn("group");

    context = mock(Context.class);
    when(context.getRepository()).thenReturn(group);
    when(context.getRequest()).thenReturn(request);
  }

  @Test
  public void missingCachedBlob_isTreatedAsCacheMiss_andRebuilt() throws Exception {
    Response response = underTest.doGet(context, new DispatchedRepositories());

    assertThat(response.getStatus().getCode(), is(200));
    assertThat(merges.get(), is(1));
    // the direct read, then the cooperation check
    assertThat(cachedReads.get(), is(2));
  }

  private Repository member(final String name) throws Exception {
    Content content = new Content(new BytesPayload("x".getBytes(), "text/plain"));
    content.getAttributes().set(Content.CONTENT_ETAG, name + "-etag");
    ViewFacet view = mock(ViewFacet.class);
    when(view.dispatch(any(), any())).thenReturn(HttpResponses.ok(content));
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);
    when(repository.facet(ViewFacet.class)).thenReturn(view);
    return repository;
  }

  /** Cooperation that runs the check then the work on the calling thread. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Cooperation2Factory factoryRunningWorkInline() throws Exception {
    Cooperation2 cooperation = mock(Cooperation2.class);
    when(cooperation.on(any())).thenAnswer(invocation -> {
      IOCall work = invocation.getArgument(0);
      IOCheck[] check = new IOCheck[1];
      Cooperation2.Builder builder = mock(Cooperation2.Builder.class);
      when(builder.checkFunction(any())).thenAnswer(i -> {
        check[0] = i.getArgument(0);
        return builder;
      });
      when(builder.cooperate(anyString(), any())).thenAnswer(i -> {
        check[0].check();
        return work.call();
      });
      return builder;
    });
    Cooperation2Factory.Builder configured = mock(Cooperation2Factory.Builder.class, RETURNS_SELF);
    when(configured.build(any(Class.class))).thenReturn(cooperation);
    Cooperation2Factory factory = mock(Cooperation2Factory.class);
    when(factory.configure()).thenReturn(configured);
    return factory;
  }

  private class TestHandler
      extends MergingGroupHandlerSupport
  {
    @Override
    protected String[] cooperationKey(final Context context) {
      return new String[]{"key"};
    }

    @Override
    protected Optional<Content> merge(
        final Context context,
        final Map<Repository, Response> successfulResponses,
        final Optional<String> etag) throws IOException
    {
      merges.incrementAndGet();
      return Optional.of(new Content(new BytesPayload("merged".getBytes(), "text/plain")));
    }

    @Override
    protected Optional<Content> getCached(final Context context) {
      cachedReads.incrementAndGet();
      MissingBlobException missing = new MissingBlobException(mock(BlobRef.class));
      throw missing;
    }
  }
}
