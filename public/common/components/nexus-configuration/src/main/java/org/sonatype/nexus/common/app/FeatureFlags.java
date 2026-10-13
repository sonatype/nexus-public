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
package org.sonatype.nexus.common.app;

/**
 * List of available feature flags
 * You can change it's values by editing ${data-dir}/nexus.properties configuration file.
 *
 * @since 3.20
 */
public class FeatureFlags
{
  /* Cargo format is temporarily hidden behind the feature flag. Default value: false */
  public static final String CARGO_FORMAT_ENABLED = "nexus.format.cargo.enabled";

  /* Hugging Face format is temporarily hidden behind the feature flag. Default value: false */
  public static final String HUGGING_FACE_FORMAT_ENABLED = "nexus.format.huggingface.enabled";

  /*
   * Kill switch for the Hugging Face XET protocol (§0.6, §5 of xet-implementation-spec).
   * Independent of HUGGING_FACE_FORMAT_ENABLED so operators can run the HF format on the
   * legacy LFS-only download path while the XET protocol is disabled. Default value: false.
   * When false the recipe skips registering XET routes and the proxy facet does not persist
   * or emit the X-Xet-Hash content-attribute; instance behaviour is bit-for-bit identical to
   * a pre-XET Nexus.
   */
  public static final String HUGGING_FACE_XET_ENABLED = "nexus.format.huggingface.xet.enabled";

  public static final String HUGGING_FACE_XET_ENABLED_NAMED_VALUE =
      "${" + HUGGING_FACE_XET_ENABLED + ":false}";

  /* Composer format is temporarily hidden behind the feature flag. Default value: false */
  public static final String COMPOSER_FORMAT_ENABLED = "nexus.format.composer.enabled";

  /*
   * Kill switch for the OCI (Open Container Initiative) repository format. When false, the OCI
   * hosted/proxy/group recipes are not registered, so OCI repositories cannot be created or served.
   * Intended as a field-rollback escape hatch. Available values: true, false. Default value: true.
   */
  public static final String OCI_FORMAT_ENABLED = "nexus.format.oci.enabled";

  public static final String OCI_FORMAT_ENABLED_NAMED_VALUE = "${nexus.format.oci.enabled:true}";

  /*
   * Feature flag for Chocolatey-specific NuGet behaviour (LegacyGallery suppression
   * in v3 hosted service index, and mixed v2+v3 group routing). When true, enables
   * Chocolatey support. Available values: true, false. Default value: true.
   */
  public static final String NUGET_CHOCOLATEY_ENABLED = "nexus.nuget.chocolatey.enabled";

  public static final String NUGET_CHOCOLATEY_ENABLED_NAMED_VALUE = "${nexus.nuget.chocolatey.enabled:true}";

  /*
   * Swift Firewall support (audit/quarantine) is gated behind this flag, which acts as a
   * kill-switch. Available values: true, false. Default value: true.
   */
  public static final String SWIFT_FIREWALL_ENABLED = "nexus.swift.firewall.enabled";

  public static final String SWIFT_FIREWALL_ENABLED_NAMED_VALUE = "${nexus.swift.firewall.enabled:true}";

  /* Docker GC Custom task enabled. Available values: true, false. Default value: false */
  public static final String DOCKER_GC_CUSTOM_TASK_ENABLED = "nexus.docker.gc.custom.enabled";

  /* Database externalization developers only. Available values: true, false. Default value: false */
  public static final String DATASTORE_DEVELOPER = "nexus.datastore.developer";

  public static final String DATASTORE_DEVELOPER_NAMED_VALUE = "${nexus.datastore.developer:false}";

  /* Distributed event service. Available values: true, false. Default value: false */
  public static final String DATASTORE_CLUSTERED_ENABLED = "nexus.datastore.clustered.enabled";

  public static final String DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE = "${nexus.datastore.clustered.enabled:false}";

  /*
   * Immediate cross-node consistency. When true on a clustered PostgreSQL node, a config/data change also
   * rings a LISTEN/NOTIFY doorbell on the distributed-event outbox so peers drain and replay it immediately
   * instead of waiting for the ~5s poll. The NOTIFY is a hint only; the durable event row still carries the
   * payload and the periodic poll remains the backstop. On by default; set
   * nexus.cluster.immediate.consistency=false to opt back out, leaving the poll as the sole mechanism exactly
   * as before. No-op on H2 / single-node deployments. Read at startup (bean registration), so changing it
   * requires a restart.
   * Available values: true, false. Default value: true.
   */
  public static final String CLUSTER_IMMEDIATE_CONSISTENCY_ENABLED = "nexus.cluster.immediate.consistency";

