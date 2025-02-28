import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

function App() {
    const [stocks, setStocks] = useState([]);
    const [portfolio, setPortfolio] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [connectionStatus, setConnectionStatus] = useState('Checking connection...');

    // Get the API URL from environment variables or use a default
    const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:9000';

    useEffect(() => {
        // Check backend health
        axios.get(`${apiUrl}/health`)
            .then(response => {
                setConnectionStatus(`Connected to backend: ${response.data.status || 'OK'}`);

                // Load stock data
                return axios.get(`${apiUrl}/api/data`);
            })
            .then(response => {
                setStocks(response.data);

                // Load portfolio data
                return axios.get(`${apiUrl}/api/portfolio`);
            })
            .then(response => {
                setPortfolio(response.data.assets);
                setLoading(false);
            })
            .catch(err => {
                console.error('Error:', err);
                if (err.code === 'ERR_NETWORK') {
                    setConnectionStatus('Cannot connect to backend. Is the server running?');
                } else {
                    setConnectionStatus(`Connected but endpoint failed: ${err.response?.status || 'Unknown error'}`);
                }
                setError(`Error: ${err.message}`);
                setLoading(false);
            });
    }, [apiUrl]);

    return (
        <div className="App">
            <header className="App-header">
                <h1>CY Stonks Dashboard</h1>
                <p className="connection-status">{connectionStatus}</p>
            </header>

            <main>
                {loading ? (
                    <div className="loading">Loading data...</div>
                ) : error ? (
                    <div className="error">
                        <p>{error}</p>
                        <p>Make sure your backend is running properly.</p>
                    </div>
                ) : (
                    <div className="dashboard">
                        <div className="card">
                            <h2>Market Data</h2>
                            {stocks.length > 0 ? (
                                <table className="stock-table">
                                    <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th>Price</th>
                                        <th>Change</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {stocks.map((stock) => (
                                        <tr key={stock.id}>
                                            <td>{stock.symbol}</td>
                                            <td>${stock.price.toFixed(2)}</td>
                                            <td className={stock.change >= 0 ? "positive" : "negative"}>
                                                {stock.change >= 0 ? "+" : ""}{stock.change.toFixed(2)}%
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            ) : (
                                <p>No market data available</p>
                            )}
                        </div>

                        <div className="card">
                            <h2>Your Portfolio</h2>
                            {portfolio.length > 0 ? (
                                <table className="portfolio-table">
                                    <thead>
                                    <tr>
                                        <th>Symbol</th>
                                        <th>Quantity</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {portfolio.map((asset, index) => (
                                        <tr key={index}>
                                            <td>{asset.symbol}</td>
                                            <td>{asset.quantity}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            ) : (
                                <p>Your portfolio is empty</p>
                            )}
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}

export default App;