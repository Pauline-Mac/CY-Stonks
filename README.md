<h1>CY Stonks Back 2025 - 2026</h1>
<p><strong>MACEIRAS Pauline, CARDENAS TEMIQUEL Donovan, BELLÊTRE Jules</strong></p>

<h2>Summary</h2>
<p>The <strong>CY Stonks Back</strong> project is a platform for managing and analyzing financial assets in a portfolio. The application includes user account management, asset analysis, and a secure login/logout system.</p>

<h2>1 - Setup & Start</h2>

<h3>Running the Project</h3>
<pre>docker-compose up --build</pre>

<h3>Restarting the Database</h3>
<pre>docker-compose down
docker volume rm cy-stonks_postgres_data
docker-compose up --build</pre>

<h3>SBT Environment</h3>
<p>To access the SBT environment and start the server:</p>
<pre>sbt</pre>
<p>Then, start the server with:</p>
<pre>reStart</pre>

<h2>2 - Features</h2>

<h3>Login and Logout</h3>

<h4>User Login:</h4>
<pre>curl -XPOST http://localhost:8081/users/login -d '{"username": "Liselott", "password": "cy-stonks"}' -H "Content-Type:application/json"</pre>
<p><strong>Note:</strong> If using a basic curl version, add <code>-c cookies.txt</code> at the end.</p>

<h4>Logout:</h4>
<pre>curl http://localhost:8081/users/logout</pre>
<p><strong>Note:</strong> If using a basic curl version, add <code>-b cookies.txt</code> at the end.</p>

<h3>User Account Management</h3>

<h4>List all users:</h4>
<pre>curl http://localhost:8081/users</pre>

<h4>Create a user:</h4>
<pre>curl -XPOST http://localhost:8081/users -d '{"uuid": "a13d86f3-943c-4207-a4d6-9672d6ece0d8", "username": "Liselott", "password": "cy-stonks", "wallets": [1, 2], "financialInterests": ["afa7c024-e548-4085-b46c-589d3661d41b"]}' -H "Content-Type:application/json"</pre>

<h4>Retrieve details of a specific user:</h4>
<pre>curl http://localhost:8081/users/[uuid]</pre>

<h4>Delete a user:</h4>
<pre>curl -XDELETE http://localhost:8081/users/[uuid]</pre>

<h4>Get information about the currently logged-in user:</h4>
<pre>curl http://localhost:8081/users/me</pre>
<p><strong>Note:</strong> If using a basic curl version, add <code>-b cookies.txt</code> at the end.</p>

<h3>Data Visualization and Analysis</h3>

<h4>Create an asset:</h4>
<pre>curl -XPOST http://localhost:8081/assets -d '{"assetId": 1, "portfolioId": 1, "assetType": "Stock", "assetSymbol": "AAPL", "quantity": 10.5, "purchasePrice": 150.25 }' -H "Content-Type:application/json"</pre>

<h4>Create a portfolio:</h4>
<pre>curl -XPOST http://localhost:8081/portfolios -d '{"portfolioId": 1, "userUuid": "a13d86f3-943c-4207-a4d6-9672d6ece0d8", "name": "First portfolio"}' -H "Content-Type:application/json"</pre>

<h4>Get analysis on a specific asset:</h4>
<pre>curl http://localhost:8081/analyse/{symbol}</pre>

<h2>Conclusion</h2>
<p>The <strong>CY Stonks Back</strong> project provides a robust infrastructure for managing and analyzing financial portfolios. Its integration with REST services ensures ease of use, and its numerous features cover a wide range of applications for asset management.</p>