  public static final String CLUSTER_IMMEDIATE_CONSISTENCY_ENABLED_NAMED_VALUE =
      "${" + CLUSTER_IMMEDIATE_CONSISTENCY_ENABLED + ":true}";

  /* Zero downtime upgrades while clustered. Available values: true, false. Default value: false */
  public static final String CLUSTERED_ZERO_DOWNTIME_ENABLED = "nexus.zero.downtime.enabled";

  public static final String CLUSTERED_ZERO_DOWNTIME_ENABLED_NAMED_VALUE = "${nexus.zero.downtime.enabled:false}";

  public static final String CLUSTERED_ZERO_DOWNTIME_ENABLED_ENV = "NEXUS_ZERO_DOWNTIME_ENABLED";

  /**
   * Heartbeat interval in seconds for cluster node health monitoring.
   * <p>
   * Default: 600 seconds (10 minutes). This interval determines:
   * <ul>
   * <li>How frequently nodes write heartbeat data to the database</li>
   * <li>The maximum time before a failed node's stale heartbeat is considered inactive</li>
   * <li>The timeout for Zero Downtime Upgrade coordination (all nodes must reach consensus within this window)</li>
   * </ul>
   */
  public static final String HEARTBEAT_INTERVAL_SECONDS = "nexus.heartbeat.interval";

  public static final int HEARTBEAT_INTERVAL_SECONDS_DEFAULT = 600;

  /* Feature flag to indicate if current db is postgresql */
  public static final String DATASTORE_IS_POSTGRESQL = "datastore.isPostgresql";

  /* JWT externalization. Available values: true, false. Default value: false */
  public static final String JWT_ENABLED = "nexus.jwt.enabled";

  /* Session flag for marking content that is only for session, and should be disabled when jwt is enabled */
  public static final String SESSION_ENABLED = "nexus.session.enabled";

  /* HTTP Replication. Available values: true, false. Default value: true */
  public static final String REPLICATION_HTTP_ENABLED = "nexus.replication.http.enabled";

  /*
   * flag for skipping blob store with soft-quota violation (for Round Robin group policy)
   * Available values: true, false. Default value: false
   */
  public static final String BLOBSTORE_SKIP_ON_SOFTQUOTA_VIOLATION = "nexus.blobstore.skipOnSoftQuotaViolation";

  /*  */
  public static final String DATASTORE_BLOBSTORE_METRICS = "nexus.datastore.blobstore.metrics.enabled";

  /**
   * The Key-Value DB storage which can be used as a distributed cache. Use it intelligently,
   * for example it makes sense to cache IQ results in a DB rather than request IQ Server each time.
   * At best should be replaced by Redis cache or Apache Ignite or other.
   */
  public static final String SQL_DISTRIBUTED_CACHE = "nexus.datastore.sql.cache.enabled";

  /**
   * Validates attribute from the node_heartbeat.node_info to determine if the deployment is valid.
   */
  public static final String DATASTORE_DEPLOYMENT_VALIDATOR = "nexus.datastore.deployment.validator.enabled";

  public static final String CHANGE_REPO_BLOBSTORE_TASK_ENABLED = "nexus.change.repo.blobstore.task.enabled";

  public static final String CHANGE_REPO_BLOBSTORE_TASK_ENABLED_NAMED_VALUE =
      "${nexus.change.repo.blobstore.task.enabled:false}";

  /**
   * Feature flag to enable/disable RecalculateBlobStoreSizeTask
   */
  public static final String RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED = "nexus.recalculate.blobstore.size.task.enabled";

  public static final String RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED_NAMED_VALUE =
      "${" + RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED + ":true}";

  /**
   * Feature flag to enable/disable the audit-events cleanup task (retention pruning).
   */
  public static final String AUDIT_EVENTS_CLEANUP_TASK_ENABLED = "nexus.audit.events.cleanup.task.enabled";

  public static final String AUDIT_EVENTS_CLEANUP_TASK_ENABLED_NAMED_VALUE =
      "${" + AUDIT_EVENTS_CLEANUP_TASK_ENABLED + ":true}";

  public static final String FIREWALL_ONBOARDING_ENABLED = "nexus.firewall.onboarding.enabled";

  public static final String FIREWALL_PUB_FORMAT_ENABLED = "nexus.firewall.pub.format.enabled";

  public static final String FIREWALL_PUB_FORMAT_ENABLED_NAMED_VALUE =
      "${" + FIREWALL_PUB_FORMAT_ENABLED + ":true}";

