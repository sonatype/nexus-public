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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.extractLinksFromIndex;

public class PyPiGroupUtils
{
  private PyPiGroupUtils() {
    // empty
  }

  public static Supplier<String> lazyMergeResult(
      final String name,
      final AssetKind assetKind,
      final Map<Repository, Response> remoteResponses,
      final TemplateHelper templateHelper)
  {
    return Suppliers.memoize(() -> {
      try {
        return mergeResponses(name, assetKind, remoteResponses, templateHelper);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  private static String mergeResponses(
      final String name,
      final AssetKind assetKind,
      final Map<Repository, Response> remoteResponses,
      final TemplateHelper templateHelper) throws IOException
  {
    Map<String, PyPiLink> results = new TreeMap<>();
    for (Entry<Repository, Response> entry : remoteResponses.entrySet()) {
      Response response = entry.getValue();
      if (response.getStatus().getCode() == HttpStatus.OK && response.getPayload() != null) {
        processResults(response, results);
      }
    }

    if (INDEX == assetKind) {
      return PyPiIndexUtils.buildIndexPage(templateHelper, name, results.values());
    }
    else {
      return PyPiIndexUtils.buildRootIndexPage(templateHelper, results.values());
    }
  }

  /**
   * Processes the content of a particular repository's response.
   */
  private static void processResults(final Response response, final Map<String, PyPiLink> results) throws IOException {
    checkNotNull(response);
    checkNotNull(results);
    Payload payload = checkNotNull(response.getPayload());
    try (InputStream in = payload.openInputStream()) {
      extractLinksFromIndex(in)
          .forEach(link -> results.putIfAbsent(link.getFile().toLowerCase(), link));
    }
  }
}
