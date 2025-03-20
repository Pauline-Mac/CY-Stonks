import React, { useState } from 'react';
import SideMenu from 'src/components/organisms/SideMenu';
import MainTitle from 'components/organisms/MainTitle';

interface Portfolio {
    id: string;
    name: string;
}

export default function PortfolioPage(): React.ReactElement {
    const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
    const [selectedPortfolio, setSelectedPortfolio] = useState<string>('');
    const [newPortfolioName, setNewPortfolioName] = useState<string>('');
    const [assetName, setAssetName] = useState<string>('');
    const [volume, setVolume] = useState<number>(0);

    const handleCreatePortfolio = async () => {
        if (!newPortfolioName.trim()) return;

        const userUuid = localStorage.getItem('userUuid');
        if (!userUuid) {
            alert('UUID utilisateur non trouvé dans le localStorage.');
            return;
        }

        const newPortfolio = {
            portfolioId: Date.now(),
            userUuid: userUuid,
            name: newPortfolioName
        };

        try {
            const response = await fetch('http://localhost:8081/portfolios', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(newPortfolio)
            });

            if (response.ok) {
                setPortfolios([...portfolios, { id: newPortfolio.portfolioId.toString(), name: newPortfolioName }]);
                setNewPortfolioName('');
            } else {
                alert('Erreur lors de la création du portefeuille.');
            }
        } catch (error) {
            console.error('Erreur:', error);
            alert('Une erreur est survenue lors de la création du portefeuille.');
        }
    };

    const handleAssetPurchase = () => {
        if (!selectedPortfolio || !assetName.trim() || volume <= 0) {
            alert('Veuillez remplir tous les champs correctement.');
            return;
        }
        console.log(`Achat de ${volume} de ${assetName} pour le portefeuille ${selectedPortfolio}`);
    };

    return (
        <div className='size-full flex flex-row overflow-x-hidden'>
            {/* Sidebar */}
            <div className='max-h-screen p-5 flex-1/6 flex flex-col justify-between sticky top-0'>
                <SideMenu />
            </div>

            {/* Main Content */}
            <main className='flex-5/6 py-5 px-10'>
                <div className='h-full flex flex-col gap-8'>
                    <MainTitle />
                    <hr className='text-gray-300' />

                    {/* Formulaire de création de portefeuille */}
                    <div className='flex flex-col gap-4'>
                        <h3 className='text-xl font-semibold'>Créer un portefeuille</h3>
                        <input
                            type='text'
                            value={newPortfolioName}
                            onChange={(e) => setNewPortfolioName(e.target.value)}
                            placeholder='Nom du portefeuille'
                            className='border p-2 rounded'
                        />
                        <button
                            onClick={handleCreatePortfolio}
                            className='bg-blue-500 text-white p-2 rounded hover:bg-blue-600'
                        >
                            Créer le portefeuille
                        </button>
                    </div>

                    {/* Formulaire d'achat d'asset */}
                    <div className='flex flex-col gap-4'>
                        <h3 className='text-xl font-semibold'>Acheter un asset</h3>
                        <select
                            value={selectedPortfolio}
                            onChange={(e) => setSelectedPortfolio(e.target.value)}
                            className='border p-2 rounded'
                        >
                            <option value=''>Sélectionner un portefeuille</option>
                            {portfolios.map((portfolio) => (
                                <option key={portfolio.id} value={portfolio.id}>
                                    {portfolio.name}
                                </option>
                            ))}
                        </select>

                        <input
                            type='text'
                            value={assetName}
                            onChange={(e) => setAssetName(e.target.value)}
                            placeholder="Nom de l'asset"
                            className='border p-2 rounded'
                        />
                        <input
                            type='number'
                            value={volume}
                            onChange={(e) => setVolume(Number(e.target.value))}
                            placeholder='Volume'
                            className='border p-2 rounded'
                        />
                        <button
                            onClick={handleAssetPurchase}
                            className='bg-green-500 text-white p-2 rounded hover:bg-green-600'
                        >
                            Acheter
                        </button>
                    </div>

                    <div className='font-semibold text-center'>CY Stonks 💰</div>
                </div>
            </main>
        </div>
    );
}
