import { AssetsData } from "components/pages/LandingPage";

export interface AssetsInformationCardsProps {
  assets: AssetsData[];
}

export default function AssetsInformationCards(props: AssetsInformationCardsProps): React.ReactElement {
  return (
    <div className='flex-1/2 flex flex-col'>
      <h2 className='text-xl text-center font-semibold'>Informations financiaires</h2>
      <br />
      <main>
        <div className='size-full flex flex-row gap-20'>
          {props.assets.map((asset) => (
            <div key={asset.symbol} className='p-3 h-full w-full flex flex-col gap-2 inset-shadow-sm shadow-xl rounded-lg'>
              <h2 className='text-xl font-bold text-center'>{asset.symbol}</h2>
              <ul>
                <li className='font-medium my-2'>Asset Type: {asset.assetType}</li>
                <li className='font-medium my-2'>Current Price: {asset.currentPrice}</li>
                <div className='flex justify-around'>
                  <li>Price Change: {asset.priceChange}</li>
                  <li>Change Percent: {asset.priceChangePercent}</li>
                </div>
                <li className='font-medium my-2'>Recommendation: {asset.recommendation}</li>
                <li className='font-medium my-2'>Signals:</li>
                <div className='flex justify-around'>
                  <li>Short Term: {asset.signals.shortTerm}</li>
                  <li>Medium Term: {asset.signals.mediumTerm}</li>
                </div>
              </ul>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