  /**
   * Feature flag allowing Firewall audit and quarantine mode to be configured on proxy repositories
   * whose format Sonatype IQ cannot fully analyze.
   * <p>
   * The override is deliberately blanket: when enabled it lifts the format restriction for every
   * format outside {@code FirewallSupportedFormat}, restoring the pre-STL-381 behaviour rather than
   * allowlisting a named subset. Defaults to {@code false} so no instance gains the affordance
   * without being opted in explicitly.
   * <p>
   * Interim measure for FIRE-956; superseded by the global Firewall capability in FIRE-895.
   */
  public static final String FIREWALL_LIMITED_SUPPORT_FORMATS_ENABLED =
      "nexus.firewall.limited.support.formats.enabled";

  public static final String FIREWALL_LIMITED_SUPPORT_FORMATS_ENABLED_NAMED_VALUE =
      "${" + FIREWALL_LIMITED_SUPPORT_FORMATS_ENABLED + ":false}";

  public static final String CLEANUP_PREVIEW_ENABLED = "nexus.cleanup.preview.enabled";

  public static final String CLEANUP_PREVIEW_ENABLED_NAMED_VALUE = "${nexus.cleanup.preview.enabled:true}";

  public static final String CLEANUP_MAVEN_RETAIN = "nexus.cleanup.mavenRetain";

  public static final String CLEANUP_DOCKER_RETAIN = "nexus.cleanup.dockerRetain";

  public static final String CLEANUP_USE_SQL = "nexus.cleanup.useSQL";

  public static final String CLEANUP_RETAIN_ALL_FORMATS = "nexus.cleanup.retainAllFormats.enabled";

  public static final String CLEANUP_RETAIN_ALL_FORMATS_NAMED_VALUE = "${nexus.cleanup.retainAllFormats.enabled:true}";

  public static final String FORMAT_RETAIN_PATTERN = "nexus.cleanup.{format}Retain";

  public static final String DISABLE_NORMALIZE_VERSION_TASK = "nexus.cleanup.disableNormalizeVersionTask";

  public static final String DISABLE_RENORMALIZE_VERSION_TASK = "nexus.cleanup.disableReNormalizeVersionTask";

  public static final String DISABLE_CREATING_COMPONENT_INDEXES_TASK = "nexus.component.index.task";

  public static final String FIREWALL_QUARANTINE_FIX_ENABLED = "nexus.firewall.quarantineFix.enabled";

  public static final String FIREWALL_QUARANTINE_FIX_ENABLED_NAMED_VALUE =
      "${nexus.firewall.quarantineFix.enabled:false}";

  public static final String REACT_PRIVILEGES = "nexus.react.privileges";

  public static final String REACT_PRIVILEGES_NAMED_VALUE = "${nexus.react.privileges:true}";

  public static final String REACT_PRIVILEGES_MODAL_ENABLED = "nexus.react.privileges.modal.enabled";

  public static final String REACT_PRIVILEGES_MODAL_NAMED_VALUE = "${nexus.react.privileges.modal.enabled:true}";

  /**
   * Feature flag to determine if we should include the repository sizes feature
   */
  public static final String REPOSITORY_SIZE_ENABLED = "nexus.repository.size";

  public static final String REPOSITORY_SIZE_ENABLED_NAMED_VALUE = "${nexus.repository.size:true}";

  public static final String CONTENT_USAGE_ENABLED_NAMED_VALUE = "${nexus.contentUsageMetrics.enabled:true}";

  public static final String REACT_ROLES_MODAL_ENABLED = "nexus.react.roles.modal.enabled";

  public static final String REACT_ROLES_MODAL_NAMED_VALUE = "${nexus.react.roles.modal.enabled:true}";

  public static final String BLOBSTORE_OWNERSHIP_CHECK_DISABLED_NAMED_VALUE =
      "${nexus.blobstore.s3.ownership.check.disabled:false}";

  public static final String STARTUP_TASKS_DELAY_SECONDS_VALUE = "${nexus.startup.task.delay.seconds:0}";

  /**
   * Feature flag to expose H2 export database to script task
   */
  public static final String H2_DATABASE_EXPORT_SCRIPT_TASK_ENABLED = "nexus.database.export.script.task.h2.enabled";

  /* When false skips the orient not supported error. Available values: true, false. Default value: true */
  public static final String ORIENT_WARNING = "nexus.orient.warning";

  public static final String ORIENT_WARNING_NAMED_VALUE = "${nexus.orient.warning:true}";

