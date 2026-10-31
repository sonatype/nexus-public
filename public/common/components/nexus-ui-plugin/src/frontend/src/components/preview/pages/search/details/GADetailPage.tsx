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

import React, { useState, useCallback, useEffect, useMemo } from 'react';
import { useRouter, useCurrentStateAndParams } from '@uirouter/react';
import {
  Badge,
  Box,
  Flex,
  Tabs,
  Callout,
  Button,
  ScrollArea,
} from '@radix-ui/themes';
import { ArrowLeft, Package } from 'lucide-react';

import type { GADetailTab } from '../core';
import { GA_SEARCH_ROUTE_NAMES, TAB_ROUTE_MAP, getTabFromRoute } from '../core';
import { consumeSearchReturnUrl } from '../unified/useSearchNavigation';

import { Breadcrumbs } from './Breadcrumbs';
import { GADetailHeader } from './GADetailHeader';
import { parseGaId as splitGaId } from './gaIdUtils';
import { buildBrowseTarget, buildComponentPath } from './detailHelpers';
import { useGADetail } from './useGADetail';
import { useComponentVersions } from './useComponentVersions';
import { useComponentSecurity } from './useComponentSecurity';
import GAOverviewTab from './GAOverviewTab';
import GAVersionsTab from './GAVersionsTab';
import GARepositoriesTab from './GARepositoriesTab';
import GAFilesTab from './GAFilesTab';
import GASecurityTab from './GASecurityTab';
import { useGARepositoriesForVersion } from './useGARepositoriesForVersion';

interface GADetailPageProps {
  /**
   * GA identifier from route params, already decoded.
   *
   * The route declares `gaId` as `type: 'path'`, so UI-Router encodes on write and decodes on
   * read. Do not decode it again here and do not encode it before handing it back to
   * `stateService.go` — either would break the round trip. A raw-format path can contain '%',
   * where a second decodeURIComponent throws URIError and blanks the page.
   */
  gaId: string;
  /**
   * Selected version from the URL, or null when it carries none.
   *
   * Nullable, not just optional: the route declares `version: { value: null, squash: true }`, so
   * UI-Router resolves a version-less URL to null and hands that to this prop. Declaring it
   * `string | undefined` was a lie that hid an infinite render loop for versionless formats.
   */
  version?: string | null;
}

/**
 * GADetailPage - Main container for GA detail view with tabs.
 *
 * Tab navigation is URL-driven via router.
 * Files and Security tabs require a version to be selected.
 */
