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
package org.sonatype.nexus.testsuite.plugin.nexus2810;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.plugins.plugin.console.api.dto.PluginInfoDTO;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

public abstract class AbstractPluginConsoleIT
    extends AbstractNexusIntegrationTest
{

  protected PluginConsoleMessageUtil pluginConsoleMsgUtil = new PluginConsoleMessageUtil();

  public AbstractPluginConsoleIT() {
    super();
  }

  public AbstractPluginConsoleIT(String testRepositoryId) {
    super(testRepositoryId);
  }

  protected List<String> getPluginsNames(List<PluginInfoDTO> pluginInfos) {
    if (pluginInfos == null) {
      return null;
    }

    List<String> names = new ArrayList<String>();
    for (PluginInfoDTO pluginInfoDTO : pluginInfos) {
      names.add(pluginInfoDTO.getName());
    }
    return names;
  }

  protected PluginInfoDTO getPluginInfoByName(List<PluginInfoDTO> pluginInfos, String name) {
    for (PluginInfoDTO pluginInfo : pluginInfos) {
      if (pluginInfo.getName().equals(name)) {
        return pluginInfo;
      }
    }

    return null;
  }

  protected void assertPropertyValid(String name, String value, String... expectedValue) {
    if (StringUtils.isEmpty(value)) {
      Assert.fail("Property '" + name + "' is empty!");
    }

    if ("N/A".equals(value)) {
      Assert.fail("Property '" + name + "' is N/A!");
    }

    if (expectedValue != null && expectedValue.length > 0) {
      Assert.assertEquals(value, expectedValue[0]);
    }
  }

}