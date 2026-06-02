# Contract: Provider Adapters

## PricingProviderAdapter

Purpose: Normalize pricing data from TCGPlayer, eBay, PriceCharting, mock
providers, and manual entry before valuation.

Input:
- Item type: CARD or SEALED_PRODUCT
- Catalog item id
- Language market
- Graded status
- PSA grade when applicable
- Search/provider reference metadata

Output:
- Provider name
- Provider result type: market price, sold listing, active listing, historical
  fallback, manual entry
- Source price
- Source currency
- Source timestamp or observed timestamp
- Listing count when available
- Recent sales count when available
- Source URL/reference when allowed
- Provider confidence metadata
- Fetch status and error code when unavailable

Rules:
- Adapters must not calculate final market value.
- Adapters must not convert final display values.
- Adapters must be mockable.
- Provider failure returns an unavailable result or handled exception metadata,
  not a business-service crash.

## ExchangeRateProviderAdapter

Purpose: Provide auditable currency conversion inputs for SGD display.

Input:
- Base currency
- Target currency: SGD
- Effective date/time preference

Output:
- Rate
- Source/provenance
- Effective timestamp
- Fetched timestamp
- Confidence/status

Rules:
- Missing exchange rate prevents unaudited final SGD display.
- Exchange-rate data is stored as a snapshot.

## GradingMetadataProviderAdapter

Purpose: Refresh PSA fee/turnaround or related metadata when a permitted source
exists.

Input:
- Grading company: PSA
- Service level
- Effective date

Output:
- Fee amount
- Fee currency
- Turnaround estimate
- Source/provenance
- Effective date range

Rules:
- PSA fees remain updateable data.
- No hardcoded permanent fee values.

