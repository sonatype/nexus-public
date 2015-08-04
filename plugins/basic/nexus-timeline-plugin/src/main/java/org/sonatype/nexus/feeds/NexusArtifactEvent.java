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
package org.sonatype.nexus.feeds;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.feeds.record.NexusItemInfo;

/**
 * A class thet encapsulates a Nexus artifact event: caching, deploying, deleting or retrieving of it.
 *
 * @author cstamas
 */
public class NexusArtifactEvent
    extends AbstractEvent
{
  public static final String ACTION_CACHED = "cached";

  public static final String ACTION_DEPLOYED = "deployed";

  public static final String ACTION_DELETED = "deleted";

  public static final String ACTION_RETRIEVED = "retrieved";

  public static final String ACTION_BROKEN = "broken";

  public static final String ACTION_BROKEN_WRONG_REMOTE_CHECKSUM = "brokenWRC";

  public static final String ACTION_BROKEN_INVALID_CONTENT = "brokenIC";

  /**
   * The artifactInfo about artifact.
   */
  private final NexusItemInfo nexusItemInfo;

  /**
   * The attributes of the item in question (if any or available).
   */
  private final Map<String, String> itemAttributes;

  public NexusArtifactEvent(final Date eventDate, final String action, final String message,
                            final NexusItemInfo nexusItemInfo)
  {
    super(eventDate, action, message);

    this.nexusItemInfo = nexusItemInfo;

    this.itemAttributes = new HashMap<String, String>();
  }

  public NexusItemInfo getNexusItemInfo() {
    return nexusItemInfo;
  }

  public Map<String, String> getItemAttributes() {
    return itemAttributes;
  }

  public void addItemAttributes(Map<String, String> atr) {
    getItemAttributes().putAll(atr);
  }
}