  /**
   * When true (default), the Secure attribute will be set on the NXSESSIONID Cookie when delivered over https.
   * In deployments with HTTP-only listeners, this setting will typically have no effect.
   * Setting false for this property in HTTPS only environments is not recommended.
   *
   * See https://owasp.org/www-community/controls/SecureCookieAttribute
   */
  public static final String NXSESSIONID_SECURE_COOKIE_NAMED_VALUE = "${nexus.session.secureCookie:true}";

  public static final String ASSET_AUDITOR_ATTRIBUTE_CHANGES_ENABLED_VALUE =
      "${nexus.audit.attribute.changes.enabled:true}";

  public static final String ZERO_DOWNTIME_MARKETING_MODAL_ENABLED = "zero.downtime.marketing.modal";

  public static final String ZERO_DOWNTIME_MARKETING_MODAL_ENABLED_NAMED_VALUE =
      "${zero.downtime.marketing.modal:false}";

  /* For testing purposes only */
  public static final String ZERO_DOWNTIME_BASELINE_FAIL = "nexus.zdu.baseline.fail";

  /* For testing purposes only */
  public static final String ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED = "nexus.zdu.future.enabled";

  public static final String MALWARE_RISK_ENABLED = "nexus.malware.risk.enabled";

  public static final String MALWARE_RISK_ENABLED_NAMED_VALUE = "${nexus.malware.risk.enabled:true}";

  public static final String MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED =
      "nexus.malware.risk.on.disk.nonadmin.override.enabled";

  public static final String MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED_NAMED_VALUE =
      "${nexus.malware.risk.on.disk.nonadmin.override.enabled:false}";

  public static final String MALWARE_REMEDIATOR_TASK_CHECK_REPOSITORY_IN_KNOWN_REGISTRIES_NAMED_VALUE =
      "${nexus.malware.remediator.task.check.repository.in.known.registries:false}";

  public static final String MALWARE_REMEDIATOR_TASK_IGNORE_QUARANTINE_STATE_NAMED_VALUE =
      "${nexus.malware.remediator.task.ignore.quarantine.state:true}";

  /* properties/env vars used by secrets service */
  public static final String SECRETS_FILE = "nexus.secrets.file";

  public static final String SECRETS_FILE_ENV = "NEXUS_SECRETS_KEY_FILE";

  public static final String RECONCILE_CLEANUP_DAYS_AGO_VALUE = "${nexus.reconcile.cleanup.daysAgo:7}";

  public static final String SECRETS_API_ENABLED = "nexus.secrets.api.enabled";

  public static final String NEXUS_SECURITY_OAUTH2_ENABLED = "nexus.security.oauth2.enabled";

  public static final String NEXUS_SECURITY_FIPS_ENABLED = "nexus.security.fips.enabled";

  public static final String NEXUS_SECURITY_FIPS_ENABLED_NAMED_VALUE = "${nexus.security.fips.enabled:false}";

  public static final String NEXUS_SECURITY_PASSWORD_ALGORITHM_NAMED_VALUE =
      "${nexus.security.password.algorithm:shiro1}";

  public static final String NEXUS_SECURITY_PASSWORD_ITERATIONS_NAMED_VALUE =
      "${nexus.security.password.iterations:}";

  /*
   * Short-lived cache of successfully verified credentials, keyed by a keyed hash of (stored-hash + submitted
   * password). Lets repeated identical Basic-Auth requests skip the deliberately-expensive password KDF. Only
   * successful verifications are cached (failures always pay the full KDF cost, preserving brute-force resistance),
   * and the stored-hash is part of the key so a password change transparently invalidates prior entries.
   */
  public static final String NEXUS_SECURITY_PASSWORD_CACHE_ENABLED_NAMED_VALUE =
      "${nexus.security.password.cache.enabled:true}";

  public static final String NEXUS_SECURITY_PASSWORD_CACHE_SIZE_NAMED_VALUE =
      "${nexus.security.password.cache.size:1000}";

  public static final String NEXUS_SECURITY_PASSWORD_CACHE_EXPIRE_SECONDS_NAMED_VALUE =
      "${nexus.security.password.cache.expireSeconds:2}";

  public static final String NEXUS_SECURITY_SECRETS_ALGORITHM_NAMED_VALUE =
      "${nexus.security.secrets.algorithm:PBKDF2WithHmacSHA1}";

  public static final String NEXUS_SECURITY_SECRETS_ITERATIONS_NAMED_VALUE =
      "${nexus.security.secrets.iterations:}";

