import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { v4 as uuidv4 } from "uuid";

export default function RegisterForm() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const navigate = useNavigate();

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError("");
        setSuccessMessage("");

        const uuid = uuidv4();
        const requestData = {
            uuid,
            username,
            password,
            wallets: [],
            financialInterests: [],
        };

        console.log("Submitting registration data:", requestData);

        try {
            const response = await fetch("http://localhost:8081/users", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(requestData),
                credentials: "include",
            });

            if (!response.ok) {
                const errorData = await response.json();
                console.log("Server error:", errorData);
                throw new Error(errorData.message || "Erreur lors de l'inscription !");
            }

            const data = await response.json();
            console.log("Registration successful, response:", data);

            setSuccessMessage("Inscription réussie! Redirection vers la page de connexion...");

            setTimeout(() => {
                navigate("/login");
            }, 1500);

        } catch (error) {
            if (error instanceof Error) {
                setError(error.message);
            } else {
                console.error("Une erreur inconnue est survenue.");
                setError("Une erreur inconnue est survenue.");
            }
        }
    };

    return (
        <div className="flex justify-center items-center min-h-screen bg-gray-100">
            <div className="bg-white p-6 rounded-lg shadow-lg w-96">
                <h2 className="text-2xl font-bold text-center mb-4">Inscription</h2>

                {/* Success message */}
                {successMessage && (
                    <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
                        {successMessage}
                    </div>
                )}

                {/* Error message */}
                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                        {error}
                    </div>
                )}

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
                    <button
                        type="submit"
                        className="bg-green-500 text-white p-2 rounded hover:bg-green-600"
                    >
                        S'inscrire
                    </button>
                </form>
                <p className="text-sm text-center mt-3">
                    Déjà un compte ? <a href="/login" className="text-blue-500">Connecte-toi</a>
                </p>
            </div>
        </div>
    );
}