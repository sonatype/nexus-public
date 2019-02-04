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
package org.sonatype.nexus.repository.pypi.internal;

import javax.annotation.Nonnull;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Router;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.matcherState;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;
import static org.sonatype.nexus.repository.pypi.internal.PyPiRecipeSupport.indexMatcher;
import static org.sonatype.nexus.repository.pypi.internal.PyPiRecipeSupport.packagesMatcher;
import static org.sonatype.nexus.repository.pypi.internal.PyPiRecipeSupport.rootIndexMatcher;
import static org.sonatype.nexus.repository.pypi.internal.PyPiRecipeSupport.searchMatcher;

public class PyPiRecipeSupportTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Request request;

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private Handler indexHandler = new Handler()
  {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) {
      String name = name(matcherState(context));
      assertThat(name, is("foo"));
      throw new RuntimeException("index");
    }
  };

  private Handler rootIndexHandler = new Handler() {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) {
      throw new RuntimeException("rootIndex");
    }
  };

  private Handler packagesHandler = new Handler() {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) {
      throw new RuntimeException("packages");
    }
  };

  private Handler searchHandler = new Handler() {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) {
      throw new RuntimeException("search");
    }
  };

  private Handler illegalHandler = new Handler() {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) {
      throw new IllegalStateException("search");
    }
  };

  private Router underTest;

  @Before
  public void setUp() throws Exception {
    Router.Builder builder = new Router.Builder();
    builder.defaultHandlers(illegalHandler);
    builder.route(indexMatcher().handler(indexHandler).create());
    builder.route(rootIndexMatcher().handler(rootIndexHandler).create());
    builder.route(packagesMatcher().handler(packagesHandler).create());
    builder.route(searchMatcher().handler(searchHandler).create());

    underTest = builder.create();
  }

  @Test
  public void routingForRootIndex() throws Exception {
    exception.expectMessage("rootIndex");
    exception.expect(RuntimeException.class);

    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(request.getPath()).thenReturn("/simple/");

    underTest.dispatch(repository, request, null);
  }

  @Test
  public void routingForIndex() throws Exception {
    exception.expectMessage("index");
    exception.expect(RuntimeException.class);

    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(request.getPath()).thenReturn("/simple/foo/");

    underTest.dispatch(repository, request, null);
  }

  @Test
  public void routingForIndexVariant() throws Exception {
    exception.expectMessage("index");
    exception.expect(RuntimeException.class);

    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(request.getPath()).thenReturn("/simple/foo");

    underTest.dispatch(repository, request, null);
  }

  @Test
  public void routingForPackages() throws Exception {
    exception.expectMessage("packages");
    exception.expect(RuntimeException.class);

    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(request.getPath()).thenReturn("/packages/foo/bar.tar.gz");

    underTest.dispatch(repository, request, null);
  }

  @Test
  public void routingForPackagesVariant() throws Exception {
    exception.expectMessage("packages");
    exception.expect(RuntimeException.class);

    when(request.getAction()).thenReturn(HttpMethods.GET);
    when(request.getPath()).thenReturn("/foo/bar.tar.gz");

    underTest.dispatch(repository, request, null);
  }

  @Test
  public void routingForSearch() throws Exception {
    exception.expectMessage("search");
    exception.expect(RuntimeException.class);

    when(request.getAction()).thenReturn(HttpMethods.POST);
    when(request.getPath()).thenReturn("/pypi");

    underTest.dispatch(repository, request, null);
  }
}
