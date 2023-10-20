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
package org.sonatype.nexus.repository.routing;

import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * {@link RoutingRule} auditor.
 *
 */
@Named
@Singleton
public class RoutingRuleAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "RoutingRule";

  public RoutingRuleAuditor() {
    registerType(RoutingRuleCreatedEvent.class, CREATED_TYPE);
    registerType(RoutingRuleUpdatedEvent.class, UPDATED_TYPE);
    registerType(RoutingRuleDeletedEvent.class, DELETED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RoutingRuleEvent event) {
    if (isRecording()) {
      RoutingRule routingRule = event.getRoutingRule();

      AuditData data = getRoutingRuleAuditData(event, routingRule);
      record(data);
    }
  }

  private AuditData getRoutingRuleAuditData(final RoutingRuleEvent event, final RoutingRule routingRule) {
    AuditData data = new AuditData();
    data.setDomain(DOMAIN);
    data.setType(type(event.getClass()));
    data.setContext(routingRule.name());

    Map<String, Object> attributes = data.getAttributes();
    attributes.put("description", routingRule.description());
    attributes.put("mode", routingRule.mode());
    attributes.put("matchers", routingRule.matchers());
    return data;
  }
}