  public static final String CONTAINER_IMAGES_EVAL_ENABLED = "nexus.container.images.eval.enabled";

  public static final String CONTAINER_IMAGES_EVAL_ENABLED_NAMED = "${nexus.container.images.eval.enabled:-true}";

  public static final String CONTAINER_IMAGES_EVAL_ENABLED_NAMED_VALUE = "${nexus.container.images.eval.enabled:true}";

  public static final String NEXUS_SECURITY_AUTH0_USER_MANAGEMENT_ENABLED =
      "nexus.security.auth0.userManagement.enabled";

  public static final String FIREWALL_CONTAINER_WORK_DIRECTORY_VALUE = "${nexus.firewall.container.workdirectory:}";

  /* Layer download timeout for container image scanning, in minutes. When unset, the scanner default is used. */
  public static final String FIREWALL_CONTAINER_DOWNLOAD_TIMEOUT_MINUTES =
      "nexus.firewall.container.download.timeout.minutes";

  public static final String FIREWALL_CONTAINER_DOWNLOAD_TIMEOUT_MINUTES_VALUE =
      "${nexus.firewall.container.download.timeout.minutes:}";

  public static final String EGRESS_METRICS_AGGREGATION_TASK_VISIBLE = "${nexus.egressmetrics.task.visible:false}";

  /*
   * PyPi metadata features (PEP 658 + 691). Enables metadata distribution and JSON API. Available values: true, false.
   * Default value: false
   */
  public static final String PYPI_METADATA_ENABLED = "nexus.pypi.metadata.enabled";

  public static final String PYPI_METADATA_ENABLED_NAMED_VALUE = "${nexus.pypi.metadata.enabled:false}";

  /*
   * PyPI repair-metadata-content-type task visibility. When true, the task appears in the Tasks UI so
   * operators can monitor and re-run it. Default: false (hidden; auto-scheduled via migration step).
   */
  public static final String PYPI_REPAIR_METADATA_CONTENT_TYPE_TASK_VISIBLE =
      "nexus.pypi.repair.metadata.content.type.task.visible";

  public static final String PYPI_REPAIR_METADATA_CONTENT_TYPE_TASK_VISIBLE_NAMED_VALUE =
      "${" + PYPI_REPAIR_METADATA_CONTENT_TYPE_TASK_VISIBLE + ":false}";

  public static final String NEXUS_USER_CONFIGURATION_SOURCE_ENABLED = "nexus.user.configuration.source.enabled";

  /* ExtJS Capabilities Page. Available values: true, false. Default value: false */
  public static final String EXTJS_CAPABILITIES_ENABLED = "nexus.extjs.capabilities.enabled";

  public static final String EXTJS_CAPABILITIES_NAMED_VALUE = "${nexus.extjs.capabilities.enabled:false}";

  /* React Capabilities Page. Available values: true, false. Default value: true */
  public static final String REACT_CAPABILITIES_ENABLED = "nexus.react.capabilities.enabled";

  public static final String REACT_CAPABILITIES_NAMED_VALUE = "${nexus.react.capabilities.enabled:true}";

  /*
   * Gates the new React onboarding wizard. When enabled, the ExtJS onboarding wizard defers so the React wizard can
   * take over. Available values: true, false. Default value: true
   */
  public static final String REACT_ONBOARDING_ENABLED = "nexus.react.onboarding.enabled";

  public static final String REACT_ONBOARDING_ENABLED_NAMED_VALUE = "${nexus.react.onboarding.enabled:true}";

  /* Enable principal permissions cache. Default value: true */
  public static final String PRINCIPAL_PERMISSIONS_CACHE_ENABLED_NAMED_VALUE =
      "${nexus.security.principal.permissions.cache.enabled:true}";

  /* Preview UI for anonymous users. Available values: true, false. Default value: true */
  public static final String PREVIEW_UI_ANONYMOUS_ENABLED = "nexus.preview.ui.anonymous.enabled";

  public static final String PREVIEW_UI_ANONYMOUS_ENABLED_NAMED_VALUE =
      "${nexus.preview.ui.anonymous.enabled:true}";

  /* Preview UI for logged-in users. Available values: true, false. Default value: true */
  public static final String PREVIEW_UI_LOGGEDIN_ENABLED = "nexus.preview.ui.loggedin.enabled";

  public static final String PREVIEW_UI_LOGGEDIN_ENABLED_NAMED_VALUE =
      "${nexus.preview.ui.loggedin.enabled:true}";

