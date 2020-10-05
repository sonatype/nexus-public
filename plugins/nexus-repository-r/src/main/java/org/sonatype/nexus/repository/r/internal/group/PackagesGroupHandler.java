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
package org.sonatype.nexus.repository.r.internal.group;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.r.internal.RException;
import org.sonatype.nexus.repository.r.internal.util.RPackagesUtils;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;

/**
 * Support for merging R PACKAGES.gz together.
 * @since 3.28
 */
@Named
@Singleton
class PackagesGroupHandler
    extends GroupHandler
{
  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final GroupHandler.DispatchedRepositories dispatched)
      throws Exception
  {
    GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);
    Map<Repository, Response> responses = getAll(context, groupFacet.members(), dispatched);
    List<Response> successfulResponses = responses.values().stream()
        .filter(response -> response.getStatus().getCode() == HttpStatus.OK && response.getPayload() != null)
        .collect(Collectors.toList());
    if (successfulResponses.isEmpty()) {
      return notFoundResponse(context);
    }
    if (successfulResponses.size() == 1) {
      return successfulResponses.get(0);
    }

    List<List<Map<String, String>>> parts = successfulResponses.stream().map(this::parseResponse).collect(
        Collectors.toList());
    List<Map<String, String>> merged = RPackagesUtils.merge(parts);

    return HttpResponses.ok(RPackagesUtils.buildPackages(merged));
  }

  protected List<Map<String, String>> parseResponse(@Nonnull final Response response) {
    Payload payload = checkNotNull(response.getPayload());
    try (InputStream in = payload.openInputStream()) {
      final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
      try (InputStream cin = compressorStreamFactory.createCompressorInputStream(GZIP, in)) {
        return RPackagesUtils.parseMetadata(cin);
      }
    }
    catch (IOException | CompressorException e) {
      throw new RException(null, e);
    }
  }

}
