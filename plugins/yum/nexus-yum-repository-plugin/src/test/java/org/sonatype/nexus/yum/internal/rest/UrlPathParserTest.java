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
package org.sonatype.nexus.yum.internal.rest;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlPathParserTest
    extends TestSupport
{

  private static final String DOMAIN = "http://localhost:8081";

  private static final String BASE_PATH = "/nexus/service/local";

  private static final String REPO_PATH = BASE_PATH + "/yum/snapshots/76.0.1";

  private static final String BASE_URL = DOMAIN + BASE_PATH;

  private static final String REPO_URL = DOMAIN + REPO_PATH;

  private static final String REPOMD = "repodata/repomd.xml";

  @Test(expected = ResourceException.class)
  public void shouldThrowExceptionIfPrefixIsNotInUri()
      throws Exception
  {
    createInterpretation("yum", 2, BASE_URL + "/bla/bla/blup/repod.xml");
  }

  @Test
  public void shouldFindPrefix()
      throws Exception
  {
    UrlPathInterpretation interpretation = createInterpretation("yum", 2, REPO_URL + "/" + REPOMD);
    assertEquals(REPO_URL, interpretation.getRepositoryUrl().toString());
    assertEquals(REPOMD, interpretation.getPath());
  }

  @Test
  public void shouldFindIndex()
      throws Exception
  {
    UrlPathInterpretation interpretation = createInterpretation("yum", 2, REPO_URL);
    assertTrue(interpretation.isIndex());
    assertTrue(interpretation.isRedirect());
    assertEquals(REPO_PATH + "/", interpretation.getRedirectUri());
  }

  @Test
  public void shouldFindIndex2()
      throws Exception
  {
    UrlPathInterpretation interpretation = createInterpretation("yum", 2, REPO_URL + "/");
    assertTrue(interpretation.isIndex());
  }

  @Test
  public void shouldFindIndex3()
      throws Exception
  {
    UrlPathInterpretation interpretation = createInterpretation("yum", 2, REPO_URL + "/repodata");
    assertTrue(interpretation.isIndex());
    assertTrue(interpretation.isRedirect());
    assertEquals(REPO_PATH + "/repodata/", interpretation.getRedirectUri());
  }

  @Test
  public void shouldFindIndex4()
      throws Exception
  {
    UrlPathInterpretation interpretation = createInterpretation("yum", 2, REPO_URL + "/repodata/");
    assertTrue(interpretation.isIndex());
  }

  @Test
  public void shouldRetrieveRpmFileInBaseDir()
      throws Exception
  {
    UrlPathInterpretation interpretation =
        createInterpretation("yum-repos", 0, BASE_URL + "/yum-repos/is24-rel-try-next-1.0-1-1.noarch.rpm");
    assertFalse(interpretation.isIndex());
    assertFalse(interpretation.isRedirect());
    assertEquals(interpretation.getPath(), "is24-rel-try-next-1.0-1-1.noarch.rpm");
  }

  private UrlPathInterpretation createInterpretation(String prefix, int segmentsAfterPrefix, String uri)
      throws ResourceException
  {
    return new UrlPathParser(prefix, segmentsAfterPrefix).parse(new Request(Method.GET, uri));
  }

}
