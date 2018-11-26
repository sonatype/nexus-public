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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.ACTION_FILE_UPLOAD;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.FIELD_ACTION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.FIELD_CONTENT;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.name;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.packagesPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.path;
import static org.sonatype.nexus.repository.pypi.internal.PyPiSearchUtils.buildSearchResponse;
import static org.sonatype.nexus.repository.pypi.internal.PyPiSearchUtils.parseSearchRequest;

/**
 * PyPI hosted handlers.
 *
 * @since 3.1
 */
@Named
@Singleton
public final class HostedHandlers
    extends ComponentSupport
{
  private final SearchService searchService;

  @Inject
  public HostedHandlers(final SearchService searchService) {
    this.searchService = checkNotNull(searchService);
  }

  /**
   * Handle request for root index.
   */
  final Handler getRootIndex = context -> {
    Content content = context.getRepository().facet(PyPiHostedFacet.class).getRootIndex();
    if (content != null) {
      return HttpResponses.ok(content);
    }
    return HttpResponses.notFound();
  };

  /**
   * Handle request for index.
   */
  final Handler getIndex = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    Content content = context.getRepository().facet(PyPiHostedFacet.class).getIndex(name(state));
    if (content != null) {
      return HttpResponses.ok(content);
    }
    return HttpResponses.notFound();
  };

  /**
   * Handle request for package.
   */
  final Handler getPackage = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    Content content = context.getRepository().facet(PyPiHostedFacet.class).getPackage(packagesPath(path(state)));
    if (content != null) {
      return HttpResponses.ok(content);
    }
    return HttpResponses.notFound();
  };

  /**
   * Handle request to register or upload (depending on the :action parameter).
   */
  final Handler postContent = context -> {
    Map<String, String> attributes = new LinkedHashMap<>();
    try (TempBlobPartPayload upload = extractPayloads(context, attributes)) {
      String action = attributes.get(FIELD_ACTION);
      if (!ACTION_FILE_UPLOAD.equals(action)) {
        throw new IllegalStateException("Unsupported :action, found: " + action);
      }
      context.getRepository().facet(PyPiHostedFacet.class).upload(upload.getName(), attributes, upload);
      return HttpResponses.ok();
    }
  };

  /**
   * Utility method to extract the uploaded content. Populates the attributes map with any other metadata. One and only
   * one file upload with a field name of "content" should be present in the upload, or {@link IllegalStateException}
   * is thrown to indicate unexpected input.
   */
  private TempBlobPartPayload extractPayloads(final Context context, final Map<String, String> attributes) throws IOException {
    checkNotNull(context);
    checkNotNull(attributes);
    Repository repository = context.getRepository();
    Request request = context.getRequest();
    Iterable<PartPayload> payloads = checkNotNull(request.getMultiparts());
    TempBlobPartPayload upload = null;
    for (PartPayload payload : payloads) {
      if (payload.isFormField()) {
        addAttribute(attributes, payload);
      }
      else if (FIELD_CONTENT.equals(payload.getFieldName())) {
        if (upload != null) {
          throw new IllegalStateException();
        }
        StorageFacet storageFacet = repository.facet(StorageFacet.class);
        upload = new TempBlobPartPayload(payload, storageFacet.createTempBlob(payload, HASH_ALGORITHMS));
      }
    }
    if (upload == null) {
      throw new IllegalStateException();
    }
    return upload;
  }

  /**
   * Adds the attribute from the payload to the attribute map. If an attribute with the same name already exists, the
   * content is concatenated with a newline.
   */
  private void addAttribute(final Map<String, String> attributes, final PartPayload payload) throws IOException {
    checkNotNull(attributes);
    checkNotNull(payload);
    try (Reader reader = new BufferedReader(new InputStreamReader(payload.openInputStream(), StandardCharsets.UTF_8))) {
      String fieldName = payload.getFieldName();
      String newValue = CharStreams.toString(reader);
      String oldValue = attributes.get(payload.getFieldName());
      if (oldValue != null && !oldValue.isEmpty()) {
        newValue = oldValue + "\n" + newValue;
      }
      attributes.put(fieldName, newValue);
    }
  }

  /**
   * Handle request for search.
   */
  public Handler search() {
    return context -> {
      Payload payload = checkNotNull(context.getRequest().getPayload());
      try (InputStream is = payload.openInputStream()) {
        QueryBuilder query = parseSearchRequest(context.getRepository().getName(), is);
        List<PyPiSearchResult> results = new ArrayList<>();
        for (SearchHit hit : searchService.browseUnrestricted(query)) {
          Map<String, Object> source = hit.getSource();
          Map<String, Object> formatAttributes = (Map<String, Object>) source.getOrDefault(
              MetadataNodeEntityAdapter.P_ATTRIBUTES, Collections.emptyMap());
          Map<String, Object> pypiAttributes = (Map<String, Object>) formatAttributes.getOrDefault(PyPiFormat.NAME,
              Collections.emptyMap());
          String name = Strings.nullToEmpty((String) pypiAttributes.get(PyPiAttributes.P_NAME));
          String version = Strings.nullToEmpty((String) pypiAttributes.get(PyPiAttributes.P_VERSION));
          String summary = Strings.nullToEmpty((String) pypiAttributes.get(PyPiAttributes.P_SUMMARY));
          results.add(new PyPiSearchResult(name, version, summary));
        }
        String response = buildSearchResponse(results);
        return HttpResponses.ok(new StringPayload(response, ContentTypes.APPLICATION_XML));
      }
    };
  }
}
