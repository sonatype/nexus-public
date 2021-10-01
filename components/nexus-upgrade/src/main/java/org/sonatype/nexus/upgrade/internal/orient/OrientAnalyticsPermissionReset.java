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
package org.sonatype.nexus.upgrade.internal.orient;

import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.upgrade.AnalyticsPermissionReset;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.parseBoolean;
import static java.util.stream.StreamSupport.stream;

/**
 * Used during the upgrade process to reset the previous response (if any) to analytics collection if permission to
 * collect analytics was previously declined.
 *
 * Note: this class is disabled by default as we don't want it to run on every OSS upgrade. To enable it, we must set
 * the 'enabledByDefault' property of the FeatureFlag annotation to true.
 *
 * @since 3.35
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
@FeatureFlag(name = "nexus.analytics.permission.reset.enabled")
public class OrientAnalyticsPermissionReset
    implements AnalyticsPermissionReset
{

  protected static final String OSS = "OSS";

  protected static final String P_ENABLED = "enabled";

  protected static final String P_PROPERTIES = "properties";

  protected static final String P_TYPE = "type";

  protected static final String ANALYTICS_CAPABILITY = "analytics-configuration";

  protected static final String SUBMIT_ANALYTICS = "submitAnalytics";

  private final Provider<DatabaseInstance> config;

  private final ApplicationVersion applicationVersion;

  private static final Logger LOG = LoggerFactory.getLogger(OrientAnalyticsPermissionReset.class);

  private static final String DB_CLASS = new OClassNameBuilder()
      .type("capability")
      .build();

  @Inject
  public OrientAnalyticsPermissionReset(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> config,
      final ApplicationVersion applicationVersion) {
    this.config = config;
    this.applicationVersion = applicationVersion;
  }

  @Override
  public void resetAnalyticsPermissionIfDisabled() {
    if (!OSS.equals(applicationVersion.getEdition())) {
      return;
    }

    LOG.debug("Checking analytics permission ...");
    try (ODatabaseDocumentTx db = config.get().connect()) {
      if (db.getMetadata().getSchema().existsClass(DB_CLASS)) {
        stream(db.browseClass(DB_CLASS).spliterator(), false)
            .filter(this::isAnalyticsCapability)
            .findFirst()
            .filter(this::isAnalyticsCapabilityDisabled)
            .ifPresent(this::deleteCapability);
      }
    }
  }

  private void deleteCapability(final ODocument document) {
    LOG.debug("Resetting analytics permission ...");
    document.delete();
  }

  private boolean isAnalyticsCapability(ODocument document) {
    return StringUtils.equals(ANALYTICS_CAPABILITY, document.field(P_TYPE, OType.STRING));
  }

  private boolean isAnalyticsCapabilityDisabled(ODocument document) {
    boolean enabled = document.field(P_ENABLED, OType.BOOLEAN);
    Map<String, String> properties = document.field(P_PROPERTIES, OType.EMBEDDEDMAP);
    return !enabled || !parseBoolean(properties.get(SUBMIT_ANALYTICS));
  }
}
