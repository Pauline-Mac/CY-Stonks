import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function LoginForm() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError("");
        try {
            const response = await fetch("http://localhost:8081/users/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password }),
                credentials: "include",
            });

            if (!response.ok) {
                throw new Error("Identifiants incorrects !");
            }

            const data = await response.json();
            console.log("Réponse complète:", data);

            localStorage.setItem("token", data.token);

            // Une fois connecté, faites une seconde requête pour obtenir les informations de l'utilisateur
            const userResponse = await fetch("http://localhost:8081/users/me", {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${data.token}`
                },
                credentials: "include",
            });

            if (userResponse.ok) {
                const userData = await userResponse.json();
                console.log("Données utilisateur:", userData);

                if (userData.uuid) {
                    localStorage.setItem("uuid", userData.uuid);
                } else {
                    console.error("UUID non trouvé dans les données utilisateur");
                }
            }

            navigate("/");
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message);
            } else {
                console.error("Une erreur inconnue est survenue.");
            }
        }
    };

    return (
        <div className="flex justify-center items-center min-h-screen bg-gray-100">
            <div className="bg-white p-6 rounded-lg shadow-lg w-96">
                <h2 className="text-2xl font-bold text-center mb-4">Connexion</h2>
                {error && <p className="text-red-500 text-sm text-center">{error}</p>}
                <form onSubmit={handleSubmit} className="flex flex-col gap-3">
                    <input
                        type="text"
                        placeholder="Nom d'utilisateur"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="border p-2 rounded"
                        required
                    />
                    <input
                        type="password"
                        placeholder="Mot de passe"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="border p-2 rounded"
                        required
                    />
                    <button type="submit" className="bg-blue-500 text-white p-2 rounded hover:bg-blue-600">
                        Se connecter
                    </button>
                </form>
                <p className="text-sm text-center mt-3">
                    Pas encore de compte ? <a href="/register" className="text-blue-500">Inscris-toi</a>
                </p>
            </div>
        </div>
    );
}
