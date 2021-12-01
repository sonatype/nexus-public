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
package org.sonatype.nexus.testcontainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Work around to support testcontainers 1.14 and 1.16
 */
public class TestContainersWorkAround
{
  /**
   * Invokes {@code DockerImageName.asCompatibleSubstituteFor} if the API is available
   */
  public static DockerImageName imageName(final String image, final String compatibleWith) {
    DockerImageName imageName = new DockerImageName(image);
    try {
      Method method = DockerImageName.class.getMethod("asCompatibleSubstituteFor", String.class);
      return (DockerImageName) method.invoke(imageName, compatibleWith);
    }
    catch (Exception e) {
      LoggerFactory.getLogger(TestContainersWorkAround.class).debug("Probably we're using 1.14", e);
    }
    return imageName;
  }

  /**
   * Invokes {@code DockerImageName.asCompatibleSubstituteFor} if the API is available
   */
  public static PostgreSQLContainer<?> postgresContainer(final String imageName) {
    try {
      Constructor<PostgreSQLContainer> constructor = PostgreSQLContainer.class.getConstructor(DockerImageName.class);
      DockerImageName image = imageName(imageName, "postgres");
      return constructor.newInstance(image);
    }
    catch (Exception e) {
      LoggerFactory.getLogger(TestContainersWorkAround.class).debug("Probably we're using 1.14", e);
    }
    return new PostgreSQLContainer(imageName);
  }
}
