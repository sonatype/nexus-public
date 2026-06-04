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
package org.sonatype.nexus.script.configuration.source;

import java.util.List;
import java.util.stream.Stream;

import org.sonatype.nexus.configuration.model.ApplyStatusXO;
import org.sonatype.nexus.configuration.model.ConfigurationXO;
import org.sonatype.nexus.configuration.model.InstanceConfigurationXO;
import org.sonatype.nexus.configuration.source.AbstractConfigurationSource;
import org.sonatype.nexus.script.ScriptManager;
import org.sonatype.nexus.script.configuration.model.ScriptConfigurationListXO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Collections.emptyList;
import static java.util.stream.StreamSupport.stream;

@Component
public class ScriptConfigurationSource
    extends AbstractConfigurationSource
{
  private static final Logger LOG = LoggerFactory.getLogger(ScriptConfigurationSource.class);

  private final ScriptManager scriptManager;

  @Autowired
  public ScriptConfigurationSource(final ScriptManager scriptManager) {
    this.scriptManager = scriptManager;
  }

  @Override
  protected List<ApplyStatusXO> doApplyToInstance(final InstanceConfigurationXO instanceConfigurationXO) {
    ScriptConfigurationListXO listXO =
        (ScriptConfigurationListXO) instanceConfigurationXO.getConfigurationXO(ScriptConfigurationListXO.TYPE_ID);
    if (listXO != null) {
      return listXO.getScriptConfigurationXOs().stream().flatMap(scriptConfigurationXO -> {
        try {
          if (scriptManager.get(scriptConfigurationXO.getName()) != null) {
            LOG.warn(
                "configuration already exists {}",
                scriptConfigurationXO.getName());
            return Stream.of(getNotAppliedStatus(": " + scriptConfigurationXO.getName() + " already exists"));
          }
          scriptManager.create(
              scriptConfigurationXO.getName(),
              scriptConfigurationXO.getContent(),
              scriptConfigurationXO.getType());
          return Stream.of(getAppliedStatus(": " + scriptConfigurationXO.getName()));
        }
        catch (Exception e) {
          LOG.error("Failed to create configuration: {}", scriptConfigurationXO.getName(), e);
          return Stream.of(getFailedStatus(": " + scriptConfigurationXO.getName() + " " + e.getMessage()));
        }
      }).toList();
    }
    return emptyList();
  }

  @Override
  public void applyToExport(final InstanceConfigurationXO instanceConfigurationXO) {
    instanceConfigurationXO.addConfigurationXO(ScriptConfigurationListXO.from(stream(
        scriptManager
            .browse()
            .spliterator(),
        false).toList()));
  }

  @Override
  public String getConfigurationTypeId() {
    return ScriptConfigurationListXO.TYPE_ID;
  }

  @Override
  public Class<? extends ConfigurationXO> getConfigurationXOType() {
    return ScriptConfigurationListXO.class;
  }
}
