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
package org.sonatype.nexus.quartz.internal.orient;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link ObjectMapper} which only pays attention to non-null fields.
 *
 * @since 3.0
 */
public class FieldObjectMapper
  extends ObjectMapper
{
  public FieldObjectMapper() {
    // only pay attention to fields
    setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
    setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
    setVisibility(PropertyAccessor.CREATOR, Visibility.NONE);
    setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);

    // ignore unknown fields when reading
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ignore null fields when writing
    setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }
}
