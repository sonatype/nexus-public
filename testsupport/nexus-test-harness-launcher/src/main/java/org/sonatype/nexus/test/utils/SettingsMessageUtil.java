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
package org.sonatype.nexus.test.utils;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResourceResponse;
import org.sonatype.nexus.rest.model.SmtpSettingsResource;
import org.sonatype.nexus.rest.model.SmtpSettingsResourceRequest;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.XStream;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

public class SettingsMessageUtil
{
  private static XStream xstream;

  static {
    xstream = XStreamFactory.getXmlXStream();
  }

  public static GlobalConfigurationResource getCurrentSettings()
      throws IOException
  {
    return getData(new GlobalConfigurationResourceResponse());
  }

  public static GlobalConfigurationResource getData(final GlobalConfigurationResourceResponse wrapper)
      throws IOException
  {
    Preconditions.checkNotNull(wrapper);
    String responseText = RequestFacade.doGetForText("service/local/global_settings/current");
    final XStreamRepresentation rep = new XStreamRepresentation(xstream, responseText, MediaType.APPLICATION_XML);
    final GlobalConfigurationResourceResponse configResponse =
        (GlobalConfigurationResourceResponse) rep.getPayload(wrapper);
    return configResponse.getData();
  }

  public static Status save(final GlobalConfigurationResource globalConfig)
      throws IOException
  {
    Preconditions.checkNotNull(globalConfig);
    final GlobalConfigurationResourceResponse configResponse = wrapData(globalConfig);
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(configResponse);
    return RequestFacade.doPutForStatus("service/local/global_settings/current", representation, null);
  }

  public static Status save(final SmtpSettingsResource smtpSettings)
      throws IOException
  {
    SmtpSettingsResourceRequest configResponse = wrapData(smtpSettings);
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(configResponse);
    return RequestFacade.doPutForStatus("service/local/check_smtp_settings", representation, null);
  }

  /**
   * Wrap a {@link GlobalConfigurationResource} in a {@link GlobalConfigurationResourceResponse} and return it.
   *
   * @param resource the resource to wrap
   * @return a wrapper containing the resource
   */
  public static GlobalConfigurationResourceResponse wrapData(final GlobalConfigurationResource resource) {
    Preconditions.checkNotNull(resource);
    final GlobalConfigurationResourceResponse wrapper = new GlobalConfigurationResourceResponse();
    wrapper.setData(resource);
    return wrapper;
  }

  public static SmtpSettingsResourceRequest wrapData(final SmtpSettingsResource resource) {
    Preconditions.checkNotNull(resource);
    final SmtpSettingsResourceRequest wrapper = new SmtpSettingsResourceRequest();
    wrapper.setData(resource);
    return wrapper;
  }

}