  /* Default to Preview UI for all users. Available values: true, false. Default value: false */
  public static final String PREVIEW_UI_DEFAULT_ENABLED = "nexus.preview.ui.default.enabled";

  public static final String PREVIEW_UI_DEFAULT_ENABLED_NAMED_VALUE =
      "${nexus.preview.ui.default.enabled:false}";

  /* Disable Legacy UI (ExtJS) for all users. Available values: true, false. Default value: false */
  public static final String PREVIEW_UI_LEGACY_DISABLED = "nexus.preview.ui.legacy.disabled";

  public static final String PREVIEW_UI_LEGACY_DISABLED_NAMED_VALUE =
      "${nexus.preview.ui.legacy.disabled:false}";

  /* Disable Classic UI switch feedback collection. Available values: true, false. Default value: false */
  public static final String PREVIEW_UI_SWITCH_FEEDBACK_DISABLED =
      "nexus.preview.ui.switch.feedback.disabled";

  public static final String PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_NAMED_VALUE =
      "${nexus.preview.ui.switch.feedback.disabled:false}";

  public static final String PREVIEW_UI_SWITCH_FEEDBACK_DISABLED_KEY =
      "preview.ui.switch.feedback.disabled";

  /*
   * Preview UI settings page visibility in the legacy UI.
   * When true, Preview UI REST endpoints and the settings page are registered.
   * Available values: true, false. Default value: true
   */
  public static final String PREVIEW_UI_SETTINGS_ENABLED = "nexus.previewui.enabled";

  public static final String PREVIEW_UI_SETTINGS_ENABLED_NAMED_VALUE = "${nexus.previewui.enabled:true}";

  /*
   * Gates Preview UI audit event DB persistence and the audit log REST/UI surface.
   * Intentionally decoupled from the preview UI visibility flags so audit DB writes can be
   * deferred until the persistence path is production-ready.
   * Available values: true, false. Default value: true
   */
  public static final String PREVIEW_UI_AUDIT_ENABLED = "nexus.previewui.audit.enabled";

  public static final String PREVIEW_UI_AUDIT_ENABLED_NAMED_VALUE = "${nexus.previewui.audit.enabled:true}";

  /*
   * Per-realm principal permissions cache maximum size. Default value: 1000. Override per realm by appending the
   * realm name, e.g. nexus.authorizingrealm.permissionscache.maximumsize.LdapRealm=5000.
   */
  public static final String PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE =
      "nexus.authorizingrealm.permissionscache.maximumsize";

  public static final String PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE_NAMED_VALUE =
      "${nexus.authorizingrealm.permissionscache.maximumsize:1000}";

  /* Principal permissions cache record statistics. Default value: false (enable when diagnosing). */
  public static final String PRINCIPAL_PERMISSIONS_CACHE_RECORD_STATS =
      "nexus.authorizingrealm.permissionscache.recordstats";

  public static final String PRINCIPAL_PERMISSIONS_CACHE_RECORD_STATS_NAMED_VALUE =
      "${nexus.authorizingrealm.permissionscache.recordstats:false}";

  /* Principal permissions cache concurrency level. Default value: 16 */
  public static final String PRINCIPAL_PERMISSIONS_CACHE_CONCURRENCY_LEVEL =
      "nexus.authorizingrealm.permissionscache.concurrencylevel";

  public static final String PRINCIPAL_PERMISSIONS_CACHE_CONCURRENCY_LEVEL_NAMED_VALUE =
      "${nexus.authorizingrealm.permissionscache.concurrencylevel:16}";

  // Hosted Repository Evaluation feature. Default value: true.
  // Operators can explicitly set nexus.hosted.repository.evaluation.enabled=false to disable.
  public static final String HOSTED_REPOSITORY_EVALUATION_ENABLED = "nexus.hosted.repository.evaluation.enabled";

  public static final String HOSTED_REPOSITORY_EVALUATION_ENABLED_NAMED_VALUE =
      "${nexus.hosted.repository.evaluation.enabled:true}";

  public static final String HOSTED_REPOSITORY_WORK_DIRECTORY_VALUE =
      "${nexus.lifecycle.hosted-repository.workdirectory:}";

  /**
   * Controls whether query parameter forwarding is available for Raw proxy repositories.
   * When enabled, the UI configuration panel and REST API fields ({@code forwardQueryParameters},
   * {@code excludedQueryParameters}) become visible and functional. When disabled (the default),
   * the feature is completely hidden: the UI panel is suppressed and the REST API ignores
   * these attributes on both input and output.
   *
   * Default: false (feature hidden)
   */
  public static final String RAW_QUERYPARAMS_FORWARDING_ENABLED = "nexus.raw.queryparams.forwarding.enabled";

