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
package org.sonatype.nexus.repository.metadata;

import java.io.IOException;

import org.sonatype.nexus.repository.metadata.model.OrderedRepositoryMirrorsMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.validation.RepositoryMetadataValidator;

/**
 * The repository metadata handler interface. Defines the basic operations for retrieving and storing the metadata, and
 * retrieving the dynamic list of mirros.
 *
 * @author cstamas
 */
public interface RepositoryMetadataHandler
{
  /**
   * Fetches the metadata. Returns null if metadata is not found. In case of transport or other IO problem,
   * IOException is raised.
   *
   * @return the metadata or null if not found.
   */
  RepositoryMetadata readRepositoryMetadata(RawTransport transport)
      throws MetadataHandlerException,
             IOException;

  /**
   * Fetches the metadata. Returns null if metadata is not found. In case of transport or other IO problem,
   * IOException is raised.
   *
   * @return the metadata or null if not found.
   */
  RepositoryMetadata readRepositoryMetadata(RawTransport transport, RepositoryMetadataValidator validator)
      throws MetadataHandlerException,
             IOException;

  /**
   * Stores the metadata in a writer. In case of transport or other IO problem, IOException is raised. Will use the
   * default validator.
   */
  void writeRepositoryMetadata(RepositoryMetadata metadata, RawTransport transport)
      throws MetadataHandlerException,
             IOException;

  /**
   * Stores the metadata in a writer. In case of transport or other IO problem, IOException is raised.
   */
  void writeRepositoryMetadata(RepositoryMetadata metadata, RawTransport transport,
                               RepositoryMetadataValidator validator)
      throws MetadataHandlerException,
             IOException;

  /**
   * Returns the ordered list of mirrors for given metadata. What (or from where) is returned depends on metadata: if
   * mirrorListSource field is present, it will try to fetch it from there. If not, it will fallback to "local" GeoIP
   * matching, if possible. As last resort, it will return the mirrors field of metadata in unmodified form.
   * <p>
   * The "strategy" fields tells how the result should be interpreted:
   * <ul>
   * <li>SERVER - the list should be consumed as is, since mirror service already formed the "best" order for us. The
   * mirror server will be contacted only when the "mirrorListSource" field is given in metadata.</li>
   * <li>CLIENT_AUTO - if "SERVER" strategy is failed or is not possible, will try (determined at runtime) to apply
   * client side GeoIP matching and ordering to the list. The list should be consumed as is, since GeoIP service
   * already formed the "best" order for us.</li>
   * <li>CLIENT_MANUAL - the list is 1:1 copy of the "static" mirror list without any reordering. In this scenario,
   * the client of this API would have to offer the list to the user over some sort of UI for further
   * introspection.</li>
   * </ul>
   */
  OrderedRepositoryMirrorsMetadata fetchOrderedMirrorMetadata(RepositoryMetadata metadata, RawTransport transport)
      throws MetadataHandlerException,
             IOException;
}
