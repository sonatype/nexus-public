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
package org.sonatype.nexus.repository.maven.internal;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeRule;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.view.ContentTypes;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven 2 specific {@link MimeRulesSource} that specifies known and format specific file MIME types.
 *
 * @since 3.0
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenMimeRulesSource
    extends ComponentSupport
    implements MimeRulesSource
{
  /**
   * Maven POMs.
   */
  public static final String POM_TYPE = ContentTypes.APPLICATION_XML;

  /**
   * Maven POMs.
   */
  private static final MimeRule POM_RULE = new MimeRule(true, POM_TYPE);

  /**
   * SHA1 and MD5 hashes.
   */
  public static final String HASH_TYPE = ContentTypes.TEXT_PLAIN;

  /**
   * SHA1 and MD5 hashes.
   */
  private static final MimeRule HASH_RULE = new MimeRule(true, HASH_TYPE);

  /**
   * Maven Repository Metadata.
   */
  public static final String METADATA_TYPE = ContentTypes.APPLICATION_XML;

  /**
   * Maven Repository Metadata.
   */
  private static final MimeRule METADATA_RULE = new MimeRule(true, METADATA_TYPE);

  /**
   * Maven PGP signature.
   */
  public static final String SIGNATURE_TYPE = "application/pgp-signature";

  /**
   * Maven PGP signature.
   */
  private static final MimeRule SIGNATURE_RULE = new MimeRule(true, SIGNATURE_TYPE, ContentTypes.TEXT_PLAIN);

  private final Maven2MavenPathParser mavenPathParser;

  @Inject
  public MavenMimeRulesSource(final Maven2MavenPathParser mavenPathParser) {
    this.mavenPathParser = checkNotNull(mavenPathParser);
  }

  @Nullable
  @Override
  public MimeRule getRuleForName(final String name) {
    MavenPath mavenPath = mavenPathParser.parsePath(name);
    if (mavenPath.isPom()) {
      return POM_RULE;
    }
    else if (mavenPath.isHash()) {
      return HASH_RULE;
    }
    else if (mavenPath.isSignature()) {
      return SIGNATURE_RULE;
    }
    else if (Constants.METADATA_FILENAME.equals(mavenPath.getFileName())) {
      return METADATA_RULE;
    }
    // otherwise no format-specific rule, use common rules
    return null;
  }
}
