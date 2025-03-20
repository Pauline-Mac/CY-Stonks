import cyStonksLogo from 'assets/cy_stonks_logo.png'
import moneyLogo from 'assets/money_logo.png'

export default function SideMenu(): React.ReactElement {
  return (
    <nav className='max-h-screen p-5 flex-1/6 border-r border-black-600 flex flex-col justify-between sticky top-0 overflow-hidden'>
      <div>
        <header className='flex justify-center align-center overflow-hidden'>
          <img src={cyStonksLogo} className="w-full" alt="React logo" />
        </header>
        <main className='my-15 py-5 flex flex-col gap-10'>
          <div className="flex flex-col gap-5">
            <h2 className='ps-7 text-black-500 font-semibold'>General</h2>
            <div className='flex'>
              <img src={moneyLogo} className="w-6" alt="React logo" />
              <p className='ps-2 hover:font-medium hover:text-black-700 cursor-pointer'>Accueil</p>
            </div>
            <div className='flex'>
              <img src={moneyLogo} className="w-6" alt="React logo" />
              <p className='ps-2 hover:font-medium hover:text-black-700 cursor-pointer'>Recherche</p>
            </div>
          </div>
          <div className="flex flex-col gap-5">
            <h2 className='ps-7 text-black-500 font-semibold'>Personnel</h2>
            <div className='flex'>
              <img src={moneyLogo} className="w-6" alt="React logo" />
              <p className='ps-2 hover:font-medium hover:text-black-700 cursor-pointer'>Portefeuille</p>
            </div>
            <div className='flex'>
              <img src={moneyLogo} className="w-6" alt="React logo" />
              <p className='ps-2 hover:font-medium hover:text-black-700 cursor-pointer'>Transaction</p>
            </div>
            <div className='flex'>
              <img src={moneyLogo} className="w-6" alt="React logo" />
              <p className='ps-2 hover:font-medium hover:text-black-700 cursor-pointer'>Notification</p>
            </div>
          </div>
        </main>
      </div>

      <footer className=''>
        <button className='bg-blue-700 w-full'>
          Se connecter
        </button>
      </footer>
    </nav >
  );
}