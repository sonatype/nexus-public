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
package org.sonatype.nexus.repository.golang.internal.hosted;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.util.GolangPathUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.repository.http.HttpResponses.created;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;
import static org.sonatype.nexus.repository.http.HttpResponses.ok;

/**
 * @since 3.17
 */
@Named
@Singleton
public class HostedHandlers
    extends ComponentSupport
{
  private GolangPathUtils pathUtils;

  @Inject
  public HostedHandlers(final GolangPathUtils pathUtils) {
    this.pathUtils = checkNotNull(pathUtils);
  }

  final Handler get = context -> {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    Content content;
    switch (assetKind) {
      case INFO:
        content = getInfo(context);
        break;
      case MODULE:
        content = getModule(context);
        break;
      case PACKAGE:
        content = getPackage(context);
        break;
      case LIST:
        content = getList(context);
        break;
      default:
        throw new RuntimeException(format("Unknown assetKind %s", assetKind.name()));
    }

    return (content != null) ? ok(content) : notFound();
  };

  private Content getList(final Context context) {
    State state = context.getAttributes().require(State.class);
    String module = pathUtils.module(state);
    return context.getRepository().facet(GolangHostedFacet.class).getList(module);
  }

  private Content getPackage(final Context context) {
    State state = context.getAttributes().require(State.class);
    String path = pathUtils.assetPath(state);
    return context.getRepository().facet(GolangHostedFacet.class).getPackage(path);
  }

  private Content getModule(final Context context) {
    State state = context.getAttributes().require(State.class);
    String path = pathUtils.assetPath(state);
    return context.getRepository().facet(GolangHostedFacet.class).getMod(path);
  }

  private Content getInfo(final Context context) {
    State state = context.getAttributes().require(State.class);
    String path = pathUtils.assetPath(state);
    GolangAttributes golangAttributes = pathUtils.getAttributesFromMatcherState(state);
    return context.getRepository().facet(GolangHostedFacet.class).getInfo(path, golangAttributes);
  }

  final Handler upload = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = pathUtils.assetPath(state);
    GolangAttributes golangAttributes = pathUtils.getAttributesFromMatcherState(state);

    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    context.getRepository().facet(GolangHostedFacet.class)
        .upload(path, golangAttributes, context.getRequest().getPayload(), assetKind);

    return created();
  };
}
