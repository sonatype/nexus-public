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
package org.sonatype.repository.conan.internal.hosted;

import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.DIGEST;
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.GROUP;
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.PATH;
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.PROJECT;
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.STATE;
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.VERSION;

/**
 * @since 3.next
 */
public class ConanHostedHelper
{
  public static final ObjectMapper MAPPER = new ObjectMapper();

  public static final String CONAN_HOSTED_PREFIX = "conans/";

  public static String getHostedAssetPath(final ConanCoords coord, final AssetKind assetKind) {
    String path = String.format("%s/%s/%s/%s%s",
        coord.getGroup(),
        coord.getProject(),
        coord.getVersion(),
        coord.getChannel(),
        coord.getSha() == null ? StringUtils.EMPTY : "/packages/" + coord.getSha());
    return String.format("%s%s/%s", CONAN_HOSTED_PREFIX, path, assetKind.getFilename());
  }

  public static ConanCoords convertFromState(final TokenMatcher.State state) {
    return new ConanCoords(
        state.getTokens().get(PATH),
        state.getTokens().get(GROUP),
        state.getTokens().get(PROJECT),
        state.getTokens().get(VERSION),
        state.getTokens().get(STATE),
        state.getTokens().getOrDefault(DIGEST, null)
    );
  }
}
