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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * A helper class to produce an appropriate HTTP Response for asset download requests.
 *
 * @since 3.7
 */
public class AssetDownloadResponseProcessor
{
  public static final String NO_SEARCH_RESULTS_FOUND = "Asset search returned no results";

  public static final String SEARCH_RETURNED_MULTIPLE_ASSETS = "Search returned multiple assets, please refine search criteria to find a single asset";

  private final List<AssetXO> assetXOs;

  AssetDownloadResponseProcessor(final List<AssetXO> assetXOs) {
    this.assetXOs = assetXOs;
  }

  /**
   * Produces the appropriate http response based upon the assets list. Initial
   * implementation is returning a BAD_REQUEST when collection has more than one asset
   * as there is no means for determining the appropriate asset in the absence of a
   * selection strategy (future work).
   * @return response the appropriate {@link Response} based on the input asset list
   */
   Response process() {
    if (assetXOs.isEmpty()) {
      throw new WebApplicationMessageException(NOT_FOUND, NO_SEARCH_RESULTS_FOUND);
    }

    if (assetXOs.size() > 1)  {
      throw new WebApplicationMessageException(BAD_REQUEST, SEARCH_RETURNED_MULTIPLE_ASSETS);
    }

    //Case when only a single assetXO is present
    return getResponse(assetXOs.get(0));
  }

  /**
   * Builds the response with the provided assetXO object
   * @param assetXO the {@link AssetXO} to be downloaded
   * @return response the redirect {@link Response} containing the download url
   */
  private Response getResponse(final AssetXO assetXO) {
    String redirectUrl = assetXO.getDownloadUrl();
    URI uri = UriBuilder.fromPath(redirectUrl).build();
    ResponseBuilder builder = Response.status(Status.FOUND).location(uri);
    return builder.build();
  }
}
