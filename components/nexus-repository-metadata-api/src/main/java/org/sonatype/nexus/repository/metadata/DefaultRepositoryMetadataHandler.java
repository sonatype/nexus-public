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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.metadata.model.OrderedMirrorMetadata;
import org.sonatype.nexus.repository.metadata.model.OrderedRepositoryMirrorsMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMirrorMetadata;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.OrderedRepositoryMirrorsMetadataXpp3Reader;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Reader;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Writer;
import org.sonatype.nexus.repository.metadata.validation.DefaultRepositoryMetadataValidator;
import org.sonatype.nexus.repository.metadata.validation.RepositoryMetadataValidator;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Singleton
@Named
public class DefaultRepositoryMetadataHandler
    implements RepositoryMetadataHandler
{
  private static final String REPOSITORY_METADATA_PATH = "/.meta/repository-metadata.xml";

  protected final RepositoryMetadataXpp3Reader repositoryMetadataXpp3Reader = new RepositoryMetadataXpp3Reader();

  protected final RepositoryMetadataXpp3Writer repositoryMetadataXpp3Writer = new RepositoryMetadataXpp3Writer();

  @Override
  public RepositoryMetadata readRepositoryMetadata(RawTransport transport)
      throws MetadataHandlerException,
             IOException
  {
    return readRepositoryMetadata(transport, new DefaultRepositoryMetadataValidator());
  }

  @Override
  public RepositoryMetadata readRepositoryMetadata(RawTransport transport, RepositoryMetadataValidator validator)
      throws MetadataHandlerException,
             IOException
  {
    try {
      byte[] data = transport.readRawData(REPOSITORY_METADATA_PATH);

      // TODO: add means for transparent on-the-fly metadata upgrade

      if (data != null) {
        try {
          ByteArrayInputStream bis = new ByteArrayInputStream(data);

          InputStreamReader isr = new InputStreamReader(bis, "UTF-8");

          RepositoryMetadata md = repositoryMetadataXpp3Reader.read(isr);

          if (validator != null) {
            validator.validate(md);
          }

          return md;
        }
        catch (XmlPullParserException e) {
          throw new MetadataHandlerException("Metadata is corrupt!", e);
        }
      }
      else {
        return null;
      }
    }
    catch (Exception e) {
      throw new MetadataHandlerException(e);
    }
  }

  @Override
  public void writeRepositoryMetadata(RepositoryMetadata metadata, RawTransport transport)
      throws MetadataHandlerException,
             IOException
  {
    writeRepositoryMetadata(metadata, transport, new DefaultRepositoryMetadataValidator());
  }

  @Override
  public void writeRepositoryMetadata(RepositoryMetadata metadata, RawTransport transport,
                                      RepositoryMetadataValidator validator)
      throws MetadataHandlerException,
             IOException
  {
    try {
      if (validator != null) {
        validator.validate(metadata);
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      OutputStreamWriter writer = new OutputStreamWriter(bos, "UTF-8");

      repositoryMetadataXpp3Writer.write(writer, metadata);

      writer.flush();

      transport.writeRawData(REPOSITORY_METADATA_PATH, bos.toByteArray());
    }
    catch (Exception e) {
      throw new MetadataHandlerException(e);
    }
  }

  @Override
  public OrderedRepositoryMirrorsMetadata fetchOrderedMirrorMetadata(RepositoryMetadata metadata,
                                                                     RawTransport transport)
      throws MetadataHandlerException,
             IOException
  {
    OrderedRepositoryMirrorsMetadata result = new OrderedRepositoryMirrorsMetadata();

    result.setVersion(OrderedRepositoryMirrorsMetadata.MODEL_VERSION);

    result.setStrategy(OrderedRepositoryMirrorsMetadata.STRATEGY_CLIENT_MANUAL);

    result.setRequestIp(null);

    result.setRequestTimestamp(System.currentTimeMillis());

    for (RepositoryMirrorMetadata mmd : (List<RepositoryMirrorMetadata>) metadata.getMirrors()) {
      OrderedMirrorMetadata omd = new OrderedMirrorMetadata();

      omd.setId(mmd.getId());
      omd.setUrl(mmd.getUrl());

      result.addMirror(omd);
    }

    return result;
  }
}
