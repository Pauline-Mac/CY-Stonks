CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
                                     user_id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                     username VARCHAR(100) UNIQUE NOT NULL,
                                     password_hash VARCHAR(255) NOT NULL,
                                     portfolio_ids INTEGER[] DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS portfolios (
                                          portfolio_id SERIAL PRIMARY KEY,
                                          user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
                                          portfolio_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS assets (
                                      asset_id SERIAL PRIMARY KEY,
                                      portfolio_id INT REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
                                      asset_type VARCHAR(50),  -- e.g., "Stock", "Crypto", "ETF"
                                      asset_symbol VARCHAR(50),  -- e.g., "AAPL" for Apple, "BTC" for Bitcoin
                                      quantity DECIMAL(20, 8),
                                      purchase_price DECIMAL(20, 8)  -- the price at which the asset was bought
);

CREATE TABLE IF NOT EXISTS market_data (
                                           id SERIAL PRIMARY KEY,
                                           asset_symbol VARCHAR(20) NOT NULL,
                                           price DOUBLE PRECISION NOT NULL,
                                           timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_market_data_symbol_timestamp ON market_data(asset_symbol, timestamp DESC);

-- Trigger to update portfolio_ids when a portfolio is added
CREATE OR REPLACE FUNCTION add_portfolio_to_user()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE users
    SET portfolio_ids = array_append(portfolio_ids, NEW.portfolio_id)
    WHERE user_id = NEW.user_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_add_portfolio
    AFTER INSERT ON portfolios
    FOR EACH ROW
EXECUTE FUNCTION add_portfolio_to_user();

-- Trigger to update portfolio_ids when a portfolio is deleted
CREATE OR REPLACE FUNCTION remove_portfolio_from_user()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE users
    SET portfolio_ids = array_remove(portfolio_ids, OLD.portfolio_id)
    WHERE user_id = OLD.user_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_remove_portfolio
    AFTER DELETE ON portfolios
    FOR EACH ROW
EXECUTE FUNCTION remove_portfolio_from_user();
