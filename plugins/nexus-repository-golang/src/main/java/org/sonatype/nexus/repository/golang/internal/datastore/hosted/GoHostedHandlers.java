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
package org.sonatype.nexus.repository.golang.internal.datastore.hosted;

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.internal.datastore.GoContentFacet;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.util.GolangPathUtils;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonatype.nexus.repository.golang.AssetKind.PACKAGE;
import static org.sonatype.nexus.repository.http.HttpResponses.created;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;

/**
 * Go's hosted handlers.
 *
 * @since 3.next
 */
@Named
@Singleton
public class GoHostedHandlers
    extends ComponentSupport
{
  final Handler get = context -> {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    Optional<Content> content;
    switch (assetKind) {
      case INFO:
        content = getInfo(context);
        break;
      case MODULE:
      case PACKAGE:
        content = getPackage(context);
        break;
      case LIST:
        content = getList(context);
        break;
      default:
        throw new IllegalArgumentException("Unsupported asset kind " + assetKind);
    }
    return content
        .map(HttpResponses::ok)
        .orElse(notFound());
  };

  private Optional<Content> getList(final Context context) {
    State state = context.getAttributes().require(State.class);
    String module = GolangPathUtils.module(state);
    return context.getRepository().facet(GoContentFacet.class).getVersions(module);
  }

  private Optional<Content> getPackage(final Context context) {
    State state = context.getAttributes().require(State.class);
    String path = GolangPathUtils.assetPath(state);
    return context.getRepository()
        .facet(GoContentFacet.class)
        .getAsset(path)
        .map(FluentAsset::download);
  }

  private Optional<Content> getInfo(final Context context) {
    State state = context.getAttributes().require(State.class);
    String path = GolangPathUtils.assetPath(state);
    GolangAttributes golangAttributes = GolangPathUtils.getAttributesFromMatcherState(state);
    return context.getRepository().facet(GoContentFacet.class).getInfo(path, golangAttributes);
  }

  final Handler upload = context -> {
    State state = context.getAttributes().require(State.class);
    String path = GolangPathUtils.assetPath(state);
    GolangAttributes golangAttributes = GolangPathUtils.getAttributesFromMatcherState(state);

    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    checkArgument(PACKAGE == assetKind, "Unsupported asset kind " + assetKind);

    context.getRepository().facet(GoContentFacet.class)
        .upload(path, golangAttributes, context.getRequest().getPayload());

    return created();
  };
}
