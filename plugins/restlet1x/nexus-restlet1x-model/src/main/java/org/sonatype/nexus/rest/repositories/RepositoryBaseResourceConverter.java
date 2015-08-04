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
package org.sonatype.nexus.rest.repositories;

import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.plexus.rest.xstream.LookAheadStreamReader;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * XStream converter that helps XStream to convert incoming JSON data properly. It handles RepositoryBaseResource
 * classes only.
 *
 * @author cstamas
 */
public class RepositoryBaseResourceConverter
    extends AbstractReflectionConverter
{
  /**
   * Repo type hosted.
   */
  public static final String REPO_TYPE_HOSTED = "hosted";

  /**
   * Repo type proxied.
   */
  public static final String REPO_TYPE_PROXIED = "proxy";

  /**
   * Repo type virtual (shadow in nexus).
   */
  public static final String REPO_TYPE_VIRTUAL = "virtual";

  /**
   * Repo type group.
   */
  public static final String REPO_TYPE_GROUP = "group";

  public RepositoryBaseResourceConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
    super(mapper, reflectionProvider);
  }

  public boolean canConvert(Class type) {
    return RepositoryBaseResource.class.equals(type);
  }

  protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
    if (LookAheadStreamReader.class.isAssignableFrom(reader.getClass())
        || LookAheadStreamReader.class.isAssignableFrom(reader.underlyingReader().getClass())) {
      String repoType = null;

      if (LookAheadStreamReader.class.isAssignableFrom(reader.getClass())) {
        repoType = ((LookAheadStreamReader) reader).getFieldValue("repoType");
      }
      else {
        repoType = ((LookAheadStreamReader) reader.underlyingReader()).getFieldValue("repoType");
      }

      if (REPO_TYPE_VIRTUAL.equals(repoType)) {
        return new RepositoryShadowResource();
      }
      else if (REPO_TYPE_PROXIED.equals(repoType)) {
        return new RepositoryProxyResource();
      }
      else if (REPO_TYPE_HOSTED.equals(repoType)) {
        return new RepositoryResource();
      }
      else if (REPO_TYPE_GROUP.equals(repoType)) {
        return new RepositoryGroupResource();
      }
      else {
        return super.instantiateNewInstance(reader, context);
      }
    }
    else {
      return super.instantiateNewInstance(reader, context);
    }
  }
}
