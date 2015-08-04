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
package org.sonatype.nexus.plugins.plugin.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugin.support.DocumentationBundle;
import org.sonatype.nexus.plugins.NexusPluginManager;
import org.sonatype.nexus.plugins.PluginResponse;
import org.sonatype.nexus.plugins.plugin.console.model.DocumentationLink;
import org.sonatype.nexus.plugins.plugin.console.model.PluginInfo;
import org.sonatype.nexus.web.WebResourceBundle;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link PluginConsoleManager} implementation.
 */
@Named
@Singleton
public class DefaultPluginConsoleManager
    extends ComponentSupport
    implements PluginConsoleManager
{
  private final NexusPluginManager pluginManager;

  private final List<WebResourceBundle> resourceBundles;

  private final Multimap<String, DocumentationBundle> docBundles;

  @Inject
  public DefaultPluginConsoleManager(final NexusPluginManager pluginManager,
                                     final List<WebResourceBundle> resourceBundles)
  {
    this.pluginManager = checkNotNull(pluginManager);
    this.resourceBundles = checkNotNull(resourceBundles);

    this.docBundles = LinkedHashMultimap.create();
    for (WebResourceBundle rb : resourceBundles) {
      if (rb instanceof DocumentationBundle) {
        DocumentationBundle doc = (DocumentationBundle) rb;

        docBundles.put(doc.getPluginId(), doc);
      }
    }
  }

  public List<PluginInfo> listPluginInfo() {
    List<PluginInfo> result = new ArrayList<PluginInfo>();

    Map<GAVCoordinate, PluginResponse> pluginResponses = pluginManager.getPluginResponses();

    for (PluginResponse pluginResponse : pluginResponses.values()) {
      result.add(buildPluginInfo(pluginResponse));
    }

    return result;
  }

  private PluginInfo buildPluginInfo(PluginResponse pluginResponse) {
    PluginInfo result = new PluginInfo();

    result.setStatus(pluginResponse.getAchievedGoal().name());
    result.setVersion(pluginResponse.getPluginCoordinates().getVersion());
    if (pluginResponse.getPluginDescriptor() != null) {
      result.setName(pluginResponse.getPluginDescriptor().getPluginMetadata().getName());
      result.setDescription(pluginResponse.getPluginDescriptor().getPluginMetadata().getDescription());
      result.setScmVersion(pluginResponse.getPluginDescriptor().getPluginMetadata().getScmVersion());
      result.setScmTimestamp(pluginResponse.getPluginDescriptor().getPluginMetadata().getScmTimestamp());
      result.setSite(pluginResponse.getPluginDescriptor().getPluginMetadata().getPluginSite());
    }
    else {
      result.setName(pluginResponse.getPluginCoordinates().getGroupId() + ":"
          + pluginResponse.getPluginCoordinates().getArtifactId());
    }

    Collection<DocumentationBundle> docs =
        docBundles.get(pluginResponse.getPluginCoordinates().getArtifactId());
    if (docs != null && !docs.isEmpty()) {
      for (DocumentationBundle bundle : docs) {
        // here, we (mis)use the documentation field, to store path segments only, the REST resource will create
        // proper URLs out this these.
        DocumentationLink link = new DocumentationLink();
        link.setLabel(bundle.getDescription());
        link.setUrl(bundle.getPluginId() + "/" + bundle.getPathPrefix());
        result.addDocumentation(link);
      }
    }

    if (!pluginResponse.isSuccessful()) {
      result.setFailureReason(pluginResponse.formatAsString(false));
    }

    return result;
  }
}
