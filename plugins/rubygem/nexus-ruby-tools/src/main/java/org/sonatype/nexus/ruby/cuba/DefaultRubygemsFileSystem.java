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
package org.sonatype.nexus.ruby.cuba;

import org.sonatype.nexus.ruby.RubygemsFileFactory;
import org.sonatype.nexus.ruby.cuba.api.ApiCuba;
import org.sonatype.nexus.ruby.cuba.api.ApiV1Cuba;
import org.sonatype.nexus.ruby.cuba.api.ApiV1DependenciesCuba;
import org.sonatype.nexus.ruby.cuba.gems.GemsCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenPrereleasesCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenPrereleasesRubygemsCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenReleasesCuba;
import org.sonatype.nexus.ruby.cuba.maven.MavenReleasesRubygemsCuba;
import org.sonatype.nexus.ruby.cuba.quick.QuickCuba;
import org.sonatype.nexus.ruby.cuba.quick.QuickMarshalCuba;
import org.sonatype.nexus.ruby.layout.DefaultLayout;
import org.sonatype.nexus.ruby.layout.Layout;

public class DefaultRubygemsFileSystem
    extends RubygemsFileSystem
{
  public DefaultRubygemsFileSystem(RubygemsFileFactory fileLayout,
                                   Layout getLayout,
                                   Layout postLayout,
                                   Layout deleteLayout)
  {
    super(fileLayout, getLayout, postLayout, deleteLayout,
        // TODO move to javax.inject
        new RootCuba(new ApiCuba(new ApiV1Cuba(new ApiV1DependenciesCuba()),
            new QuickCuba(new QuickMarshalCuba()), new GemsCuba()),
            new QuickCuba(new QuickMarshalCuba()),
            new GemsCuba(),
            new MavenCuba(new MavenReleasesCuba(new MavenReleasesRubygemsCuba()),
                new MavenPrereleasesCuba(new MavenPrereleasesRubygemsCuba()))));
  }

  public DefaultRubygemsFileSystem(Layout getLayout, Layout postLayout, Layout deleteLayout) {
    this(new DefaultLayout(), getLayout, postLayout, deleteLayout);
  }

  public DefaultRubygemsFileSystem() {
    this(new DefaultLayout(), null, null, null);
  }
}