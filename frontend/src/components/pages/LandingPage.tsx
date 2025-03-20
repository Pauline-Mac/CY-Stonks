import React, { } from 'react'
import SideMenu from 'src/components/organisms/SideMenu'
import ExchangeRateCard, { ExchangeRateCardProps } from 'src/components/organisms/ExchangeRateCard'
import { useEffect, useState } from "react";
import LoadingScreen from 'components/pages/LoadingScreen';

export interface AssetsData {
  "symbol": string,
  "assetType": string,
  "lastRefreshed": string,
  "currentPrice": number,
  "priceChange": number,
  "priceChangePercent": number,
  "historicalPrices": {
    "date": string,
    "price": number
  }[],
  "technicalAnalysis": {
    "rsi": {
      "value": number,
      "interpretation": string,
      "thresholds": {
        "oversold": number,
        "neutral": number,
        "overbought": number
      }
    },
    "macd": {
      "value": number,
      "interpretation": string,
      "thresholds": {
        "signal": number,
        "histogram": number
      }
    },
    "volatility": {
      "value": number,
      "interpretation": string,
      "thresholds": {
        "low": number,
        "medium": number,
        "high": number
      }
    }
  },
  "recommendation": string,
  "signals": {
    "shortTerm": string,
    "mediumTerm": string,
    "overallTrend": string
  }
}


export function MainTitle(): React.ReactElement {
  const [username, setUsername] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("token");

    if (token) {
      fetch("http://localhost:8081/users/me", {
        method: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        credentials: "include",
      })
          .then((response) => {
            if (!response.ok) {
              throw new Error("User not authenticated");
            }
            return response.json();
          })
          .then((data) => setUsername(data.username))
          .catch(() => setUsername(null));
    }
  }, []);

  return (
      <header className="flex justify-between items-center">
        <h2 className="text-2xl font-semibold italic">
          Suivez, Analysez et Faites Croître Votre Patrimoine
        </h2>
        <div className="flex items-center gap-5">
          <input type="text" className="outline-none border p-2 rounded" placeholder="🕵️‍♀️ Chercher en bourse" />

          {username ? (
              <span className="text-lg font-medium">👤 {username}</span>
          ) : (
              <a href="/login" className="text-blue-500 hover:underline">Se connecter</a>
          )}
        </div>
      </header>
  );
}


function TrendingAssetsCard(): React.ReactElement {
  return (
    <main className='flex-1/2 flex flex-col gap-2'>
      <div className='flex justify-between'>
        <h2 className='text-m font-semibold'>Tendance</h2>
        <p className='text-m text-gray-500'>Tout voir →</p>
      </div>
      <div className='h-full bg-gray-200 rounded-lg'></div>
    </main>
  );
}

function MostProfitableAssetsCard(): React.ReactElement {
  return (
    <main className='flex-1/2 flex flex-col gap-2'>
      <div className='flex justify-between'>
        <h2 className='text-m font-semibold'>Plus profitables</h2>
        <p className='text-m text-gray-500'>Tout voir →</p>
      </div>
      <div className='h-full flex flex-row gap-2'>
        <div className='h-full w-full bg-gray-200 rounded-lg'></div>
        <div className='h-full w-full bg-gray-200 rounded-lg'></div>
        <div className='h-full w-full bg-gray-200 rounded-lg'></div>
        <div className='h-full w-full bg-gray-200 rounded-lg'></div>
        <div className='h-full w-full bg-gray-200 rounded-lg'></div>
      </div>
    </main>
  );
}

function NewsCard(): React.ReactElement {
  return (
    <main className='h-full flex flex-col gap-2'>
      <h2 className='text-m font-semibold'>News</h2>
      <div className='flex-1/4 grow bg-gray-200 rounded-lg'></div>
      <div className='flex-1/4 bg-gray-200 rounded-lg'></div>
      <div className='flex-1/4 bg-gray-200 rounded-lg'></div>
      <div className='flex-1/4 bg-gray-200 rounded-lg'></div>
    </main>
  );
}

function PortfolioCard(): React.ReactElement {
  return (
    <main className='flex-1/2 flex flex-col gap-2'>
      <div className='flex justify-between'>
        <h2 className='text-m text-m font-semibold'>Mon portefeuille</h2>
        <p className='text-m text-gray-500'>Tout plus →</p>
      </div>
      <div className='h-full flex gap-5'>
        <div className='size-full bg-gray-200 rounded-lg flex-1'></div>
        <div className='size-full bg-gray-200 rounded-lg flex-1'></div>
      </div>
    </main>
  );
}

