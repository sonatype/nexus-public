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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.plugins.plugin.console.api.dto.PluginInfoDTO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItems;

public class Nexus2810PluginConsoleIT
    extends AbstractPluginConsoleIT
{

  @Override
  protected void copyTestResources()
      throws IOException
  {
    super.copyTestResources();

    File source = this.getTestFile("broken-plugin");

    File desti = new File(this.getNexusBaseDir(), RELATIVE_PLUGIN_REPOSITORY_DIR);

    FileUtils.copyDirectory(source, desti);
  }

  @Test
  public void testListPluginInfos()
      throws Exception
  {
    String pluginName = "Nexus Plugin Console Plugin";

    List<PluginInfoDTO> pluginInfos = pluginConsoleMsgUtil.listPluginInfos();

    MatcherAssert.assertThat(getPluginsNames(pluginInfos),
        hasItems(pluginName, "Nexus Broken Plugin"));

    PluginInfoDTO pluginConsolePlugin =
        this.getPluginInfoByName(pluginInfos, pluginName);
    assertPropertyValid("Name", pluginConsolePlugin.getName(), pluginName);
    assertPropertyValid("Version", pluginConsolePlugin.getVersion());
    assertPropertyValid("Description", pluginConsolePlugin.getDescription(), "Adds a UI to view installed plugins.");
    assertPropertyValid("Status", pluginConsolePlugin.getStatus(), "ACTIVATED");
    if (new File("./.svn").exists()) {
      assertPropertyValid("SCM Version", pluginConsolePlugin.getScmVersion());
      assertPropertyValid("SCM Timestamp", pluginConsolePlugin.getScmTimestamp());
    }
    Assert.assertTrue(StringUtils.isEmpty(pluginConsolePlugin.getFailureReason()));

    PluginInfoDTO pgpPlugin = this.getPluginInfoByName(pluginInfos, "Nexus Broken Plugin");
    assertPropertyValid("Name", pgpPlugin.getName());
    assertPropertyValid("Version", pgpPlugin.getVersion());
    assertPropertyValid("Status", pgpPlugin.getStatus(), "BROKEN");
    Assert.assertNull(pgpPlugin.getDescription());
    Assert.assertEquals(pgpPlugin.getScmVersion(), "N/A");
    Assert.assertEquals(pgpPlugin.getScmTimestamp(), "N/A");
    assertPropertyValid("Site", pgpPlugin.getSite());
    Assert.assertFalse(StringUtils.isEmpty(pgpPlugin.getFailureReason()));
  }
}
