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
package org.sonatype.nexus.repository.view.handlers;

import java.io.IOException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;

/**
 * Updates the asset last downloaded time
 *
 * @since 3.next
 */
public class LastDownloadedHandler
    extends ComponentSupport
    implements Handler
{
  @Override
  public Response handle(final Context context) throws Exception {
    Response response = context.proceed();

    if (isSuccessfulRequestWithContent(context, response)) {
      Repository repository = context.getRepository();
      StorageFacet storageFacet = repository.facet(StorageFacet.class);
      
      Content content = (Content) response.getPayload();

      AttributesMap attributes = content.getAttributes();

      Asset asset = attributes.get(Asset.class);

      maybeUpdateLastDownloaded(storageFacet, asset);
    }

    return response;
  }

  private boolean isSuccessfulRequestWithContent(final Context context, final Response response) {
    return isGetOrHeadRequest(context)
        && isSuccessfulOrNotModified(response)
        && response.getPayload() != null
        && response.getPayload() instanceof Content;
  }
  
  private boolean isSuccessfulOrNotModified(final Response response) {
    return response.getStatus().isSuccessful() || response.getStatus().getCode() == 304;
  }

  private boolean isGetOrHeadRequest(final Context context) {
    String action = context.getRequest().getAction();
    
    return GET.equals(action) || HEAD.equals(action);
  }

  private void maybeUpdateLastDownloaded(final StorageFacet storageFacet, final Asset asset)
      throws IOException
  {
    if (asset != null && asset.markAsDownloaded()) {
      TransactionalStoreMetadata.operation.withDb(storageFacet.txSupplier()).throwing(IOException.class)
          .call(() -> {
            StorageTx tx = UnitOfWork.currentTx();
            tx.saveAsset(asset);
            return null;
          });
    }
  }
}
