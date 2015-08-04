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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.proxy.maven.metadata.operations.AddPluginOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility;
import org.sonatype.nexus.proxy.maven.metadata.operations.PluginOperand;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;

import static org.sonatype.nexus.proxy.maven.metadata.operations.MetadataUtil.isPluginEquals;

/**
 * Process maven metadata in plugin group directory
 *
 * @author juven
 */
public class GroupDirMetadataProcessor
    extends AbstractMetadataProcessor
{
  public GroupDirMetadataProcessor(AbstractMetadataHelper metadataHelper) {
    super(metadataHelper);
  }

  @Override
  public void processMetadata(String path)
      throws IOException
  {
    Metadata md = createMetadata(path);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    MetadataBuilder.write(md, outputStream);

    String mdString = outputStream.toString();

    outputStream.close();

    metadataHelper.store(mdString, path + AbstractMetadataHelper.METADATA_SUFFIX);

  }

  private Metadata createMetadata(String path)
      throws IOException
  {
    try {
      Metadata md = new Metadata();

      List<MetadataOperation> ops = new ArrayList<MetadataOperation>();

      for (Plugin plugin : metadataHelper.gData.get(path)) {
        ops.add(new AddPluginOperation(new PluginOperand(ModelVersionUtility.LATEST_MODEL_VERSION, plugin)));
      }

      MetadataBuilder.changeMetadata(md, ops);

      return md;
    }
    catch (MetadataException e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean shouldProcessMetadata(String path) {
    Collection<Plugin> plugins = metadataHelper.gData.get(path);

    if (plugins != null && !plugins.isEmpty()) {
      return true;
    }

    return false;
  }

  @Override
  public void postProcessMetadata(String path) {
    metadataHelper.gData.remove(path);
  }

  @Override
  protected boolean isMetadataCorrect(Metadata oldMd, String path)
      throws IOException
  {
    Metadata md = createMetadata(path);

    List<Plugin> oldPlugins = oldMd.getPlugins();

    if (oldPlugins == null) {
      return false;
    }

    List<Plugin> plugins = md.getPlugins();

    if (oldPlugins.size() != plugins.size()) {
      return false;
    }

    for (int i = 0; i < oldPlugins.size(); i++) {
      Plugin oldPlugin = oldPlugins.get(i);

      if (!containPlugin(plugins, oldPlugin)) {
        return false;
      }
    }

    return true;

  }

  private boolean containPlugin(List<Plugin> plugins, Plugin expect) {
    for (Plugin plugin : plugins) {
      if (isPluginEquals(plugin, expect)) {
        return true;
      }
    }

    return false;
  }

}
