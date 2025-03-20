import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import cyStonksLogo from 'assets/cy_stonks_logo.png';
import moneyLogo from 'assets/money_logo.png';

export default function SideMenu(): React.ReactElement {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const handleLogout = () => {
    localStorage.removeItem("token");
    setIsLoggedIn(false);
    navigate("/login");
  };

  useEffect(() => {
    const checkLoginStatus = () => {
      const token = localStorage.getItem("token");
      setIsLoggedIn(!!token);
    };

    checkLoginStatus();

    const intervalId = setInterval(checkLoginStatus, 1000);

    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === "token" || e.key === null) {
        checkLoginStatus();
      }
    };

    window.addEventListener('storage', handleStorageChange);

    // Cleanup
    return () => {
      clearInterval(intervalId);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  return (
      <nav className="max-h-screen p-5 flex-1/6 border-r border-black-600 flex flex-col justify-between sticky top-0 overflow-hidden">
        <div>
          <header className="flex justify-center align-center overflow-hidden">
            <img src={cyStonksLogo} className="w-full" alt="Cy Stonks Logo" />
          </header>
          <main className="my-15 py-5 flex flex-col gap-10">
            <div className="flex flex-col gap-5">
              <h2 className="ps-7 text-black-500 font-semibold">General</h2>
              <div className="flex cursor-pointer" onClick={() => navigate("/")}>
                <img src={moneyLogo} className="w-6" alt="Accueil" />
                <p className="ps-2 hover:font-medium hover:text-black-700">Accueil</p>
              </div>
              <div className="flex cursor-pointer" onClick={() => navigate("/analyse")}>
                <img src={moneyLogo} className="w-6" alt="Analyse" />
                <p className="ps-2 hover:font-medium hover:text-black-700">Analyse</p>
              </div>
            </div>
            <div className="flex flex-col gap-5">
              <h2 className="ps-7 text-black-500 font-semibold">Personnel</h2>
              <div className="flex cursor-pointer" onClick={() => navigate("/portefeuille")}>
                <img src={moneyLogo} className="w-6" alt="Portefeuille" />
                <p className="ps-2 hover:font-medium hover:text-black-700">Portefeuille</p>
              </div>
              <div className="flex cursor-pointer" onClick={() => navigate("/transaction")}>
                <img src={moneyLogo} className="w-6" alt="Transaction" />
                <p className="ps-2 hover:font-medium hover:text-black-700">Transaction</p>
              </div>
              <div className="flex cursor-pointer" onClick={() => navigate("/notifications")}>
                <img src={moneyLogo} className="w-6" alt="Notification" />
                <p className="ps-2 hover:font-medium hover:text-black-700">Notification</p>
              </div>
            </div>
          </main>
        </div>
        <footer>
          {isLoggedIn ? (
              <button className="bg-red-600 text-white p-2 rounded-lg w-full hover:bg-red-500" onClick={handleLogout}>
                Se déconnecter
              </button>
          ) : (
              <button className="bg-blue-700 text-white p-2 rounded-lg w-full hover:bg-blue-600" onClick={() => navigate("/login")}>
                Se connecter
              </button>
          )}
        </footer>
      </nav>
  );
}