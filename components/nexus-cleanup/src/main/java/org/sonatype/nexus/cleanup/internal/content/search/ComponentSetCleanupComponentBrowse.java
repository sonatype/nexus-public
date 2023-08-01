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
package org.sonatype.nexus.cleanup.internal.content.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.internal.datastore.search.criteria.AssetCleanupEvaluator;
import org.sonatype.nexus.cleanup.internal.datastore.search.criteria.ComponentCleanupEvaluator;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

@Named("ComponentSetCleanupComponentBrowse")
@Singleton
public class ComponentSetCleanupComponentBrowse
    extends DataStoreCleanupComponentBrowse
    implements CleanupComponentBrowse
{
  @Inject
  public ComponentSetCleanupComponentBrowse(
      final Map<String, ComponentCleanupEvaluator> componentCriteria,
      final Map<String, AssetCleanupEvaluator> assetCriteria)
  {
    super(componentCriteria, assetCriteria);
  }

  /*
  Example Criteria map (1 day for times) :
    {regex=.*, isPrerelease=true, lastDownloaded=86400, lastBlobUpdated=86400}
   */
  @Override
  protected Map<String, String> getFilterableCriteria(final Repository repository, final CleanupPolicy policy) {
    Map<String, String> criteria = policy.getCriteria();
    FluentComponents fluentComponents = repository.facet(ContentFacet.class).components();
    Set<String> processedCriteria = fluentComponents.getProcessedCleanupCriteria();
    HashMap<String, String> applicableCriteria = new HashMap<>();
    criteria.forEach((k, v) -> {
      if (!processedCriteria.contains(k)) {
        applicableCriteria.put(k, v);
      }
    });
    return applicableCriteria;
  }

  @Override
  protected ContinuationBrowse<FluentComponent> getComponentBrowser(
      final Repository repository,
      final CleanupPolicy policy)
  {
    log.debug("Using Retain-N code pathway");
    return new ComponentSetCleanupBrowser(repository, policy);
  }
}
