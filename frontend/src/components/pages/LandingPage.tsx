import React, { } from 'react'
import SideMenu from 'src/components/organisms/SideMenu'
import { useEffect, useState } from "react";
import LoadingScreen from 'components/pages/LoadingScreen';
import ErrorScreen from 'components/pages/ErrorScreen';
import ExchangeRateCard, { ExchangeRateCardProps } from 'src/components/organisms/ExchangeRateCard';
import AssetsInformationCards, { AssetsInformationCardsProps } from '../organisms/AssetsInformationCards';
import MainTitle from 'components/organisms/MainTitle';

interface LandingPageProps {
  demo: boolean;
}

export default function App(props: LandingPageProps): React.ReactElement {
  const [assetsData, setAssetData] = useState<AssetsData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const queryParams = new URLSearchParams(window.location.search);

  const symbol = queryParams.get('symbol') || 'demo';
  const backendUrl = `http://localhost:8081/analyse/${symbol}`;
  useEffect(() => {
    fetch(backendUrl)
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
        setError(error.message);
      });
  }, []);

  if (error && !props.demo) {
    return (
      <div>
        <ErrorScreen message={"Something went wrong fetching the data..."} />
      </div>
    );
  }

  if (!assetsData && !props.demo) {
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
      assetsData || assetsMockData,
    ]
  }

  const AssetsInformationCardsProps: AssetsInformationCardsProps = props.demo ? assetsDataMockedProps : assetsDataProdProps;
  const exchangeRateCardProps: ExchangeRateCardProps = props.demo ? assetsDataMockedProps : assetsDataProdProps;

  return (
    <div className='size-full flex flex-row overflow-x-hidden'>
      <div className='max-h-screen p-5 flex-1/6 flex flex-col justify-between sticky top-0'>
        <SideMenu />
      </div>
      <main className='flex-5/6 py-5 px-10'>
        <div className='h-full flex flex-col gap-8'>
          <MainTitle />
          <hr className='text-gray-300' />
          <div className='flex flex-row gap-5 justify-center'>
            <div className='size-7/10'>
              <ExchangeRateCard {...exchangeRateCardProps} />
            </div>
          </div>
          <hr className='text-gray-300' />
          <div className='flex flex-row justify-center'>
            <AssetsInformationCards {...AssetsInformationCardsProps} />
          </div>
          <div className='font-semibold text-center'>CY Stonks 💰</div>
        </div>
      </main>
    </div>
  );
}

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