export function GADetailPage({ gaId: gaIdParam, version }: GADetailPageProps) {
  const router = useRouter();
  const { params: routeParams } = useCurrentStateAndParams();
  /** Live $stateParams — UIView resolve props can stay stale when only `version` changes (UIView useMemo deps). */
  const initialVersion = useMemo(() => {
    const fromRoute = routeParams?.version as string | null | undefined;
    if (fromRoute != null && fromRoute !== '') {
      return fromRoute;
    }
    return version;
  }, [routeParams, version]);

  // No parseGaId (decodeURIComponent) here: `type: 'path'` already decoded it — see the prop doc.
  const gaId = gaIdParam || '';

  // Which tab is active, from the route name — drives Tabs.Root below. It no longer gates any
  // fetching: every tab's data source is bounded and owns its own lifecycle.
  const currentTab = getTabFromRoute(router.globals.current?.name || '');

  const {
    detail,
    selectedVersion,
    assets,
    assetsLoading,
    versionLastUpdated,
    selectVersion,
  } = useGADetail({ gaId, initialVersion });

  // Owned here, above Tabs.Root: Radix unmounts inactive Tabs.Content (no forceMount is
  // passed below), so a machine owned inside the Versions tab would lose its page cache on
  // every tab switch. gaId is the decoded 'format:group:name' string every consumer here
  // expects — useGADetail and useComponentSecurity both split it themselves.
  const versionsState = useComponentVersions({ gaId });

  /**
   * Newest version, for anything that needs a default. Latched by the versions machine from its
   * eager, default-ordered first page — not read off `versionsState.versions`, which is the page
   * the user is currently looking at and so becomes the oldest version under an ascending sort,
   * this page's first row on page 2, or the first match once a filter is typed. Not
   * `detail.versions` either: that is empty until an aggregate-backed tab loads, and was ordered
   * by a client-side comparator that dropped qualifiers.
   */
  const newestVersion = versionsState.newestVersion ?? undefined;

  /**
   * What every downstream tab treats as "the current version": user selection, then the URL's
   * initial version, then the newest version off the versions machine. Falls back to null so
   * hooks that gate on it don't fire spurious fetches before any version is known.
   *
   * `??` throughout, never `||`. '' is the real selected version of a versionless format, and it
   * is falsy — with `||` every candidate for a raw component is falsy ('' , null, ''), so this
   * collapsed to null and the Repositories tab and Overview's Repository row came up empty for
   * every one of them. `initialVersion` is nullish-coalesced for the same reason the sync effect
   * in useGADetail is: a squashed `version` param resolves to null, not undefined.
   */
  const effectiveVersion = selectedVersion ?? initialVersion ?? newestVersion ?? null;

  const securityResult = useComponentSecurity({ gaId, version: selectedVersion });
  const {
    rows: repoRows,
    totalCount: repoCount,
    loading: repoLoading,
    error: repoError,
  } = useGARepositoriesForVersion({ gaId, selectedVersion: effectiveVersion });


  const securityCount =
    securityResult.data &&
    (securityResult.data.criticalCount +
      securityResult.data.severeCount +
      securityResult.data.moderateCount +
      securityResult.data.lowCount);

  /**
   * Canonicalise the URL once the newest version is known, so `?version` is always present and
   * the selected version has exactly one source of truth. `location: 'replace'` keeps this out
   * of history — Back must return to search, not to the version-less URL we just left.
   *
   * Three cases deliberately do nothing:
   *  - a version is already in the URL — nothing to resolve
   *  - newestVersion is undefined — page 1 has not landed yet
   *  - newestVersion is '' — a versionless format. The route squashes the param, so '' cannot
   *    round-trip; redirecting would be a no-op that re-fires on every render.
   */
  useEffect(() => {
    if (initialVersion != null && initialVersion !== '') return;
    if (newestVersion === undefined || newestVersion === '') return;
    router.stateService.go(
      router.globals.current?.name || GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW,
      { gaId, version: newestVersion },
      { location: 'replace' },
    );
  }, [initialVersion, newestVersion, router, gaId]);

  /**
   * Versionless formats (raw) resolve in-context rather than through the URL: the effect above
   * skips them because '' cannot round-trip through the squashed `version` param, and nothing
   * else ever moves the machine's `selectedVersion` off its initial `null`. Without this,
   * shouldLoadAssets in gaDetailMachine never passes and the Files/Security tabs stay
   * permanently empty for every raw component (NEXUS-54201).
   */
  useEffect(() => {
    if (initialVersion != null && initialVersion !== '') return;
    if (newestVersion !== '') return;
    if (selectedVersion === '') return;
    selectVersion('');
  }, [initialVersion, newestVersion, selectedVersion, selectVersion]);

  /**
   * Header repository/group facets. SEARCH_REPOSITORIES rows for the selected version are
   * partitioned by type: group-type rows drive the Group facet, everything else (hosted/proxy)
   * drives the Repository facet. Seeded from the URL (deep-link support, NEXUS-54428), then
   * re-defaulted whenever the version (and thus repoRows) changes and the prior selection no
   * longer serves it.
   */
  const routeRepository = (routeParams?.repository as string | null | undefined) ?? null;
  const routeGroup = (routeParams?.group as string | null | undefined) ?? null;
  const [selectedRepository, setSelectedRepository] = useState<string | null>(routeRepository);
  const [selectedGroup, setSelectedGroup] = useState<string | null>(routeGroup);
  useEffect(() => {
    // Rows arrive asynchronously; until they do, keep whatever the URL seeded so a deep-linked
    // repository/group is not clobbered to null before it can be validated.
    if (repoRows.length === 0) {
      return;
    }
    const members = repoRows.filter((r) => r.type !== 'group');
    const groups = repoRows.filter((r) => r.type === 'group');
    setSelectedRepository((prev) =>
      prev && members.some((r) => r.repositoryName === prev)
        ? prev
        : members[0]?.repositoryName ?? null
    );
    setSelectedGroup((prev) =>
      prev && groups.some((r) => r.repositoryName === prev)
        ? prev
        : groups[0]?.repositoryName ?? null
    );
  }, [repoRows]);

  /**
   * Re-adopt the URL's facet params after the initial mount. The useState seeds above run once, so
   * without this a browser Back/Forward — which changes routeParams and re-renders via
   * useCurrentStateAndParams — would leave the selectors pinned to the value they were seeded with.
   *
   * Runs after the validity effect so that when a Back also changes the version (and thus repoRows,
   * which arrives in a later async commit), the re-adopted value is still reconciled against the new
   * rows. Guarded on `!= null`: a URL that carries no facet param must not clobber a selection the
   * validity effect auto-defaulted, so we only sync a value the URL actually provides.
   */
  useEffect(() => {
    if (routeRepository != null) {
      setSelectedRepository(routeRepository);
    }
    if (routeGroup != null) {
      setSelectedGroup(routeGroup);
    }
  }, [routeRepository, routeGroup]);

  /**
   * Facet changes update local state and mirror into the URL as dynamic params (replace, not
   * push, so the selector does not spam browser history). `inherit` keeps gaId/version/tab.
   */
  const changeRepository = useCallback(
    (repository: string) => {
      setSelectedRepository(repository);
      router.stateService.go('.', { repository }, { location: 'replace', inherit: true });
    },
    [router]
  );
  const changeGroup = useCallback(
    (group: string) => {
      setSelectedGroup(group);
      router.stateService.go('.', { group }, { location: 'replace', inherit: true });
    },
    [router]
  );

  /**
   * Files tab is scoped to the selected repository so it mirrors the header context. The
   * aggregate walk returns assets across all repositories serving the version; narrow to the
   * chosen one when set, otherwise show everything.
   */
  const filesAssets = useMemo(
    () => (selectedRepository ? assets.filter((a) => a.repository === selectedRepository) : assets),
    [assets, selectedRepository]
  );

  // Handle tab change via router
  const handleTabChange = useCallback(
    (tab: string) => {
      const routeName = TAB_ROUTE_MAP[tab as GADetailTab];
      if (routeName) {
        router.stateService.go(routeName, {
          gaId,
          version: effectiveVersion || undefined,
        });
      }
    },
    [router, gaId, effectiveVersion],
  );

  /**
   * Selecting a version updates the header (and downstream tabs). Stay on the current
   * component-detail child state when possible so the Versions tab does not jump to Overview.
   * Parent-only `preview.browse.search.component` has no tab segment — use Overview there.
   */
  const handleVersionSelect = (versionStr: string) => {
    selectVersion(versionStr);
    const currentName = router.globals.current?.name ?? '';
    const isNestedComponentTab =
      currentName.startsWith(`${GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL}.`) &&
      currentName !== GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL;
    const targetState = isNestedComponentTab
      ? currentName
      : GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_OVERVIEW;
    router.stateService.go(targetState, {
      gaId,
      version: versionStr,
    });
  };

  /**
   * Back button on the error state. Intentionally does NOT restore the prior
   * search context: if the detail page rendered an error we may not have a
   * trustworthy sessionStorage payload for the previous search, and the user
   * has likely reached a dead end and simply wants the plain search page.
   * The breadcrumb (see {@link handleBreadcrumbSearchClick}) is the affordance
   * for state-preserving return; it consumes the stored return URL, and this
   * handler deliberately does not.
   */
  const handleBackClick = () => {
    router.stateService.go('preview.browse.search.unified');
  };

  /**
   * Return to unified search at the exact URL the user left.
   *
   * `navigateToDetail` captured that URL under `SEARCH_RETURN_URL_KEY`;
   * `consumeSearchReturnUrl` reads it, clears it, and rejects anything that is
   * not a unified-search hash. Assigning `window.location.hash` (rather than
   * calling `stateService.go`) is deliberate: the URL carries filter params the
   * route does not declare, and UI-Router rebuilds the URL from declared params
   * only, so a `go` would strip them. A hash assignment reaches the router as a
   * URL-sourced sync, which it does not rewrite.
   *
   * With no usable stored URL, fall back to the bare search page.
   */
  const handleBreadcrumbSearchClick = useCallback(() => {
    const returnUrl = consumeSearchReturnUrl();
    if (returnUrl) {
      window.location.hash = returnUrl;
      return;
    }
    router.stateService.go('preview.browse.search.unified');
  }, [router]);

  /*
   * No spinner and no whole-page error state: there is nothing page-wide left to wait on or to
   * fail. `detail` is the shell, derived synchronously from the gaId, so it is non-null on the
   * first render — this branch only remains for the empty-gaId case. Each tab owns the loading
   * and error state of its own bounded source.
   */
  if (!detail) {
    return (
      <Box p="6">
        <Button variant="ghost" onClick={handleBackClick} mb="4">
          <ArrowLeft size={16} />
          Back to Search
        </Button>
        <Callout.Root color="red">
          <Callout.Icon>
            <Package size={16} />
          </Callout.Icon>
          <Callout.Text>Component not found</Callout.Text>
        </Callout.Root>
      </Box>
    );
  }

  // `??`, not `||`: '' is a real selected version of a versionless format, not an absence, so it
  // must not fall through to initialVersion/newestVersion and build a path for the wrong version.
  const ver = selectedVersion ?? initialVersion ?? newestVersion ?? '';

  // Registry path for this component/version, used to deep-link into Browse. Null for unsupported
  // formats or a versionless component; buildBrowseTarget then falls back to the repository root.
  const componentPath = ver ? buildComponentPath(gaId, ver) : null;

  /**
   * Router target for the header's "View in Browse" action (NEXUS-54430), which replaced the
   * copy-path button. It deep-links to this component/version's folder in the new Browse tree via
   * the shared {@link buildBrowseTarget} helper — the same helper the Files tab uses, so both
   * resolve to the current edition's Browse URL rather than the Classic UI.
   *
   * The repository is the one selected in the header — the same repository the Download action
   * targets — so a component that lives in several repositories deep-links into the one the user
   * is actually looking at, and switching the Repository selector re-points the link. Before a
   * selection resolves it falls back to the first repository the version lives in (repoRows is
   * ordered by the API). When no repository is known yet — still loading, or the version is in
   * none — the target is null and the button is hidden. `componentPath` may be null for
   * unsupported formats or versionless components; buildBrowseTarget then falls back to the
   * repository root, which is still useful.
   */
  const browseTarget = buildBrowseTarget(
    selectedRepository ?? repoRows[0]?.repositoryName ?? '',
    componentPath,
  );

  return (
    <ScrollArea scrollbars="vertical" style={{ height: '100%' }}>
      <Box px="4" pt="0" pb="6">
        <Box maxWidth="1280px" mx="auto" width="100%" pb="9">
          {/* Breadcrumbs */}
          <Breadcrumbs
            items={[
              {
                label: 'Search',
                onClick: handleBreadcrumbSearchClick,
              },
              {
                label: detail.displayName,
              },
              {
                label: ver,
              },
            ]}
          />

          {/* Component Header: group/repository context, name/version, Download, View in Browse */}
          <GADetailHeader
            gaId={gaId}
            displayName={detail.displayName}
            coordinate={splitGaId(gaId).group}
            selectedVersion={effectiveVersion}
            repoRows={repoRows}
            selectedRepository={selectedRepository}
            onRepositoryChange={changeRepository}
            selectedGroup={selectedGroup}
            onGroupChange={changeGroup}
            onViewInBrowse={
              browseTarget
                ? () => router.stateService.go(browseTarget.state, browseTarget.params)
                : undefined
            }
          />

          {/* Tabs */}
          <Tabs.Root value={currentTab} onValueChange={handleTabChange}>
              <Box px="4" pt="0" pb="2">
                <Tabs.List size="2" style={{ borderBottom: 'none' }}>
                  <Tabs.Trigger value="overview">Overview</Tabs.Trigger>
                  <Tabs.Trigger value="versions">
                    <Flex align="center" gap="2">
                      Versions
                      {/* totalVersions, not total: the badge counts the component's versions, so
                          it must not drop to the match count while a filter is active in the tab. */}
                      <Badge color="gray" variant="soft" size="1" radius="full">
                        {versionsState.totalVersions}
                      </Badge>
                    </Flex>
                  </Tabs.Trigger>
                  <Tabs.Trigger value="repositories">
                    <Flex align="center" gap="2">
                      Repositories
                      <Badge color="gray" variant="soft" size="1" radius="full">{repoCount}</Badge>
                    </Flex>
                  </Tabs.Trigger>
                  <Tabs.Trigger value="files">
                    <Flex align="center" gap="2">
                      Files
                      {/* !== null, not truthiness: '' is the valid selected version for
                          versionless formats (raw), and is falsy. */}
                      <Badge color="gray" variant="soft" size="1" radius="full">
                        {selectedVersion !== null ? filesAssets.length : 0}
                      </Badge>
                    </Flex>
                  </Tabs.Trigger>
                  <Tabs.Trigger value="security">
                    <Flex align="center" gap="2">
                      Security
                      {typeof securityCount === 'number' && (
                        <Badge color="gray" variant="soft" size="1" radius="full">
                          {securityCount}
                        </Badge>
                      )}
                    </Flex>
                  </Tabs.Trigger>
                </Tabs.List>
              </Box>

              <Box p="4">
                <Tabs.Content value="overview">
                  {/* `??`, not `||`, for selectedVersion: '' is the valid selected version for
                      versionless formats (raw), and is falsy.

                      selectedRepository/repoRows come from the per-version sources, not from
                      detail.repositories or detail.versions — those were filled by the aggregate
                      walk this ticket removed and are now permanently empty. Sharing repoRows with
                      the Repositories tab keeps the two views in agreement. */}
                  <GAOverviewTab
                    detail={detail}
                    selectedVersion={selectedVersion ?? newestVersion ?? null}
                    selectedRepository={selectedRepository}
                    repoRows={repoRows}
                    versionLastUpdated={versionLastUpdated}
                  />
                </Tabs.Content>

                <Tabs.Content value="versions">
                  <GAVersionsTab
                    versions={versionsState.versions}
                    selectedVersion={effectiveVersion}
                    onSelectVersion={handleVersionSelect}
                    total={versionsState.total}
                    totalPages={versionsState.totalPages}
                    currentPage={versionsState.currentPage}
                    itemsPerPage={versionsState.itemsPerPage}
                    sortKey={versionsState.sortKey}
                    sortDirection={versionsState.sortDirection}
                    searchQuery={versionsState.searchQuery}
                    loading={versionsState.loading}
                    error={versionsState.error}
                    onPageChange={versionsState.onPageChange}
                    onItemsPerPageChange={versionsState.onItemsPerPageChange}
                    onSortChange={versionsState.onSortChange}
                    onSearchQueryChange={versionsState.onSearchQueryChange}
                    onRetry={versionsState.retry}
                  />
                </Tabs.Content>

                <Tabs.Content value="repositories">
                  <GARepositoriesTab
                    rows={repoRows}
                    loading={repoLoading}
                    error={repoError}
                    selectedVersion={effectiveVersion}
                  />
                </Tabs.Content>

                <Tabs.Content value="files">
                  {/* `assetsLoading` is the only loading input the Files tab has. The
                      aggregate-drain `loading` flag this used to be ORed with is gone entirely
                      (NEXUS-54201 / 54220) — ORing it in painted a spinner over assets that were
                      already correct. Not to be confused with the sibling `repoLoading` above:
                      that is the Repositories tab's own per-version fetch, unrelated to assets. */}
                  <GAFilesTab
                    assets={filesAssets}
                    selectedVersion={selectedVersion}
                    selectedRepository={selectedRepository}
                    loading={assetsLoading}
                  />
                </Tabs.Content>

                <Tabs.Content value="security">
                  <GASecurityTab
                    gaId={gaId}
                    selectedVersion={selectedVersion}
                    securityData={securityResult.data}
                    securityLoading={securityResult.loading}
                    securityError={securityResult.error}
                    securityStatus={securityResult.status}
                    iqConnected={securityResult.iqConnected}
                    onRefetch={securityResult.refetch}
                  />
                </Tabs.Content>
              </Box>
            </Tabs.Root>
        </Box>
      </Box>
    </ScrollArea>
  );
}

export default GADetailPage;
