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
package org.sonatype.nexus.common.cooperation2.internal.orient;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.internal.DisabledCooperation2;
import org.sonatype.nexus.common.cooperation2.internal.MutableConfigSupport;
import org.sonatype.nexus.common.io.CooperationFactory;

/**
 * @since 3.41
 */
@Named
@Singleton
public class OrientCooperation2Factory
    extends ComponentSupport
    implements Cooperation2Factory
{
  private CooperationFactory cooperationFactory;

  @Inject
  public OrientCooperation2Factory(final CooperationFactory cooperationFactory) {
    this.cooperationFactory = cooperationFactory;
  }

  @Override
  public Builder configure() {
    return new DefaultCooperation2Builder();
  }

  protected class DefaultCooperation2Builder
      extends MutableConfigSupport
  {
    @Override
    public Cooperation2 build(final String id) {
      if (!enabled) {
        log.debug("Disabled cooperation: {}", id);
        return new DisabledCooperation2(id);
      }
      return new OrientCooperation2(cooperationFactory.configure()
          .majorTimeout(majorTimeout())
          .minorTimeout(minorTimeout())
          .threadsPerKey(threadsPerKey)
          .build(id));
    }

    @Override
    public Cooperation2 build(final Class<?> id, final String... keys) {
      if (!enabled) {
        log.debug("Disabled cooperation: {}", id);
        return new DisabledCooperation2(stripGuice(id, keys));
      }
      return new OrientCooperation2(cooperationFactory.configure()
          .majorTimeout(majorTimeout())
          .minorTimeout(minorTimeout())
          .threadsPerKey(threadsPerKey)
          .build(stripGuice(id, keys)));
    }
  }

  /*
   * When classes are enhanced by Guice AOP they can have random strings and we need them to be consistent
   */
  protected static String stripGuice(final Class<?> clazz, final String... keys) {
    String simpleName = clazz.getSimpleName();
    // this is the normal case due to method interceptors
    if (simpleName.contains("EnhancerByGuice") && clazz.getSuperclass() != null) {
      return stripGuice(clazz.getSuperclass());
    }

    return Arrays.asList(keys).stream().collect(Collectors.joining("-", simpleName + '-', ""));
  }
}
