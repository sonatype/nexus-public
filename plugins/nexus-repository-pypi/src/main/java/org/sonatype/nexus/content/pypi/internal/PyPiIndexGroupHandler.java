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
package org.sonatype.nexus.content.pypi.internal;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.content.pypi.internal.ContentPypiPathUtils.indexPath;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiGroupUtils.lazyMergeResult;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;

/**
 * Support for merging PyPI simple indexes together.
 *
 * @since 3.next
 */
@Named
@Singleton
class PyPiIndexGroupHandler
    extends GroupHandler
{
  private final TemplateHelper templateHelper;

  @Inject
  public PyPiIndexGroupHandler(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
  }

  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    checkNotNull(context);
    checkNotNull(dispatched);

    String name;
    AssetKind assetKind = context.getAttributes().get(AssetKind.class);
    if (ROOT_INDEX == assetKind) {
      name = indexPath();
    }
    else {
      name = "/" + name(context.getAttributes().require(TokenMatcher.State.class));
    }

    PyPiGroupFacet groupFacet = context.getRepository().facet(PyPiGroupFacet.class);
    Content content = groupFacet.getFromCache(name, assetKind);

    Map<Repository, Response> memberResponses = getAll(context, groupFacet.members(), dispatched);

    if (groupFacet.isStale(name, content, memberResponses)) {
      return HttpResponses.ok(
          groupFacet.buildIndexRoot(
              name, assetKind, lazyMergeResult(name, assetKind, memberResponses, templateHelper)));
    }

    return HttpResponses.ok(content);
  }
}
