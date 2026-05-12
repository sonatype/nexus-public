# Agent 2: Apt/Debian Search - Sprint 2

## Your Mission
Build the Apt/Debian package search UI with real API integration.

## API Endpoint
```
GET /service/rest/v1/search?format=apt
```

## Required Files

Create these files in `/search/apt/`:

| File | Purpose |
|------|---------|
| `apt.types.ts` | TypeScript types |
| `AptSearchPage.tsx` | Main page component |
| `AptSearchFilters.tsx` | Filter controls |
| `AptSearchResults.tsx` | Results list |
| `AptResultRow.tsx` | Single result row |
| `AptDetailPage.tsx` | Package detail view |
| `useAptSearch.ts` | Search hook with real API |
| `mockData.ts` | Mock data for dev/testing |
| `AptSearchPage.scss` | Styles |
| `index.ts` | Exports |
| `__tests__/AptSearchPage.test.tsx` | Unit tests |

## Apt-Specific Filters

```typescript
interface AptSearchFilters {
  name?: string;        // Package name
  version?: string;     // Version
  architecture?: string; // amd64, arm64, i386, all
  distribution?: string; // e.g., bullseye, bookworm
  component?: string;   // main, contrib, non-free
  repository?: string;  // Repository name
}
```

## API Parameters

| Filter | API Param |
|--------|-----------|
| name | `q` or `name` |
| version | `version` |
| architecture | `apt.architecture` |
| distribution | `apt.distribution` |
| component | `apt.component` |
| repository | `repository` |

## Reference Implementation
Copy patterns from `/search/pypi/usePyPISearch.ts`:
- `USE_REAL_API = true`
- Axios calls to `/service/rest/v1/search?format=apt`
- Result aggregation by package name

## Apt Result Fields
```typescript
interface AptResult {
  id: string;
  name: string;
  version: string;
  architecture: string;
  distribution?: string;
  component?: string;
  repository: string;
  description?: string;
  maintainer?: string;
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
1. Ensure all tests pass: `yarn jest -- src/search/apt`
2. Ensure build passes: `yarn build`
3. Report completion to Agent 0


