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
package org.sonatype.nexus.plugins.plugin.console.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(value = "pluginInfos")
@XmlRootElement(name = "pluginInfos")
public class PluginInfoListResponseDTO
{
  private List<PluginInfoDTO> data = new ArrayList<PluginInfoDTO>();

  @XmlElementWrapper(name = "data")
  @XmlElement(name = "pluginInfo")
  public List<PluginInfoDTO> getData() {
    return data;
  }

  public void setData(List<PluginInfoDTO> data) {
    this.data = data;
  }

  public void addPluginInfo(PluginInfoDTO pluginInfo) {
    data.add(pluginInfo);
  }
}
