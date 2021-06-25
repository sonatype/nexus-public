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
package org.sonatype.repository.conan.internal.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.emptyMap;

/**
 * This class removes the absolute paths from download_url files so as to redirect back to this repository
 *
 * @since 3.32
 */
@Singleton
@Named
public class ConanUrlIndexer
    extends ComponentSupport
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public String updateAbsoluteUrls(
      final Context context,
      final Content content,
      final Repository repository) throws IOException
  {
    Map<String, URL> downloadUrlContents = readIndex(content.openInputStream());
    Map<String, String> remappedContents = new HashMap<>();

    State state = context.getAttributes().require(State.class);
    ConanCoords coords = ConanProxyHelper.convertFromState(state);

    for (Map.Entry<String, URL> entry : downloadUrlContents.entrySet()) {
      String fileName = entry.getKey();
      AssetKind assetKind = ConanProxyHelper.ASSET_KIND_FILENAMES.get(fileName);
      remappedContents.put(fileName, repository.getUrl() + ConanProxyHelper.getProxyAssetPath(coords, assetKind));
    }

    return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(remappedContents);
  }

  private Map<String, URL> readIndex(final InputStream stream) {
    TypeReference<HashMap<String, URL>> typeRef = new TypeReference<HashMap<String, URL>>() { };
    try {
      return MAPPER.readValue(stream, typeRef);
    }
    catch (IOException e) {
      log.warn("Unable to read index for asset", e);
    }
    return emptyMap();
  }

  public String findUrl(final InputStream inputStream, final String find) {
    Map<String, URL> downloadUrlContents = readIndex(inputStream);
    if (downloadUrlContents.containsKey(find)) {
      return downloadUrlContents.get(find).toString();
    }
    return null;
  }
}

