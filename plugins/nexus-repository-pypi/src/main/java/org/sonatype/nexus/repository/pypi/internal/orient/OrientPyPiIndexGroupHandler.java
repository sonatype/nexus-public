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
package org.sonatype.nexus.repository.pypi.internal.orient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils;
import org.sonatype.nexus.repository.pypi.internal.PyPiLink;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.extractLinksFromIndex;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;

/**
 * Support for merging PyPI simple indexes together.
 *
 * @since 3.1
 */
@Named
@Singleton
class OrientPyPiIndexGroupHandler
    extends GroupHandler
{
  private final TemplateHelper templateHelper;

  @Inject
  public OrientPyPiIndexGroupHandler(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
  }

  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final GroupHandler.DispatchedRepositories dispatched)
      throws Exception
  {
    checkNotNull(context);
    checkNotNull(dispatched);

    String name;
    AssetKind assetKind = context.getAttributes().get(AssetKind.class);
    if (ROOT_INDEX.equals(assetKind)) {
      name = INDEX_PATH_PREFIX;
    }
    else {
      name = name(context.getAttributes().require(TokenMatcher.State.class));
    }

    OrientPyPiGroupFacet groupFacet = context.getRepository().facet(OrientPyPiGroupFacet.class);
    Content content = groupFacet.getFromCache(name, assetKind);

    Map<Repository, Response> memberResponses = getAll(context, groupFacet.members(), dispatched);

    if (groupFacet.isStale(name, content, memberResponses)) {
      return HttpResponses.ok(
          groupFacet.buildIndexRoot(
              name, assetKind, lazyMergeResult(name, assetKind, memberResponses)));
    }

    return HttpResponses.ok(content);
  }

  private Supplier<String> lazyMergeResult(final String name,
                                           final AssetKind assetKind,
                                           final Map<Repository, Response> remoteResponses)
  {
    return Suppliers.memoize(() -> {
      try {
        return mergeResponses(name, assetKind, remoteResponses);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  private String mergeResponses(final String name,
                                final AssetKind assetKind,
                                final Map<Repository, Response> remoteResponses) throws IOException
  {
    Map<String, PyPiLink> results = new TreeMap<>();
    for (Entry<Repository, Response> entry : remoteResponses.entrySet()) {
      Response response = entry.getValue();
      if (response.getStatus().getCode() == HttpStatus.OK && response.getPayload() != null) {
        processResults(response, results);
      }
    }

    if (INDEX.equals(assetKind)) {
      return PyPiIndexUtils.buildIndexPage(templateHelper, name, results.values());
    }
    else {
      return PyPiIndexUtils.buildRootIndexPage(templateHelper, results.values());
    }
  }

  /**
   * Processes the content of a particular repository's response.
   */
  private void processResults(final Response response, final Map<String, PyPiLink> results) throws IOException {
    checkNotNull(response);
    checkNotNull(results);
    Payload payload = checkNotNull(response.getPayload());
    try (InputStream in = payload.openInputStream()) {
      extractLinksFromIndex(in)
          .forEach(link -> results.putIfAbsent(link.getFile().toLowerCase(), link));
    }
  }
}
