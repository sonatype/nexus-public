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
package org.sonatype.nexus.datastore.mybatis;

import java.util.function.Supplier;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretDeserializer;
import org.sonatype.nexus.crypto.secrets.SecretSerializer;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import org.apache.ibatis.type.TypeHandler;

/**
 * MyBatis {@link TypeHandler} that sets up an {@link ObjectMapper} with some custom serializers to be able to
 * store/retrieve object attributes with type {@link Secret}
 *
 * @param <T> the java type of the object to be handled
 */
public class AbstractJsonWithSecretsTypeHandler<T>
    extends AbstractJsonTypeHandler<T>
{
  // this guarantees the constructor and buildObjectMapper work on the same mapper, regardless which runs first
  private static final ThreadLocal<ObjectMapper> mapper = ThreadLocal.withInitial(ObjectMapper::new);

  protected AbstractJsonWithSecretsTypeHandler(final SecretsFactory secretsFactory) {
    mapper.get()
        .setAnnotationIntrospector(new OverrideIgnoreTypeIntrospector(
            ImmutableList.of(Secret.class)
        ))
        .registerModule(new SimpleModule()
            .addSerializer(
                Secret.class,
                new SecretSerializer()
            )
            .addDeserializer(
                Secret.class,
                new SecretDeserializer(secretsFactory)
            ));
  }

  @Override
  protected ObjectMapper buildObjectMapper(final Supplier<ObjectMapper> mapperFactory) {
    return mapper.get();
  }
}

