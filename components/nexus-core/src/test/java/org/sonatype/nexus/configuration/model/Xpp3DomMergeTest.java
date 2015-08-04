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
package org.sonatype.nexus.configuration.model;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class Xpp3DomMergeTest
    extends TestSupport
{
  private static final String XML_BASE =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><externalConfiguration></externalConfiguration>";

  @Test
  public void testMergeOfCollection()
      throws Exception
  {
    List<String> empty = Collections.emptyList();

    SimpleXpp3ConfigHolder aHolder = new SimpleXpp3ConfigHolder(XML_BASE);
    SimpleXpp3ConfigHolder bHolder = new SimpleXpp3ConfigHolder(XML_BASE);

    aHolder.setCollection(bHolder.getRootNode(), "memberRepositories", empty);
    aHolder.addToCollection(aHolder.getRootNode(), "memberRepositories", "central-m1", true);
    aHolder.addToCollection(aHolder.getRootNode(), "memberRepositories", "m1h", true);
    aHolder.addToCollection(aHolder.getRootNode(), "memberRepositories", "m1p", true);

    bHolder.setCollection(bHolder.getRootNode(), "memberRepositories", empty);
    bHolder.addToCollection(bHolder.getRootNode(), "memberRepositories", "central-m1", true);
    bHolder.addToCollection(bHolder.getRootNode(), "memberRepositories", "m1h", true);
    bHolder.addToCollection(bHolder.getRootNode(), "memberRepositories", "m1p", true);

    bHolder.removeFromCollection(bHolder.getRootNode(), "memberRepositories", "m1p");

    aHolder.apply(bHolder);

    SimpleXpp3ConfigHolder resultHolder = new SimpleXpp3ConfigHolder(XML_BASE);
    resultHolder.setCollection(resultHolder.getRootNode(), "memberRepositories", empty);
    resultHolder.addToCollection(resultHolder.getRootNode(), "memberRepositories", "central-m1", true);
    resultHolder.addToCollection(resultHolder.getRootNode(), "memberRepositories", "m1h", true);

    assertThat(resultHolder.getRootNode(), equalTo(aHolder.getRootNode()));
  }

  private static class SimpleXpp3ConfigHolder
      extends AbstractXpp3DomExternalConfigurationHolder
  {
    public SimpleXpp3ConfigHolder(String xml)
        throws XmlPullParserException, IOException
    {
      super(Xpp3DomBuilder.build(new StringReader(xml)));
    }

    @Override
    public ValidationResponse doValidateChanges(ApplicationConfiguration applicationConfiguration,
                                                CoreConfiguration owner, Xpp3Dom configuration)
    {
      return new ValidationResponse();
    }
  }
}
