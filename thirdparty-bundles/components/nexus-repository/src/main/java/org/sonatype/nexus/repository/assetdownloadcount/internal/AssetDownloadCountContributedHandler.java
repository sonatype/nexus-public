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
package org.sonatype.nexus.repository.assetdownloadcount.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.assetdownloadcount.AssetDownloadCountStore;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.handlers.ContributedHandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * A {@link ContributedHandler} that will record asset downloads.
 *
 * @since 3.3
 */
@Named
@Singleton
public class AssetDownloadCountContributedHandler
    extends ComponentSupport
    implements ContributedHandler
{
  private final AssetDownloadCountStore assetDownloadCountStore;

  @Inject
  public AssetDownloadCountContributedHandler(final AssetDownloadCountStore assetDownloadCountStore)
  {
    this.assetDownloadCountStore = checkNotNull(assetDownloadCountStore);
  }

  @Override
  public Response handle(final Context context) throws Exception {
    Response response = context.proceed();

    if (assetDownloadCountStore.isEnabled() &&
        response != null &&
        response.getStatus().isSuccessful() &&
        isGetRequest(context.getRequest())) {
      Asset asset = getAssetFromPayload(response.getPayload());
      if (asset != null) {
        assetDownloadCountStore.incrementCount(context.getRepository().getName(), asset.name());
      }
    }

    return response;
  }

  private boolean isGetRequest(final Request request) {
    return GET.equals(request.getAction());
  }

  private Asset getAssetFromPayload(final Payload payload) {
    if (payload instanceof Content) {
      return ((Content) payload).getAttributes().get(Asset.class);
    }
    return null;
  }
}
