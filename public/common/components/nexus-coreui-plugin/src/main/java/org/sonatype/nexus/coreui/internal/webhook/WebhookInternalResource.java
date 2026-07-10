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
package org.sonatype.nexus.coreui.internal.webhook;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.sonatype.nexus.coreui.ReferenceXO;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.webhooks.GlobalWebhook;
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook;
import org.sonatype.nexus.webhooks.WebhookService;
import org.sonatype.nexus.webhooks.WebhookType;

import jakarta.inject.Inject;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Produces(APPLICATION_JSON)
@Path(WebhookInternalResource.RESOURCE_PATH)
public class WebhookInternalResource
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/webhooks";

  private final WebhookService webhookService;

  @Inject
  public WebhookInternalResource(final WebhookService webhookService) {
    this.webhookService = checkNotNull(webhookService);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  public List<ReferenceXO> getWebhookTypes(@QueryParam("type") final String type) {
    WebhookType webhookType = "repository".equalsIgnoreCase(type)
        ? RepositoryWebhook.TYPE
        : GlobalWebhook.TYPE;

    return webhookService.getWebhooks()
        .stream()
        .filter(webhook -> webhook.getType() == webhookType)
        .map(webhook -> new ReferenceXO(webhook.getName(), webhook.getName()))
        .collect(Collectors.toList());
  }
}
