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

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.plugins.plugin.console.api.dto.PluginInfoDTO;
import org.sonatype.nexus.plugins.plugin.console.api.dto.PluginInfoListResponseDTO;
import org.sonatype.nexus.rest.model.AliasingListConverter;
import org.sonatype.nexus.test.utils.XStreamConfigurator;
import org.sonatype.nexus.test.utils.plugin.XStreamFactory;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConsoleMessageUtil
{
  private static final String PLUGIN_INFOS_URL = "service/local/plugin_console/plugin_infos";

  private static XStream xmlXstream;

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginConsoleMessageUtil.class);

  static {
    xmlXstream = XStreamFactory.getXmlXStream(new XStreamConfigurator()
    {
      public void configure(XStream xstream) {
        xstream.processAnnotations(PluginInfoDTO.class);
        xstream.processAnnotations(PluginInfoListResponseDTO.class);

        xstream.registerLocalConverter(PluginInfoListResponseDTO.class, "data", new AliasingListConverter(
            PluginInfoDTO.class, "pluginInfo"));
      }
    });
  }

  public List<PluginInfoDTO> listPluginInfos()
      throws IOException
  {
    String serviceURI = PLUGIN_INFOS_URL;

    LOGGER.info("HTTP GET: " + serviceURI);

    final Response response = RequestFacade.sendMessage(serviceURI, Method.GET);
    try {
      if (response.getStatus().isSuccess()) {
        String responseText = response.getEntity().getText();

        LOGGER.debug("Response Text: \n" + responseText);

        XStreamRepresentation representation =
            new XStreamRepresentation(xmlXstream, responseText, MediaType.APPLICATION_XML);

        PluginInfoListResponseDTO responseDTO =
            (PluginInfoListResponseDTO) representation.getPayload(new PluginInfoListResponseDTO());

        return responseDTO.getData();
      }
      else {
        LOGGER.warn("HTTP Error: '" + response.getStatus().getCode() + "'");

        LOGGER.warn(response.getEntity().getText());

        return null;
      }
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

}
