ALTER TABLE portfolio_valuation_snapshot
    ADD COLUMN realized_gain_loss_sgd NUMERIC(19, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE portfolio_valuation_snapshot
    ADD COLUMN realized_gain_loss_percent NUMERIC(9, 4) NOT NULL DEFAULT 0.0000;

ALTER TABLE portfolio_valuation_snapshot
    ADD COLUMN realized_cost_basis_sgd NUMERIC(19, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE portfolio_valuation_snapshot
    ADD COLUMN total_performance_sgd NUMERIC(19, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE portfolio_valuation_snapshot
    ADD COLUMN total_performance_percent NUMERIC(9, 4) NOT NULL DEFAULT 0.0000;
