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
package org.sonatype.nexus.proxy.maven.metadata;

import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.artifact.repository.metadata.Plugin;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class GroupDirMetadataProcessorTest
{

  @Test
  public void testNoModelVersionForPluginGroupMetadata()
      throws IOException
  {
    DefaultMetadataHelper helper = new DefaultMetadataHelper(null, null)
    {

      @Override
      public void store(String content, String path)
          throws IOException
      {

        assertThat(content, not(containsString("modelVersion")));
      }

    };
    Plugin plugin = new Plugin();
    plugin.setName("pName");
    plugin.setArtifactId("aid");
    plugin.setPrefix("pPrefix");
    helper.gData.put("/gid", Arrays.asList(plugin));

    GroupDirMetadataProcessor processor = new GroupDirMetadataProcessor(helper);
    processor.processMetadata("/gid");
  }

}
