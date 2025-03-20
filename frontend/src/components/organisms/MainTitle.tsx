import React, { useEffect, useState } from "react";

export default function MainTitle(): React.ReactElement {
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
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const query = (e.target as HTMLFormElement).elements.namedItem('search') as HTMLInputElement;
            window.open(`https://www.google.com/finance/quote/${query.value}:NYSE?hl=fr`, '_blank');
          }}
        >
          <input
            type="text"
            name="search"
            className="outline-none px-5 py-2 rounded-full border border-gray-300"
            placeholder="🕵️‍♀️ Chercher en bourse"
          />
        </form>

        {username ? (
          <span className="text-lg font-medium">👤 {username}</span>
        ) : (
          <a href="/login" className="text-blue-500 hover:underline">Se connecter</a>
        )}
      </div>
    </header>
  );
}