const assetsMockData = {
  "symbol": "AAPL",
  "assetType": "Stock",
  "lastRefreshed": "2023-10-01",
  "currentPrice": 150.25,
  "priceChange": 1.5,
  "priceChangePercent": 1.01,
  "historicalPrices": [
    {
      "date": "2023-09-30",
      "price": 148.75
    },
    {
      "date": "2023-09-29",
      "price": 200.0
    },
    {
      "date": "2023-09-28",
      "price": 180
    },
    {
      "date": "2023-09-27",
      "price": 100
    },
    {
      "date": "2023-09-26",
      "price": 50
    },
    // More historical prices...
  ],
  "technicalAnalysis": {
    "rsi": {
      "value": 55.5,
      "interpretation": "Bullish momentum - trend likely continuing upward",
      "thresholds": {
        "oversold": 30.0,
        "neutral": 50.0,
        "overbought": 70.0
      }
    },
    "macd": {
      "value": 0.25,
      "interpretation": "Bullish - MACD above signal line",
      "thresholds": {
        "signal": 0.2,
        "histogram": 0.05
      }
    },
    "volatility": {
      "value": 0.015,
      "interpretation": "Moderate - typical market volatility",
      "thresholds": {
        "low": 0.005,
        "medium": 0.015,
        "high": 0.025
      }
    }
  },
  "recommendation": "Buy - Technical indicators suggest favorable entry point",
  "signals": {
    "shortTerm": "Buy",
    "mediumTerm": "Hold",
    "overallTrend": "Bullish"
  }
}

const assetsMockData2 = {
  "symbol": "DOGE",
  "assetType": "Stock",
  "lastRefreshed": "2023-10-01",
  "currentPrice": 150.25,
  "priceChange": 1.5,
  "priceChangePercent": 1.01,
  "historicalPrices": [
    {
      "date": "2023-09-30",
      "price": 50.25
    },
    {
      "date": "2023-09-29",
      "price": 10.0
    },
    {
      "date": "2023-09-28",
      "price": 100
    },
    {
      "date": "2023-09-27",
      "price": 70
    },
    {
      "date": "2023-09-26",
      "price": 40
    },
    // More historical prices...
  ],
  "technicalAnalysis": {
    "rsi": {
      "value": 55.5,
      "interpretation": "Bullish momentum - trend likely continuing upward",
      "thresholds": {
        "oversold": 30.0,
        "neutral": 50.0,
        "overbought": 70.0
      }
    },
    "macd": {
      "value": 0.25,
      "interpretation": "Bullish - MACD above signal line",
      "thresholds": {
        "signal": 0.2,
        "histogram": 0.05
      }
    },
    "volatility": {
      "value": 0.015,
      "interpretation": "Moderate - typical market volatility",
      "thresholds": {
        "low": 0.005,
        "medium": 0.015,
        "high": 0.025
      }
    }
  },
  "recommendation": "Buy - Technical indicators suggest favorable entry point",
  "signals": {
    "shortTerm": "Buy",
    "mediumTerm": "Hold",
    "overallTrend": "Bullish"
  }
}

interface LandingPageProps {
  demo: boolean;
}

export default function App(props: LandingPageProps): React.ReactElement {
  const [assetsData, setAssetData] = useState<AssetsData | null>(null);

  useEffect(() => {
    fetch("http://localhost:8081/analyse/ibm")
      .then((response) => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.json();
      })
      .then((response) => response as unknown as AssetsData)
      .then((response) => setAssetData(response))
      .catch((error) => {
        console.error('There was a problem with the fetch operation:', error);
      });
  }, []);

  console.log(assetsData);

  if (!assetsData) {
    return (
      <div>
        <LoadingScreen />
      </div>
    );
  }

  const assetsDataMockedProps = {
    assets: [
      assetsMockData,
      assetsMockData2
    ]
  }

  const assetsDataProdProps = {
    assets: [
      assetsData,
    ]
  }

  const ExchangeRateCardProps: ExchangeRateCardProps = props.demo ? assetsDataMockedProps : assetsDataProdProps;
  return (
    <div className='min-h-screen flex flex-row overflow-x-hidden'>
      <SideMenu />
      <main className='flex-5/6 py-5 px-10'>
        <div className='h-full flex flex-col gap-5'>
          <MainTitle />
          <div className='flex-1/2 flex flex-row gap-5'>
            <div className='flex-1/2 flex flex-col gap-2'>
              <TrendingAssetsCard />
              <MostProfitableAssetsCard />
            </div>
            <div className='flex-1/2'>
              <NewsCard />
            </div>
          </div>
          <div className='flex-1/2 flex flex-row gap-5'>
            <ExchangeRateCard {...ExchangeRateCardProps} />
            <PortfolioCard />
          </div>
        </div >
      </main >
    </div >
  );
}