  public static final String RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE =
      "${nexus.raw.queryparams.forwarding.enabled:false}";

  /**
   * Controls whether internal JVM/infrastructure metrics (JVM gauges, Jetty metrics, HikariCP metrics, thread dumps,
   * HTTP request metrics, and @Timed/@ExceptionMetered aspects) are registered. When disabled, the metrics endpoints
   * remain available but only expose custom application gauges. Default: true (all metrics enabled).
   */
  public static final String METRICS_INTERNAL_ENABLED = "nexus.metrics.internal.enabled";

  public static final String METRICS_INTERNAL_ENABLED_NAMED_VALUE = "${nexus.metrics.internal.enabled:true}";

  /*
   * Authentication rate limiting (brute force protection, CWE-307). Available values: true, false. Default value: true
   */
  public static final String AUTH_RATE_LIMIT_ENABLED = "nexus.auth.ratelimit.enabled";

  public static final String AUTH_RATE_LIMIT_ENABLED_NAMED_VALUE = "${nexus.auth.ratelimit.enabled:true}";

  /* NuGet Symbol Server support. Available values: true, false. Default value: true */
  public static final String NUGET_SYMBOL_SERVER_ENABLED = "nexus.nuget.symbol.server.enabled";

  public static final String NUGET_SYMBOL_SERVER_ENABLED_NAMED_VALUE = "${nexus.nuget.symbol.server.enabled:true}";

  /* Firewall capability shim for backwards API compatibility. Available values: true, false. Default value: true */
  public static final String FIREWALL_CAPABILITY_SHIM_ENABLED = "nexus.firewall.capability.shim.enabled";

  /*
   * Kill switch for the new repository-config-driven firewall lifecycle (FirewallRepositoryService
   * — audit task scheduling, online/offline + IQ-capability transition handling, RepositoryUpdated/
   * Created/Deleted event handling). Setting this to false disables the new lifecycle entirely;
   * existing repository firewall.mode configuration remains in storage but is dormant. Intended
   * as a field-rollback escape hatch for the migration window. Available values: true, false.
   * Default value: true.
   */
  public static final String FIREWALL_REPOSITORY_SERVICE_ENABLED = "nexus.firewall.repository.service.enabled";

  /**
   * Logical deletion (soft-delete) and recovery support (NEXUS-49147).
   * When enabled, delete operations mark entities as soft-deleted instead of permanently removing them,
   * allowing recovery within a configurable retention period.
   */
  public static final String SOFT_DELETE_ENABLED = "nexus.softDelete.enabled";

  public static final String SOFT_DELETE_ENABLED_NAMED_VALUE = "${nexus.softDelete.enabled:false}";

  /**
   * Number of days to retain soft-deleted entities before permanent purge.
   */
  public static final String SOFT_DELETE_RETENTION_DAYS = "nexus.softDelete.retentionDays";

  public static final String SOFT_DELETE_RETENTION_DAYS_NAMED_VALUE = "${nexus.softDelete.retentionDays:30}";

  /**
   * Comma-separated allowlist of repository formats for which soft-delete is active. Enables a phased,
   * format-by-format rollout: the soft-delete schema columns are added to every format up front (they
   * are inert/nullable), but a delete only becomes a recoverable soft-delete when {@link #SOFT_DELETE_ENABLED}
   * is on AND the repository's format is in this list. Fail-closed: empty/unset means <em>no</em> format
   * is soft-deleted (everything hard-deletes), even when the flag is on. The single-entry wildcard
   * {@code *} enables all formats. Formats not listed hard-delete exactly as before, so their
   * format-specific metadata handlers are not required until they are added here.
   */
  public static final String SOFT_DELETE_FORMATS = "nexus.softDelete.formats";

  public static final String SOFT_DELETE_FORMATS_NAMED_VALUE = "${nexus.softDelete.formats:}";

  /*
   * FIRE-105 / NEXUS-52802 — PyPI PCCS throughput perf fixes (template content cache, Guava HTML escaper,
   * pre-escape relocation + safeLink scheme guard, reflection cache). Gates the optimised PyPI
   * simple-index rendering path; default true (optimised path on unless explicitly
   * disabled). Set false to run the legacy path. Available values: true, false.
   */
  public static final String NEXUS_PCCS_PERF_PYPI_ENABLED = "nexus.pccs.perf.pypi.enabled";

