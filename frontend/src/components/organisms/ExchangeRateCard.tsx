
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
    <main className='flex flex-col gap-2' >
      <h2 className='text-xl text-center font-semibold'>Evolution du prix par jour</h2>
      <div className=''>
        <Chart {...chartProps} />
      </div>
    </main >
  );
}