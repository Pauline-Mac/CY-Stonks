# CY stonks Back 2025 - 2026
MACEIRAS Pauline
CARDENAS TEMIQUEL Donovan

Useful Documentation :
https://doc.akka.io/

## Run the project

Start an sbt environnement `sbt`
Start the server `reStart`

List all users:

    curl http://localhost:8081/users

Create a user:

    curl -XPOST http://localhost:8081/users -d '{"uuid": "a13d86f3-943c-4207-a4d6-9672d6ece0d8", "username": "Liselott", "password": "cy-stonks", "wallets": ["2d74bc0a-22b4-4892-907c-53e247bbb2d6", "b4f13f99-377c-4cd8-9975-7b40aa613074"], "financialInterests": ["afa7c024-e548-4085-b46c-589d3661d41b"]}' -H "Content-Type:application/json"

Get the details of one user:

    curl http://localhost:8081/users/[uuid]
Delete a user:

    curl -XDELETE http://localhost:8081/users/[uuid]


Create an asset:
``curl -XPOST http://localhost:8081/assets -d '{"assetId": 1, "portfolioId": 101, "assetType": "Stock", "assetSymbol": "AAPL", "quantity": 10.5, "purchasePrice": 150.25 }' -H "Content-Type:application/json"``



