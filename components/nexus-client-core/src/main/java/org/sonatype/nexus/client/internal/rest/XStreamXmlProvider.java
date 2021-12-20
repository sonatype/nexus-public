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
package org.sonatype.nexus.client.internal.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.client.internal.util.Check;
import org.sonatype.nexus.rest.model.NexusResponse;

import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.security.TypeHierarchyPermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;

/**
 * Jersey provider that uses XStream, as we want to use the same tech (as we have all the config bits already) on
 * client
 * side for marshaling as server side does.
 *
 * @since 2.1
 */
@Provider
public class XStreamXmlProvider
    extends AbstractMessageReaderWriterProvider<Object>
{

  private static final String DEFAULT_ENCODING = "utf-8";

  private final MediaType xstreamMediaType;

  private final XStream xstream;

  public XStreamXmlProvider(final XStream xstream, final MediaType xstreamMediaType) {
    xstream.addPermission(new TypeHierarchyPermission(NexusResponse.class));
    xstream.addPermission(new WildcardTypePermission(new String[]{"com.sonatype.nexus.staging.api.dto.*"}));

    this.xstream = Check.notNull(xstream, XStream.class);
    this.xstreamMediaType = Check.notNull(xstreamMediaType, MediaType.class);
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return xstreamMediaType.isCompatible(mediaType);
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
    return xstreamMediaType.isCompatible(mediaType);
  }

  protected String getCharsetAsString(MediaType m) {
    if (m == null) {
      return DEFAULT_ENCODING;
    }
    String result = m.getParameters().get("charset");
    return (result == null) ? DEFAULT_ENCODING : result;
  }

  @Override
  public Object readFrom(Class<Object> aClass, Type genericType, Annotation[] annotations, MediaType mediaType,
                         MultivaluedMap<String, String> map, InputStream stream)
      throws IOException, WebApplicationException
  {
    String encoding = getCharsetAsString(mediaType);
    return xstream.fromXML(new InputStreamReader(stream, encoding));
  }

  @Override
  public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, Object> map, OutputStream stream)
      throws IOException, WebApplicationException
  {
    String encoding = getCharsetAsString(mediaType);
    xstream.marshal(o, new CompactWriter(new OutputStreamWriter(stream, encoding)));
  }

  @Override
  public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      String encoding = getCharsetAsString(mediaType);
      xstream.marshal(o, new CompactWriter(new OutputStreamWriter(bos, encoding)));
      return bos.size();
    }
    catch (UnsupportedEncodingException e) {
      // huh?
      return -1;
    }
  }
}
