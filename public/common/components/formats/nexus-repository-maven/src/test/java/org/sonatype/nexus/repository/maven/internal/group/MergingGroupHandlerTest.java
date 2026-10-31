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
package org.sonatype.nexus.repository.maven.internal.group;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.view.Headers;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Regression tests for NEXUS-53780. Covers {@link MergingGroupHandler#toMemberRequest}:
 * <ul>
 * <li>An incoming HEAD group request is rewritten to a synthetic GET before member fan-out
 * so remote proxies fetch real bodies rather than body-less {@code HeaderOnlyPayload}s.
 * <li>Non-HEAD requests are returned unchanged (identity) so no unnecessary allocation or
 * attribute-copy overhead is incurred on the common GET path.
 * <li>The synthetic request's {@link AttributesMap} is a defensive copy — mutations by
 * downstream handlers (e.g. {@code DispatchedRepositories} tracking) must not leak back
 * into the outer HEAD request.
 * </ul>
 */
public class MergingGroupHandlerTest
{
  @Test
  public void headRequestIsRewrittenToGetForMemberDispatch() {
    Request head = requestOf(HttpMethods.HEAD, "/com/example/foo/maven-metadata.xml");

    Request memberRequest = MergingGroupHandler.toMemberRequest(head);

    assertThat(memberRequest.getAction(), is(HttpMethods.GET));
    assertThat(memberRequest.getPath(), is("/com/example/foo/maven-metadata.xml"));
    assertThat("headers should be forwarded to member handlers",
        memberRequest.getHeaders(), sameInstance(head.getHeaders()));
    assertThat("parameters should be forwarded to member handlers",
        memberRequest.getParameters(), sameInstance(head.getParameters()));
  }

  @Test
  public void getRequestIsReturnedUnchanged() {
    Request get = requestOf(HttpMethods.GET, "/com/example/foo/maven-metadata.xml");

    Request memberRequest = MergingGroupHandler.toMemberRequest(get);

    // For non-HEAD, no synthetic request is built: the original is returned by identity.
    assertThat(memberRequest, sameInstance(get));
  }

  @Test
  public void headRewriteInstallsFreshAttributesMapSoDownstreamMutationsDoNotLeakBack() {
    Request head = requestOf(HttpMethods.HEAD, "/com/example/foo/maven-metadata.xml");
    head.getAttributes().set("dispatched-marker", "outer");

    Request memberRequest = MergingGroupHandler.toMemberRequest(head);

    assertThat("attributes must be a fresh map, not the original by reference",
        memberRequest.getAttributes(), not(sameInstance(head.getAttributes())));
    assertThat("existing attribute values must be visible to member handlers",
        memberRequest.getAttributes().get("dispatched-marker", String.class), equalTo("outer"));

    // Mutating the synthetic request's attributes must not leak back into the outer HEAD
    // request's attribute map (e.g. DispatchedRepositories tracking, format-specific state).
    memberRequest.getAttributes().set("added-by-member", "leak");
    assertThat(head.getAttributes().get("added-by-member"), is(nullValue()));
  }

  private static Request requestOf(final String action, final String path) {
    return new Request.Builder()
        .action(action)
        .path(path)
        .headers(new Headers())
        .parameters(new Parameters())
        .attributes(new AttributesMap())
        .build();
  }
}
