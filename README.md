# Sample Akka HTTP server

This is a sample Akka HTTP endpoint keeping an in-memory database of users that can be created and listed.

Sources in the sample:

* `QuickstartApp.scala` -- contains the main method which bootstraps the application
* `UserRoutes.scala` -- Akka HTTP `routes` defining exposed endpoints
* `UserRegistry.scala` -- the actor which handles the registration requests
* `JsonFormats.scala` -- converts the JSON data from requests into Scala types and from Scala types into JSON responses

## Interacting with the sample

After starting the sample with `sbt run` the following requests can be made:

List all users:

    curl http://localhost:8080/users

Create a user:

    curl -XPOST http://localhost:8080/users -d '{"uuid": "a13d86f3-943c-4207-a4d6-9672d6ece0d8", "username": "Liselott", "password": "cy-stonks", "wallets": ["2d74bc0a-22b4-4892-907c-53e247bbb2d6", "b4f13f99-377c-4cd8-9975-7b40aa613074"], "financialInterests": ["afa7c024-e548-4085-b46c-589d3661d41b"]}' -H "Content-Type:application/json"

Get the details of one user:

    curl http://localhost:8080/users/Liselott

Delete a user:

    curl -XDELETE http://localhost:8080/users/Liselott

# Object means Singleton