import React, { useEffect, useState } from "react";

export default function History({ user }) {

  const [matches, setMatches] = useState([]);

  useEffect(() => {

    console.log("Loading history for user:", user);

    fetch(`http://localhost:8080/api/moves/history/${user.id}`)
      .then((res) => res.json())
      .then((data) => {

        console.log("Fetched history:", data);

        setMatches(data);

      })
      .catch((err) => {
        console.error("History fetch error:", err);
      });

  }, [user]);

  return (
    <div>

      <h2>Match History</h2>

      <div>
        Matches Loaded: {matches.length}
      </div>

      {matches.length === 0 ? (

        <p>No matches found.</p>

      ) : (

        matches.map((match) => (

          <div
            key={match.id}
            style={{
              border: "1px solid gray",
              padding: "10px",
              marginTop: "10px",
              backgroundColor: "#222",
              color: "white"
            }}
          >

            <div>
              <strong>ID:</strong> {match.id}
            </div>

            <div>
              Time: {new Date(match.matchTime).toLocaleString()}
            </div>

            <div>
              <strong>Moves:</strong>
            </div>

            <pre>
              {match.moves}
            </pre>

          </div>

        ))

      )}

    </div>
  );
}
