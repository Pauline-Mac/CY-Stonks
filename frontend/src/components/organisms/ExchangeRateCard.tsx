
import Chart from 'src/components/molecules/Chart';
import { AssetsData } from 'src/components/pages/LandingPage';

export interface ExchangeRateCardProps {
  assets: AssetsData[]
}

export default function ExchangeRateCard(props: ExchangeRateCardProps): React.ReactElement {
  const series = props.assets.map((asset) => ({
    x: asset.historicalPrices.map((price) => price.date),
    y: asset.historicalPrices.map((price) => price.price)
  }));

  const chartProps = {
    series: series,
    name: props.assets.map((asset) => asset.symbol)
  }

  return (
    <main className='flex-1/2 flex flex-col gap-2' >
      <div className='flex justify-between'>
        <h2 className='text-m font-semibold'>Evolution du prix par jour</h2>
      </div>
      <div className='h-full bg-gray-200 rounded-lg flex-1 flex justify-between align-center'>
        <Chart {...chartProps} />
      </div>
    </main >
  );
}