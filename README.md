# CY stonks Back 2025 - 2026

MACEIRAS Pauline  CARDENAS TEMIQUEL Donovan  BELLÊTRE Jules


## Summary

## 1 - Setup & Start

## 2 - Features

##      Loginand logout

##      Account management

##      Data visualisation + analyse





## 1 - Setup & start

## Run the project

    docker-compose up --build

### Restart the database

`docker-compose down
docker volume rm cy-stonks_postgres_data
docker-compose up --build`

Start an sbt environnement `sbt`
Start the server `reStart`


## 2 - Features


## Login and logout

Login to a user : 

    curl -XPOST http://localhost:8081/users/login -d '{"username": "Liselott", "password": "cy-stonks"}' -H "Content-Type:application/json" 
if just curl version add `-c cookies.txt` at the end

Logout : 

    curl http://localhost:8081/users/logout
if just curl version add `-b cookies.txt` at the end


## Account management

List all users:

    curl http://localhost:8081/users

Create a user:

    curl -XPOST http://localhost:8081/users -d '{"uuid": "a13d86f3-943c-4207-a4d6-9672d6ece0d8", "username": "Liselott", "password": "cy-stonks", "wallets": [1, 2], "financialInterests": ["afa7c024-e548-4085-b46c-589d3661d41b"]}' -H "Content-Type:application/json"

Get the details of one user:

    curl http://localhost:8081/users/[uuid]

Delete a user:

    curl -XDELETE http://localhost:8081/users/[uuid]

Get connected user information :

    curl http://localhost:8081/users/me
if just curl version add `-b cookies.txt` at the end


## Data visualisation and analyse


Create an asset:

    curl -XPOST http://localhost:8081/assets -d '{"assetId": 1, "portfolioId": 1, "assetType": "Stock", "assetSymbol": "AAPL", "quantity": 10.5, "purchasePrice": 150.25 }' -H "Content-Type:application/json"


Create a portfolio:

    curl -XPOST http://localhost:8081/portfolios -d '{"portfolioId": 1, "userUuid": "a13d86f3-943c-4207-a4d6-9672d6ece0d8", "name": "First portfolio"}' -H "Content-Type:application/json"


Get analysis on a specific asset:

    curl http://localhost:8081/analyse/{symmbol}

Useful Documentation :
https://doc.akka.io/