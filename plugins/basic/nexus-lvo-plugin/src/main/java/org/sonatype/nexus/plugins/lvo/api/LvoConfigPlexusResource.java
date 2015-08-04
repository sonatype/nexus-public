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
package org.sonatype.nexus.plugins.lvo.api;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.plugins.lvo.api.dto.LvoConfigDTO;
import org.sonatype.nexus.plugins.lvo.api.dto.LvoConfigRequest;
import org.sonatype.nexus.plugins.lvo.api.dto.LvoConfigResponse;
import org.sonatype.nexus.plugins.lvo.config.LvoPluginConfiguration;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class LvoConfigPlexusResource
    extends AbstractPlexusResource
{
  private final LvoPluginConfiguration config;

  @Inject
  public LvoConfigPlexusResource(final LvoPluginConfiguration config) {
    this.config = checkNotNull(config);
    this.setModifiable(true);
  }

  @Override
  public Object getPayloadInstance() {
    return new LvoConfigRequest();
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/lvo_config", "authcBasic,perms[nexus:settings]");
  }

  @Override
  public String getResourceUri() {
    return "/lvo_config";
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    config.isEnabled();

    LvoConfigResponse resp = new LvoConfigResponse();

    LvoConfigDTO dto = new LvoConfigDTO();
    dto.setEnabled(config.isEnabled());

    resp.setData(dto);

    return resp;
  }

  @Override
  public Object put(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    LvoConfigRequest req = (LvoConfigRequest) payload;

    try {
      if (req.getData().isEnabled()) {
        config.enable();
      }
      else {
        config.disable();
      }
    }
    catch (IOException | ConfigurationException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to store lvo configuration", e);
    }

    return null;
  }
}
