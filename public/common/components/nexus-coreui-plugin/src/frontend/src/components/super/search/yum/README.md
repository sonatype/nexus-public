# Agent 1: Yum/RPM Search - Sprint 2

## Your Mission
Build the Yum/RPM package search UI with real API integration.

## API Endpoint
```
GET /service/rest/v1/search?format=yum
```

## Required Files

Create these files in `/search/yum/`:

| File | Purpose |
|------|---------|
| `yum.types.ts` | TypeScript types |
| `YumSearchPage.tsx` | Main page component |
| `YumSearchFilters.tsx` | Filter controls |
| `YumSearchResults.tsx` | Results list |
| `YumResultRow.tsx` | Single result row |
| `YumDetailPage.tsx` | Package detail view |
| `useYumSearch.ts` | Search hook with real API |
| `mockData.ts` | Mock data for dev/testing |
| `YumSearchPage.scss` | Styles |
| `index.ts` | Exports |
| `__tests__/YumSearchPage.test.tsx` | Unit tests |

## Yum-Specific Filters

```typescript
interface YumSearchFilters {
  name?: string;        // Package name (q parameter)
  version?: string;     // Version
  architecture?: string; // x86_64, noarch, i686
  repository?: string;  // Repository name
}
```

## API Parameters

| Filter | API Param |
|--------|-----------|
| name | `q` or `name` |
| version | `version` |
| architecture | `yum.architecture` |
| repository | `repository` |

## Reference Implementation
Copy patterns from `/search/pypi/usePyPISearch.ts`:
- `USE_REAL_API = true`
- Axios calls to `/service/rest/v1/search?format=yum`
- Result aggregation by package name

## Yum Result Fields
```typescript
interface YumResult {
  id: string;
  name: string;
  version: string;
  release: string;
  architecture: string;
  repository: string;
  summary?: string;
  description?: string;
  downloadUrl?: string;
}
```

## Test Requirements
- Test page renders
- Test filters work
- Test search execution
- Test result display
- Mock Axios in tests

## When Done
1. Ensure all tests pass: `yarn jest -- src/search/yum`
2. Ensure build passes: `yarn build`
3. Report completion to Agent 0


