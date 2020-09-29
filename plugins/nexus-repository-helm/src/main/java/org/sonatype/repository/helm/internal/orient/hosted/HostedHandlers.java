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
package org.sonatype.repository.helm.internal.orient.hosted;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.util.HelmPathUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;
import static org.sonatype.nexus.repository.http.HttpResponses.ok;

/**
 * Helm Hosted Handlers
 *
 * @since 3.28
 */
@Named
@Singleton
public class HostedHandlers
    extends ComponentSupport
{
  private HelmPathUtils helmPathUtils;

  @Inject
  public HostedHandlers(final HelmPathUtils helmPathUtils) {
    this.helmPathUtils = checkNotNull(helmPathUtils);
  }

  public final Handler get = context -> {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    String path;
    if (assetKind == AssetKind.HELM_INDEX) {
      path = "index.yaml";
    }
    else {
      State state = context.getAttributes().require(State.class);
      path = helmPathUtils.filename(state);
    }
    Content content = context.getRepository().facet(HelmHostedFacet.class).get(path);

    return (content != null) ? ok(content) : notFound();
  };

  public final Handler upload = context -> {
    State state = context.getAttributes().require(State.class);
    String path = helmPathUtils.buildAssetPath(state);
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    context.getRepository().facet(HelmHostedFacet.class).upload(path, context.getRequest().getPayload(), assetKind);
    return ok();
  };

  public final Handler delete = context -> {
    State state = context.getAttributes().require(State.class);
    String path = helmPathUtils.buildAssetPath(state);

    boolean deleted = context.getRepository().facet(HelmHostedFacet.class).delete(path);

    return (deleted) ? ok() : notFound();
  };
}
