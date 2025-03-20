package com.cystonks.models

import scala.collection.immutable
import java.util.UUID

final case class Asset(assetId: Int, portfolioId: Int, assetType: String,           // Type de l'actif (e.g., "Stock", "Crypto", "ETF")
                        assetSymbol: String,         // Symbole de l'actif (e.g., "AAPL", "BTC")
                        quantity: BigDecimal, purchasePrice: BigDecimal    // Prix d'achat de l'actif
                      )
final case class Assets(assets: immutable.Seq[Asset])