  public static final String NEXUS_PCCS_PERF_PYPI_ENABLED_NAMED_VALUE = "${nexus.pccs.perf.pypi.enabled:true}";

  /*
   * NEXUS-52802 — npm PCCS throughput perf fixes (shared ObjectMapper singleton + ThreadLocal,
   * reflection cache). Gates the optimised npm metadata streaming path; default true (optimised
   * path on unless explicitly disabled). Set false to run the legacy per-request path. Available
   * values: true, false.
   */
  public static final String NEXUS_PCCS_PERF_NPM_ENABLED = "nexus.pccs.perf.npm.enabled";

  public static final String NEXUS_PCCS_PERF_NPM_ENABLED_NAMED_VALUE = "${nexus.pccs.perf.npm.enabled:true}";

  /*
   * FIRE-732 / FIRE-773 / FIRE-887 — npm PCCS rendered metadata view. When true and a base URL is configured,
   * npm proxy metadata responses are served from a pre-rendered cached blob, bypassing both parse
   * passes on cache hits.
   */
  public static final String NEXUS_PROXY_RENDERED_VIEW_NPM_ENABLED = "nexus.proxy.rendered.view.npm.enabled";

  public static final String NEXUS_PROXY_RENDERED_VIEW_NPM_ENABLED_NAMED_VALUE =
      "${nexus.proxy.rendered.view.npm.enabled:false}";

  /*
   * FIRE-732 / FIRE-773 — PyPI PCCS rendered metadata view. Same as the npm flag but for PyPI
   * HTML and JSON simple indexes.
   */
  public static final String NEXUS_PROXY_RENDERED_VIEW_PYPI_ENABLED = "nexus.proxy.rendered.view.pypi.enabled";

  public static final String NEXUS_PROXY_RENDERED_VIEW_PYPI_ENABLED_NAMED_VALUE =
      "${nexus.proxy.rendered.view.pypi.enabled:false}";

  /*
   * Feature flag for the Group Rendered View optimization for PyPI format.
   * When enabled, PyPI group repositories can serve cached rendered metadata views
   * without member dispatch when the cache is fresh.
   */
  public static final String GROUP_RENDERED_VIEW_PYPI_ENABLED = "nexus.group.rendered.view.pypi.enabled";

  public static final String GROUP_RENDERED_VIEW_PYPI_ENABLED_NAMED_VALUE =
      "${nexus.group.rendered.view.pypi.enabled:false}";

  /*
   * Feature flag for the Group Rendered View optimization for npm format.
   * When enabled, npm group repositories can serve cached rendered metadata views
   * without member dispatch when the cache is fresh.
   */
  public static final String GROUP_RENDERED_VIEW_NPM_ENABLED = "nexus.group.rendered.view.npm.enabled";

  public static final String GROUP_RENDERED_VIEW_NPM_ENABLED_NAMED_VALUE =
      "${nexus.group.rendered.view.npm.enabled:false}";

  /* Enables batched S3 DeleteObjects path in the compact task. Default value: false */
  public static final String S3_COMPACT_BATCH_ENABLED = "nexus.s3.compact.batchEnabled";

  public static final String S3_COMPACT_BATCH_ENABLED_NAMED_VALUE = "${nexus.s3.compact.batchEnabled:false}";

  /*
   * Development gate for the Support Zip Generation on Cloud epic (NEXUS-48398). When enabled,
   * exposes the HA support-zip REST endpoints under /service/rest/internal/cloud/supportzip/* on the
   * cloud edition via a dedicated cloud-only resource. Default value: false so the endpoints
   * cannot be enabled on main; intended for internal testing environments only until the epic
   * is ready to ship.
   */
  public static final String SUPPORT_ZIP_CLOUD_ENABLED = "nexus.support.zip.cloud.enabled";

  /* Blobs per S3 DeleteObjects call; max 333 (333×3 keys < S3 limit of 1000). Default value: 300 */
  public static final String S3_COMPACT_DELETE_BATCH_SIZE = "nexus.s3.compact.deleteBatchSize";

  public static final String S3_COMPACT_DELETE_BATCH_SIZE_NAMED_VALUE = "${nexus.s3.compact.deleteBatchSize:300}";

  /* Parallel worker threads per blobstore in the compact batch-delete phase. Default value: 8 */
  public static final String S3_COMPACT_WORKER_POOL_SIZE = "nexus.s3.compact.workerPoolSize";

  public static final String S3_COMPACT_WORKER_POOL_SIZE_NAMED_VALUE = "${nexus.s3.compact.workerPoolSize:8}";
}
