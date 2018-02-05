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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.rest.Resource;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

@Named
@Singleton
@Path("/echo")
public class EchoResource
    implements Resource
{
  @GET
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public List<String> get(@QueryParam("foo") String foo,
                          @QueryParam("bar") Integer bar)
  {
    final List<String> result = Lists.newArrayList();
    if (foo != null) {
      result.add("foo=" + foo);
    }
    if (bar != null) {
      result.add("bar=" + bar);
    }
    return result;
  }

  @GET
  @Path("/multiple")
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public List<String> get(@QueryParam("foo") List<String> foo) {
    return Lists.transform(foo, new Function<String, String>()
    {
      public String apply(final String value) {
        return "foo=" + value;
      }
    });
  }
}
