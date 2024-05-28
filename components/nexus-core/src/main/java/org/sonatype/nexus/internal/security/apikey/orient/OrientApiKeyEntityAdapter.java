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
package org.sonatype.nexus.internal.security.apikey.orient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.io.ObjectInputStreamWithClassLoader;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.orient.entity.action.DeleteEntitiesAction;
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * {@link OrientApiKey} entity adapter.
 *
 * since 3.0
 */
@Named
@Singleton
public class OrientApiKeyEntityAdapter
    extends IterableEntityAdapter<OrientApiKey>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("api_key")
      .build();

  private static final String P_DOMAIN = "domain";

  private static final String P_APIKEY = "api_key";

  private static final String P_PRIMARY_PRINCIPAL = "primary_principal";

  private static final String P_PRINCIPALS = "principals";

  private static final String P_CREATED = "created";

  private static final String P_EXPIRED_TIME = "expired";

  private static final String I_APIKEY = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_DOMAIN)
      .property(P_APIKEY)
      .build();

  private static final String I_PRIMARY_PRINCIPAL = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_DOMAIN)
      .property(P_PRIMARY_PRINCIPAL)
      .property(P_PRINCIPALS)
      .build();

  private static final String DOMAIN_QUERY_STRING = format(
      "SELECT FROM %s WHERE %s = :domain ORDER BY %s", DB_CLASS, P_DOMAIN, P_CREATED);

  private static final String PRINCIPAL_QUERY_STRING = format(
      "SELECT FROM %s WHERE %s = :primary_principal ORDER BY %s", DB_CLASS, P_PRIMARY_PRINCIPAL, P_CREATED);

  private static final String DOMAIN_AND_CREATED_QUERY_STRING = format(
      "SELECT FROM %s WHERE %s = :domain AND %s > :created ORDER BY %s", DB_CLASS, P_DOMAIN, P_CREATED, P_CREATED);

  private static final String COUNT_DOMAIN_QUERY_STRING =
      format("SELECT count(*) as count FROM %s WHERE %s = :domain", DB_CLASS, P_DOMAIN);

  private static final String
      EXPIRED_QUERY_STRING = format(
      "SELECT FROM %s WHERE %s < :expired", DB_CLASS, P_CREATED);

  private final DeleteEntitiesAction deleteAll = new DeleteEntitiesAction(this);

  private final ReadEntityByPropertyAction<OrientApiKey> findByApiKey =
      new ReadEntityByPropertyAction<>(this, P_DOMAIN, P_APIKEY);

  private final ClassLoader uberClassLoader;

  @Inject
  public OrientApiKeyEntityAdapter(@Named("nexus-uber") final ClassLoader uberClassLoader) {
    super(DB_CLASS);
    this.uberClassLoader = checkNotNull(uberClassLoader);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    super.defineType(db, type);
    enableRecordEncryption(db, type);
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
    type.createProperty(P_CREATED, OType.LONG)
        .setMandatory(true)
        .setNotNull(true);
    type.createIndex(I_APIKEY, INDEX_TYPE.UNIQUE, P_DOMAIN, P_APIKEY);
    type.createIndex(I_PRIMARY_PRINCIPAL, INDEX_TYPE.UNIQUE, P_DOMAIN, P_PRIMARY_PRINCIPAL, P_PRINCIPALS);
  }

  @Override
  protected OrientApiKey newEntity() {
    return new OrientApiKey();
  }

  @Override
  protected void readFields(final ODocument document, final OrientApiKey entity) {
    String domain = document.field(P_DOMAIN, OType.STRING);
    String apiKey = document.field(P_APIKEY, OType.STRING);
    Long createdTimestamp = document.field(P_CREATED, OType.LONG);
    final PrincipalCollection principals = (PrincipalCollection) deserialize(document, P_PRINCIPALS);

    entity.setDomain(domain);
    entity.setApiKey(apiKey.toCharArray());
    entity.setPrincipals(principals);

    if (createdTimestamp == null) {
      createdTimestamp = 0L;
    }

    OffsetDateTime created = OffsetDateTime.ofInstant(Instant.ofEpochMilli(createdTimestamp), ZoneOffset.UTC);

    entity.setCreated(created);
  }

  private Object deserialize(final ODocument document, final String fieldName) {
    final byte[] bytes = document.field(fieldName, OType.BINARY);
    try (ObjectInputStream objects =
             new ObjectInputStreamWithClassLoader(new ByteArrayInputStream(bytes), uberClassLoader)) {
      return objects.readObject();
    }
    catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void writeFields(final ODocument document, final OrientApiKey entity) {
    document.field(P_DOMAIN, entity.getDomain());
    document.field(P_APIKEY, String.valueOf(entity.getApiKey()));
    document.field(P_PRIMARY_PRINCIPAL, entity.getPrincipals().getPrimaryPrincipal().toString());
    document.field(P_PRINCIPALS, serialize(entity.getPrincipals()));
    document.field(P_CREATED, Optional.ofNullable(entity.getCreated())
        .orElseGet(OffsetDateTime::now)
        .toInstant()
        .toEpochMilli());
  }

  private byte[] serialize(final Object object) {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      ObjectOutputStream objects = new ObjectOutputStream(bytes);
      objects.writeObject(object);
      objects.flush();
      return bytes.toByteArray();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  //
  // Actions
  //

  /**
   * @since 3.1
   */
  public void deleteAll(final ODatabaseDocumentTx db) {
    deleteAll.execute(db);
  }

  /**
   * Browse all entities which have matching primary principal.
   *
   * @since 3.1
   */
  public Iterable<OrientApiKey> browseByPrimaryPrincipal(final ODatabaseDocumentTx db, final String username) {
    Map<String, Object> params = ImmutableMap.of(P_PRIMARY_PRINCIPAL, username);

    return query(db, PRINCIPAL_QUERY_STRING, params);
  }

  /**
   * Browse all keys in the specified domain
   */
  public Iterable<OrientApiKey> browseByDomain(final ODatabaseDocumentTx db, final String domain) {
    Map<String, Object> params = ImmutableMap.of(P_DOMAIN, domain);

    return query(db, DOMAIN_QUERY_STRING, params);
  }

  public int countByDomainI(final ODatabaseDocumentTx db, final String domain) {
    Map<String, Object> params = ImmutableMap.of(P_DOMAIN, domain);

    List<ODocument> results = db.command(new OCommandSQL(COUNT_DOMAIN_QUERY_STRING)).execute(params);
    return results.get(0).field("count", Integer.class);
  }

  /**
   * Browse all keys in the specified domain after the created date
   */
  public Iterable<OrientApiKey> browseByDomainAndCreated(
      final ODatabaseDocumentTx db,
      final String domain,
      final OffsetDateTime created)
  {
    Map<String, Object> params = ImmutableMap.of(P_DOMAIN, domain, P_CREATED, created.toInstant().toEpochMilli());

    return query(db, DOMAIN_AND_CREATED_QUERY_STRING, params);
  }

  private Iterable<OrientApiKey> query(
      final ODatabaseDocumentTx db,
      final String query,
      final Map<String, Object> params)
  {
    List<ODocument> result = db.command(new OSQLSynchQuery<>(query)).execute(params);

    if (result != null) {
      return result.stream().map(this::readEntity).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Nullable
  public OrientApiKey findByApiKey(final ODatabaseDocumentTx db, final String domain, final char[] apiKey) {
    checkNotNull(domain);
    checkNotNull(apiKey);
    return findByApiKey.execute(db, domain, String.valueOf(apiKey));
  }

  public Iterable<OrientApiKey> browseByExpiration(
      final ODatabaseDocumentTx db,
      final OffsetDateTime expiration)
  {
    Map<String, Object> params = ImmutableMap.of(P_EXPIRED_TIME, expiration.toInstant().toEpochMilli());

    return query(db, EXPIRED_QUERY_STRING, params);
  }
}
