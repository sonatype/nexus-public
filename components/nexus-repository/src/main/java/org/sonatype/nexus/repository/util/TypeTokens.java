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
package org.sonatype.nexus.repository.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.hash.HashCode;
import com.google.common.reflect.TypeToken;

/**
 * Collection of {@link TypeToken}.
 *
 * @since 3.0
 */
public class TypeTokens
{
  private TypeTokens() {}

  public static final TypeToken<Collection<String>> COLLECTION_STRING = new TypeToken<Collection<String>>() {};

  public static final TypeToken<List<String>> LIST_STRING = new TypeToken<List<String>>() {};

  public static final TypeToken<Map<HashAlgorithm, HashCode>> HASH_CODES_MAP = new TypeToken<Map<HashAlgorithm, HashCode>>() {};
}
