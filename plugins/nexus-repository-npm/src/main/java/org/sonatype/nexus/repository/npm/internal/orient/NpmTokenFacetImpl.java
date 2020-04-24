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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.npm.internal.NpmJsonUtils;
import org.sonatype.nexus.repository.npm.internal.NpmResponses;
import org.sonatype.nexus.repository.npm.internal.NpmTokenFacet;
import org.sonatype.nexus.repository.npm.internal.security.NpmTokenManager;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link NpmTokenFacet}.
 *
 * @since 3.0
 */
@Named
public class NpmTokenFacetImpl
    extends FacetSupport
    implements NpmTokenFacet
{
  private final NpmTokenManager npmTokenManager;

  @Inject
  public NpmTokenFacetImpl(final NpmTokenManager npmTokenManager) {
    this.npmTokenManager = checkNotNull(npmTokenManager);
  }

  @Override
  public Response login(final Context context) {
    final Payload payload = context.getRequest().getPayload();
    if (payload == null) {
      return NpmResponses.badRequest("Missing body");
    }
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, NpmFacetUtils.HASH_ALGORITHMS)) {
      NestedAttributesMap request = NpmJsonUtils.parse(tempBlob);
      String token = npmTokenManager.login(request.get("name", String.class), request.get("password", String.class));
      if (null != token) {
        NestedAttributesMap response = new NestedAttributesMap("response", Maps.newHashMap());
        response.set("ok", Boolean.TRUE.toString());
        response.set("rev", "_we_dont_use_revs_any_more");
        response.set("id", "org.couchdb.user:undefined");
        response.set("token", token);
        return HttpResponses.created(new BytesPayload(NpmJsonUtils.bytes(response), ContentTypes.APPLICATION_JSON));
      }
      else {
        return NpmResponses.badCredentials("Bad username or password");
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Response logout(final Context context) {
    if (npmTokenManager.logout()) {
      NestedAttributesMap response = new NestedAttributesMap("response", Maps.newHashMap());
      response.set("ok", Boolean.TRUE.toString());
      return NpmResponses.ok(new BytesPayload(NpmJsonUtils.bytes(response), ContentTypes.APPLICATION_JSON));
    }
    else {
      return NpmResponses.notFound("Token not found");
    }
  }
}
