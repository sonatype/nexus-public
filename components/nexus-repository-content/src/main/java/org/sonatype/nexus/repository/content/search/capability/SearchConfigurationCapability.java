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
package org.sonatype.nexus.repository.content.search.capability;

import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.common.template.TemplateParameters;

@Named(SearchConfigurationCapabilityDescriptor.TYPE_ID)
public class SearchConfigurationCapability
    extends CapabilitySupport<Integer>
{
  /*
   * Update {@link SearchConfigurationCapabilityDescriptor.Messages} when changing this value
   */
  public static final int DEFAULT_REFETCH_LIMIT = 10;

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Refetch Limit: %s")
    String description(int count);
  }

  private static final Messages messages = I18N.create(Messages.class);

  static final String REFETCH_LIMIT = "refetch.limit";

  @Override
  protected Integer createConfig(final Map<String, String> properties) throws Exception {
    return Optional.ofNullable(properties.get(REFETCH_LIMIT))
        .map(Integer::valueOf)
        .orElse(null);
  }

  @Override
  protected String renderDescription() throws Exception {
    // Only show count when active else store will be stopped and details inaccessible
    if (context().isActive()) {
      return messages.description(getRefetchLimit());
    }
    return null;
  }

  @Override
  protected String renderStatus() throws Exception {
    if (!context().isActive()) {
      return null;
    }
    return render("searchconfiguration-status.vm", new TemplateParameters()
        .set("refetchLimit", getRefetchLimit()));
  }

  private int getRefetchLimit() {
    Integer refetch = getConfig();
    return refetch == null ? DEFAULT_REFETCH_LIMIT : refetch;
  }
}
