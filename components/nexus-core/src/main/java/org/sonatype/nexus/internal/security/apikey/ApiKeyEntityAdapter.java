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
package org.sonatype.nexus.internal.security.apikey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.PbeCompression;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.orient.entity.action.BrowseEntitiesByPropertyAction;
import org.sonatype.nexus.orient.entity.action.DeleteEntitiesAction;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link ApiKey} entity adapter.
 *
 * since 3.0
 */
@Named
@Singleton
public class ApiKeyEntityAdapter
    extends IterableEntityAdapter<ApiKey>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("api_key")
      .build();

  private static final String P_DOMAIN = "domain";

  private static final String P_APIKEY = "api_key";

  private static final String P_PRIMARY_PRINCIPAL = "primary_principal";

  private static final String P_PRINCIPALS = "principals";

  private static final String I_APIKEY = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_DOMAIN)
      .property(P_APIKEY)
      .build();

  private static final String I_PRIMARY_PRINCIPAL = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_DOMAIN)
      .property(P_PRIMARY_PRINCIPAL)
      .build();

  public ApiKeyEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    super.defineType(db, type);
    PbeCompression.enableRecordEncryption(db, type);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_APIKEY, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_DOMAIN, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_PRIMARY_PRINCIPAL, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_PRINCIPALS, OType.BINARY)
        .setMandatory(true)
        .setNotNull(true);
    type.createIndex(I_APIKEY, INDEX_TYPE.UNIQUE, P_DOMAIN, P_APIKEY);
    type.createIndex(I_PRIMARY_PRINCIPAL, INDEX_TYPE.UNIQUE, P_DOMAIN, P_PRIMARY_PRINCIPAL);
  }

  @Override
  protected ApiKey newEntity() {
    return new ApiKey();
  }

  @Override
  protected void readFields(final ODocument document, final ApiKey entity) {
    String domain = document.field(P_DOMAIN, OType.STRING);
    String apiKey = document.field(P_APIKEY, OType.STRING);
    final PrincipalCollection principals = (PrincipalCollection) deserialize(document, P_PRINCIPALS);

    entity.setDomain(domain);
    entity.setApiKey(apiKey.toCharArray());
    entity.setPrincipals(principals);
  }

  private Object deserialize(final ODocument document, final String fieldName) {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    final byte[] bytes = document.field(fieldName, OType.BINARY);
    try (ObjectInputStream objects = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      return objects.readObject();
    }
    catch (IOException | ClassNotFoundException e) {
      throw Throwables.propagate(e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  @Override
  protected void writeFields(final ODocument document, final ApiKey entity) {
    document.field(P_DOMAIN, entity.getDomain());
    document.field(P_APIKEY, String.valueOf(entity.getApiKey()));
    document.field(P_PRIMARY_PRINCIPAL, entity.getPrincipals().getPrimaryPrincipal().toString());
    document.field(P_PRINCIPALS, serialize(entity.getPrincipals()));
  }

  private byte[] serialize(final Object object) {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      ObjectOutputStream objects = new ObjectOutputStream(bytes);
      objects.writeObject(object);
      objects.flush();
      return bytes.toByteArray();
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // Actions
  //

  public final DeleteEntitiesAction deleteAll = new DeleteEntitiesAction(this);

  /**
   * Browse all entities which have matching primary principal.
   */
  public final BrowseEntitiesByPropertyAction<ApiKey> browseByPrimaryPrincipal =
      new BrowseEntitiesByPropertyAction<>(this, P_PRIMARY_PRINCIPAL);

  private static final String SELECT_BY_API_KEY = String.format("SELECT FROM %s WHERE %s=? AND %s=?", DB_CLASS, P_DOMAIN, P_APIKEY);

  @Nullable
  public ApiKey findByApiKey(final ODatabaseDocumentTx db, final String domain, final char[] apiKey) {
    checkNotNull(domain);
    checkNotNull(apiKey);

    final OResultSet<ODocument> resultSet = db
        .command(new OSQLSynchQuery<ODocument>(SELECT_BY_API_KEY))
        .execute(domain, String.valueOf(apiKey));

    if (resultSet.isEmpty()) {
      return null;
    }

    return readEntity(resultSet.iterator().next());
  }
}
