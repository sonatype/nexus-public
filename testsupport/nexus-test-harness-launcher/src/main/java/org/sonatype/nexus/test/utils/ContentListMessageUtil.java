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
import java.util.List;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class ContentListMessageUtil
{

  private XStream xstream;

  private MediaType mediaType;

  public ContentListMessageUtil(XStream xstream, MediaType mediaType) {
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  protected Response getResponse(String repoId, String path, boolean isGroup)
      throws IOException
  {
    String groupOrRepoPart = isGroup ? "repo_groups/" : "repositories/";
    String uriPart = RequestFacade.SERVICE_LOCAL + groupOrRepoPart + repoId + "/content" + path;

    return RequestFacade.sendMessage(uriPart, Method.GET);
  }

  public List<ContentListResource> getContentListResource(String repoId, String path, boolean isGroup)
      throws IOException
  {
    Response response = this.getResponse(repoId, path, isGroup);

    String responeText = response.getEntity().getText();
    Assert.assertTrue(
        "Expected sucess: Status was: " + response.getStatus() + "\nResponse:\n" + responeText,
        response.getStatus().isSuccess());

    XStreamRepresentation representation = new XStreamRepresentation(this.xstream, responeText, this.mediaType);
    ContentListResourceResponse listRepsonse = (ContentListResourceResponse) representation
        .getPayload(new ContentListResourceResponse());

    return listRepsonse.getData();

  }
}
