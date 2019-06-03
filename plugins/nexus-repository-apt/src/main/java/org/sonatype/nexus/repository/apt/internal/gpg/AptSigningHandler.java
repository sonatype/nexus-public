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
package org.sonatype.nexus.repository.apt.internal.gpg;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;


import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * @since 3.next
 */
@Named
@Singleton
public class AptSigningHandler
    extends ComponentSupport
    implements Handler
{
  @Override
  public Response handle(Context context) throws Exception {
    String path = assetPath(context);
    String method = context.getRequest().getAction();
    AptSigningFacet facet = context.getRepository().facet(AptSigningFacet.class);

    if ("repository-key.gpg".equals(path) && GET.equals(method)) {
      return HttpResponses.ok(facet.getPublicKey());
    }

    return context.proceed();
  }

  private String assetPath(Context context) {
    return context.getAttributes().require(AptSnapshotHandler.State.class).assetPath;
  }
}
