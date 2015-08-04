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
package org.sonatype.nexus.testsuite.support;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.sonatype.sisu.goodies.marshal.internal.jackson2.JacksonMarshaller;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * Utility class for loading Nexus integration tests parameters.
 *
 * @since 2.2
 */
public abstract class ParametersLoaders
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ParametersLoaders.class);

  /**
   * Load test parameters from specified file. The file should contain a serialized Object[][] in json format.  Fails
   * if such a file cannot be found.
   *
   * @param parametersFile file containing parameters
   * @return test parameters loader. Never null.
   */
  public static Loader testParameters(final File parametersFile) {
    return testParameters(parametersFile, true);
  }

  /**
   * Load test parameters from specified file. The file should contain a serialized Object[][] in json format.
   *
   * @param parametersFile file containing parameters
   * @param failIfNotFound whether it should fail if parameters file cannot be found or return an empty list
   * @return test parameters loader. Never null.
   */
  public static Loader testParameters(final File parametersFile, final boolean failIfNotFound) {
    return new Loader()
    {

      @Override
      public Collection<Object[]> load() {
        if (!checkNotNull(parametersFile).exists()) {
          if (failIfNotFound) {
            throw new RuntimeException("Cannot find file '" + parametersFile.getAbsolutePath() + "'");
          }
          LOGGER.info(
              "File '" + parametersFile.getAbsolutePath() + "' cannot be found and will be ignored"
          );
          return Lists.newArrayList();
        }
        LOGGER.info("Loading test parameters from {}", parametersFile.getAbsolutePath());
        try {
          final Object[][] parametersSets = new JacksonMarshaller().unmarshal(
              readFileToString(checkNotNull(parametersFile)), Object[][].class
          );
          if (parametersSets == null) {
            return null;
          }
          return Arrays.asList(parametersSets);
        }
        catch (final Exception e) {
          throw Throwables.propagate(e);
        }
      }

    };
  }

  /**
   * Load test specific parameters by looking up a file named "<test class name>-parameters.json" from classpath.
   * Fails if such a file cannot be found.
   *
   * @param testClass test class
   * @return test parameters loader. Never null.
   * @see {@link #testParameters}
   */
  public static Loader testParameters(final Class testClass) {
    return testParameters(testClass, true);
  }

  /**
   * Load test specific parameters by looking up a file named "<test class name>-parameters.json" from classpath.
   *
   * @param testClass      test class
   * @param failIfNotFound whether it should fail if parameters file cannot be found or return an empty list
   * @return test parameters loader. Never null.
   * @see {@link #testParameters}
   */
  public static Loader testParameters(final Class testClass, final boolean failIfNotFound) {
    return new Loader()
    {
      @Override
      public Collection<Object[]> load() {
        final String parametersFileName = checkNotNull(testClass).getSimpleName() + "-parameters.json";
        final URL resource = testClass.getClassLoader().getResource(parametersFileName);
        if (resource == null) {
          if (failIfNotFound) {
            throw new RuntimeException(
                "Cannot find a file named '" + parametersFileName + "' in classpath"
            );
          }
          LOGGER.info(
              "File named '" + parametersFileName + "' cannot be found in classpath and will be ignored"
          );
          return Lists.newArrayList();
        }
        return testParameters(new File(resource.getFile())).load();
      }
    };
  }

  /**
   * Load test parameters by looking up an "parameters.json" file in classpath. Fails if such a file cannot be found.
   *
   * @return test parameters loader. Never null.
   * @see {@link #testParameters}
   */
  public static Loader defaultTestParameters() {
    return defaultTestParameters(true);
  }

  /**
   * Load test parameters by looking up an "parameters.json" file in classpath.
   *
   * @param failIfNotFound whether it should fail if parameters file cannot be found or return an empty list
   * @return test parameters loader. Never null.
   * @see {@link #testParameters}
   */
  public static Loader defaultTestParameters(final boolean failIfNotFound) {
    return new Loader()
    {

      @Override
      public Collection<Object[]> load() {
        final URL resource = ParametersLoaders.class.getClassLoader().getResource("parameters.json");
        if (resource == null) {
          if (failIfNotFound) {
            throw new RuntimeException("Cannot find a file named 'parameters.json' in classpath");
          }
          LOGGER.info("File named 'parameters.json' cannot be found in classpath and will be ignored");
          return Lists.newArrayList();
        }
        return testParameters(new File(resource.getFile())).load();
      }

    };
  }

  /**
   * Load test parameters by looking up file specified via a system property named "NexusItSupport.parameters" (if
   * defined).  Fails if such a file cannot be found.
   *
   * @return test parameters  loader. Never null.
   * @see {@link #testParameters}
   */
  public static Loader systemTestParameters() {
    return systemTestParameters(true);
  }

  /**
   * Load test parameters by looking up file specified via a system property named "NexusItSupport.parameters" (if
   * defined).
   *
   * @param failIfNotFound whether it should fail if parameters file cannot be found or return an empty list
   * @return test parameters loader. Never null.
   * @see {@link #testParameters}
   */
  public static Loader systemTestParameters(final boolean failIfNotFound) {
    return new Loader()
    {
      @Override
      public Collection<Object[]> load() {
        final String sysPropsParameters = System.getProperty("it.parameters");
        if (sysPropsParameters == null) {
          return Lists.newArrayList();
        }
        return testParameters(new File(sysPropsParameters), failIfNotFound).load();
      }

    };
  }

  /**
   * Load test parameters from an array.
   *
   * @param parameters parameters arrays (sets)
   * @return test parameters loader. Never null.
   */
  public static Loader testParameters(final String[]... parameters) {
    return new Loader()
    {
      @Override
      public Collection<Object[]> load() {
        return Arrays.<Object[]>asList(parameters);
      }

    };
  }

  /**
   * Use system property as test parameter.
   *
   * @param propertyName name of system property
   * @return test parameters loader. Never null.
   */
  public static Loader systemProperty(final String propertyName) {
    return new Loader()
    {
      @Override
      public Collection<Object[]> load() {
        String property = System.getProperty(propertyName);
        if (property != null) {
          return Collections.singleton(new Object[]{property});
        }
        return null;
      }

    };
  }

  /**
   * Iterates provided loaders and return the first non empty list of parameters.
   *
   * @param loaders to be iterated
   * @return first available parameters loader. Never null.
   */
  public static Loader firstAvailableTestParameters(final Loader... loaders) {
    return new Loader()
    {
      @Override
      public Collection<Object[]> load() {
        for (final Loader loader : checkNotNull(loaders)) {
          final Collection<Object[]> loaded = checkNotNull(loader).load();
          if (loaded != null && !loaded.isEmpty()) {
            return loaded;
          }
        }
        throw new RuntimeException("No parameters found");
      }

    };
  }

  /**
   * Iterates provided loaders and returns a aggregation of all parameters.
   *
   * @param loaders to be iterated
   * @return aggregation of all parameters loader. Never null.
   */
  public static Loader allAvailableTestParameters(final Loader... loaders) {
    return new Loader()
    {
      @Override
      public Collection<Object[]> load() {
        final Collection<Object[]> parameters = Lists.newArrayList();
        for (final Loader loader : checkNotNull(loaders)) {
          final Collection<Object[]> loaded = checkNotNull(loader).load();
          if (loaded != null && !loaded.isEmpty()) {
            parameters.addAll(loaded);
          }
        }
        if (parameters.isEmpty()) {
          throw new RuntimeException("No parameters found");
        }
        return parameters;
      }

    };
  }

  public interface Loader
  {

    Collection<Object[]> load();

  }

}
