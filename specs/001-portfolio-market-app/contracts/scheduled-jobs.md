# Contract: Scheduled Jobs

## Daily Price Refresh

Trigger: Daily schedule and optional owner/admin manual run.

Inputs:
- Enabled provider adapters
- Active catalog items or portfolio-relevant items
- Latest exchange-rate snapshots or exchange-rate refresh result

Outputs:
- Pricing provider results
- Calculated price snapshots
- Job run metadata

Rules:
- Never overwrite price history.
- Continue when one provider fails and other usable providers remain.
- Lower confidence when source availability or quality is poor.

## Daily Portfolio Valuation Snapshot

Trigger: Daily schedule and optional owner/admin manual run.

Inputs:
- Owned items
- Latest calculated market values

Outputs:
- Portfolio valuation snapshot
- Job run metadata

Rules:
- Use latest calculated market value.
- Preserve historical valuation snapshots.
- Surface low-confidence valuation count.

## Daily Alert Check

Trigger: Daily schedule after valuation refresh and optional owner/admin manual
run.

Inputs:
- Owned items
- Latest price snapshots
- Existing alerts

Outputs:
- New alert records
- Job run metadata

Rules:
- Below SGD 100 purchase price: alert at gain of at least SGD 10.
- SGD 100 or above purchase price: alert at gain of at least SGD 25.
- Deduplicate alerts on rerun.

## Exchange Rate Refresh

Trigger: Daily schedule and on-demand before valuation if no usable rate exists.

Outputs:
- Exchange-rate snapshots
- Job run metadata

Rules:
- Store source/provenance and effective timestamp.
- Do not display unaudited final converted values without a usable rate.

## PSA Fee / Turnaround Refresh

Trigger: Schedule if a permitted provider exists, otherwise owner/admin manual
update.

Outputs:
- Updateable grading fee records
- Job run metadata

Rules:
- PSA only for v1.
- Preserve effective-date history where practical.

