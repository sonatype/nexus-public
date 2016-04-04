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
package org.sonatype.nexus.siesta.internal.resteasy;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.jboss.resteasy.client.exception.mapper.ClientExceptionMapper;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.StringConverter;
import org.jboss.resteasy.spi.StringParameterUnmarshaller;
import org.jboss.resteasy.util.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sisu {@link ResteasyProviderFactory} that supports removal of providers.
 *
 * @since 3.0
 */
public class SisuResteasyProviderFactory
    extends ResteasyProviderFactory
{
  private static final Logger log = LoggerFactory.getLogger(SisuResteasyProviderFactory.class);

  /**
   * Unregisters a @Provider type from this factory.
   */
  public void removeRegistrations(final Class<?> type) {
    checkNotNull(type);

    log.debug("Removing registrations for: {}", type.getName());

    classContracts.remove(type);

    removeInstancesOf(type, providerInstances);
    providerClasses.remove(type);

    if (ExceptionMapper.class.isAssignableFrom(type)) {
      removeInstancesOf(type, exceptionMappers.values());
    }
    else if (MessageBodyReader.class.isAssignableFrom(type)) {
      clearInstancesOf(type, clientMessageBodyReaders, DUMMY_READER);
      clearInstancesOf(type, serverMessageBodyReaders, DUMMY_READER);
    }
    else if (MessageBodyWriter.class.isAssignableFrom(type)) {
      clearInstancesOf(type, clientMessageBodyWriters, DUMMY_WRITER);
      clearInstancesOf(type, serverMessageBodyWriters, DUMMY_WRITER);
    }
    else if (ContextResolver.class.isAssignableFrom(type)) {
      Type[] args = Types.getActualTypeArgumentsOfAnInterface(type, ContextResolver.class);
      contextResolvers.remove(Types.getRawType(args[0]));
    }
    else if (Feature.class.isAssignableFrom(type)) {
      removeInstancesOf(type, featureInstances);
      removeInstancesOf(type, enabledFeatures);
      featureClasses.remove(type);
    }
    else if (DynamicFeature.class.isAssignableFrom(type)) {
      removeInstancesOf(type, clientDynamicFeatures);
      removeInstancesOf(type, serverDynamicFeatures);
    }
    else if (ParamConverterProvider.class.isAssignableFrom(type)) {
      removeInstancesOf(type, paramConverterProviders);
    }
    else if (ClientExceptionMapper.class.isAssignableFrom(type)) {
      removeInstancesOf(type, clientExceptionMappers.values());
    }
    else if (StringConverter.class.isAssignableFrom(type)) {
      removeInstancesOf(type, stringConverters.values());
    }
    else if (StringParameterUnmarshaller.class.isAssignableFrom(type)) {
      stringParameterUnmarshallers.values().remove(type);
    }
    else {
      log.warn("Unable to remove registrations for: {}", type.getName());
    }
  }

  /**
   * Remove any instances of the given type from the collection.
   */
  private static void removeInstancesOf(final Class<?> type, final Collection<?> collection) {
    for (Object o : collection) {
      if (type.isInstance(o)) {
        collection.remove(o); // ResteasyProviderFactory's collections are all concurrent, so this is safe
      }
    }
  }

  /**
   * Clear any instances of the given type from the {@link MediaTypeMap} by replacing them with the placeholder.
   *
   * Unfortunately removing the entries is tricky as they're scattered across multiple read-only nested maps.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static <T> void clearInstancesOf(final Class type,
                                           final MediaTypeMap<SortedKey<T>> mediaTypeMap,
                                           final T placeholder)
  {
    for (SortedKey key : mediaTypeMap.getPossible(MediaType.WILDCARD_TYPE)) {
      if (type.isInstance(key.obj)) {
        key.readerClass = Void.class;
        key.template = Void.class;
        key.obj = placeholder;
      }
    }
    mediaTypeMap.getClassCache().clear();
  }

  /**
   * Dummy {@link MessageBodyReader} that never matches anything.
   */
  private static final MessageBodyReader<?> DUMMY_READER = new MessageBodyReader<Object>()
  {
    public boolean isReadable(final Class<?> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType)
    {
      return false;
    }

    public Object readFrom(final Class<Object> type,
                           final Type genericType,
                           final Annotation[] annotations,
                           final MediaType mediaType,
                           final MultivaluedMap<String, String> httpHeaders,
                           final InputStream entityStream)
    {
      return null;
    }
  };

  /**
   * Dummy {@link MessageBodyWriter} that never matches anything.
   */
  private static final MessageBodyWriter<?> DUMMY_WRITER = new MessageBodyWriter<Object>()
  {
    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType)
    {
      return false;
    }

    public void writeTo(final Object t,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream)
    {
      // no-op
    }

    public long getSize(final Object t,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType)
    {
      return 0;
    }
  };
}
