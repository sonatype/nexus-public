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
package org.sonatype.nexus.testsuite.config.nexus2490;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.plugins.lvo.api.dto.LvoConfigDTO;
import org.sonatype.nexus.plugins.lvo.api.dto.LvoConfigRequest;
import org.sonatype.nexus.plugins.lvo.api.dto.LvoConfigResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;

public class Nexus2490LvoConfigCheckIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void testConfiguration()
      throws Exception
  {
    updateConfig(true);
    Assert.assertTrue(isEnabled());
    updateConfig(false);
    Assert.assertFalse(isEnabled());
    updateConfig(true);
    Assert.assertTrue(isEnabled());
    updateConfig(false);
    Assert.assertFalse(isEnabled());
  }

  private void updateConfig(boolean enabled)
      throws Exception
  {
    XStreamRepresentation representation =
        new XStreamRepresentation(getJsonXStream(), "", MediaType.APPLICATION_JSON);

    LvoConfigRequest request = new LvoConfigRequest();

    LvoConfigDTO dto = new LvoConfigDTO();
    dto.setEnabled(enabled);
    request.setData(dto);

    representation.setPayload(request);

    Assert.assertTrue(
        RequestFacade.sendMessage("service/local/lvo_config", Method.PUT, representation).getStatus().isSuccess());
  }

  private boolean isEnabled()
      throws Exception
  {
    String responseText = RequestFacade.doGetForText("service/local/lvo_config");

    XStreamRepresentation representation =
        new XStreamRepresentation(getXMLXStream(), responseText, MediaType.APPLICATION_XML);

    LvoConfigResponse resp = (LvoConfigResponse) representation.getPayload(new LvoConfigResponse());

    return resp.getData().isEnabled();
  }
}
