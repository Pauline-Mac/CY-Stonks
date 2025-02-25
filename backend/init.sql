CREATE TABLE IF NOT EXISTS users  (
                       user_id SERIAL PRIMARY KEY,
                       username VARCHAR(100) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS portfolios (
                            portfolio_id SERIAL PRIMARY KEY,
                            user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
                            portfolio_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS assets (
                        asset_id SERIAL PRIMARY KEY,
                        portfolio_id INT REFERENCES portfolios(portfolio_id) ON DELETE CASCADE,
                        asset_type VARCHAR(50),  -- e.g., "Stock", "Crypto", "ETF"
                        asset_symbol VARCHAR(50),  -- e.g., "AAPL" for Apple, "BTC" for Bitcoin
                        quantity DECIMAL(20, 8),  -- number of units owned
                        purchase_price DECIMAL(20, 8),  -- the price at which the asset was bought
                        purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        current_value DECIMAL(20, 8)  -- real-time price
);



