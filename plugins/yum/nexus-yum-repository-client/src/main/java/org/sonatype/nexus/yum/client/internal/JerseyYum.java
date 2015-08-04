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
package org.sonatype.nexus.yum.client.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.yum.client.Yum;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Jersey based {@link Yum}.
 *
 * @since yum 3.0
 */
public class JerseyYum
    extends SubsystemSupport<JerseyNexusClient>
    implements Yum
{

  public JerseyYum(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public String getAlias(String repositoryId, String alias) {
    try {
      return getNexusClient()
          .serviceResource(getUrlPath(repositoryId, alias))
          .get(String.class);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public void createOrUpdateAlias(String repositoryId, String alias, String version) {
    try {
      getNexusClient()
          .serviceResource(getUrlPath(repositoryId, alias))
          .type(TEXT_PLAIN)
          .post(String.class, version);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  private String getUrlPath(String repositoryId, String alias) {
    return format("yum/alias/%s/%s", encodeUtf8(repositoryId), encodeUtf8(alias));
  }

  private static String encodeUtf8(String string) {
    try {
      return URLEncoder.encode(string, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Could not utf8-encode string : " + string, e);
    }
  }

}
