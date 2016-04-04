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
package org.sonatype.nexus.siesta;

import org.sonatype.nexus.siesta.internal.ValidationErrorsExceptionMapper;
import org.sonatype.nexus.siesta.internal.WebappExceptionMapper;
import org.sonatype.nexus.validation.ValidationModule;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

/**
 * Test module.
 */
public class TestModule
    extends AbstractModule
{
  public static final String MOUNT_POINT = "/siesta";

  @Override
  protected void configure() {
    install(new ResteasyModule());
    install(new ValidationModule());

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        serve(MOUNT_POINT + "/*").with(SiestaServlet.class, ImmutableMap.of(
            "resteasy.servlet.mapping.prefix", MOUNT_POINT
        ));
      }
    });

    // register exception mappers required by tests
    register(ValidationErrorsExceptionMapper.class);
    register(WebappExceptionMapper.class);

    // register test resources
    register(EchoResource.class);
    register(ErrorsResource.class);
    register(UserResource.class);
    register(ValidationErrorsResource.class);
  }

  private void register(final Class<?> impl) {
    // this makes the implementation visible as any type in its hierarchy
    bind(Object.class).annotatedWith(Names.named(impl.getName())).to(impl);
  }
}