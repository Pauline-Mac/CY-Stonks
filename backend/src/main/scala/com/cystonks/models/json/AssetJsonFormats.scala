package com.cystonks.models.json

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import com.cystonks.models.{Asset, Assets}
import com.cystonks.actors.asset.AssetRegistry

object AssetJsonFormats extends DefaultJsonProtocol {
  implicit val assetJsonFormat: RootJsonFormat[Asset] = jsonFormat6(Asset)
  implicit val assetsJsonFormat: RootJsonFormat[Assets] = jsonFormat1(Assets)
  implicit val actionPerformedJsonFormat: RootJsonFormat[AssetRegistry.ActionPerformed] = jsonFormat1(AssetRegistry.ActionPerformed)
}
