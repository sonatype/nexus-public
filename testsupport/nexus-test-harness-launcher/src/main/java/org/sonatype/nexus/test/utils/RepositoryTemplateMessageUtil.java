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
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.not;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.inError;

public class RepositoryTemplateMessageUtil
{
  private static final Logger LOG = LoggerFactory.getLogger(RepositoryTemplateMessageUtil.class);

  public static final String TEMPLATE_PROXY_SNAPSHOT = "default_proxy_snapshot";

  public static final String TEMPLATE_PROXY_RELEASE = "default_proxy_release";

  public RepositoryBaseResource getTemplate(String id)
      throws IOException
  {
    String responseText = RequestFacade.doGetForText("service/local/templates/repositories/" + id
        , not(inError()));

    LOG.debug("responseText: \n" + responseText);

    XStreamRepresentation representation = new XStreamRepresentation(
        XStreamFactory.getXmlXStream(),
        responseText,
        MediaType.APPLICATION_XML);

    RepositoryResourceResponse resourceResponse = (RepositoryResourceResponse) representation
        .getPayload(new RepositoryResourceResponse());

    return resourceResponse.getData();
  }
}
