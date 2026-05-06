import React, { useEffect, useState } from "react";
import "./Dashboard.css";

export default function Dashboard() {
  const [moves, setMoves] = useState({});
  const [text, setText] = useState("Waiting for match...");
  const [running, setRunning] = useState(false);

  // Auto-refresh only while running
  useEffect(() => {

    if (!running) {
      return;
    }

    const interval = setInterval(() => {
      fetch("http://localhost:8080/api/moves")
        .then((res) => res.json())
        .then((data) => setMoves(data))
        .catch((err) => console.error(err));
    }, 1000);

    return () => clearInterval(interval);

  }, [running]);

  // Start predictor
  const startPredictor = () => {

    fetch("http://localhost:8080/api/moves/start", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      }
    });

    // clear previous match data
    setMoves({});

    setRunning(true);

    setText("Match running...");
  };

  // Stop predictor
  const stopPredictor = () => {

    fetch("http://localhost:8080/api/moves/stop", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      }
    });

    setRunning(false);

    // clear displayed moves
    setMoves({});

    setText("Waiting for match...");
  };

  return (
    <div className="container">

      {/* Background */}
      <img
        src="/training-room.png"
        alt="background"
        className="background"
      />

      {/* Move display */}
      <div className="moves">
        <h2>Moves</h2>

        {Object.keys(moves).length === 0 ? (
          <div>No moves detected</div>
        ) : (
          Object.entries(moves).map(([move, count]) => (
            <div key={move}>
              {move}: {count}
            </div>
          ))
        )}
      </div>

      {/* Start/Stop button */}
      <div className="controls">
        {!running ? (
          <button onClick={startPredictor}>
            Start
          </button>
        ) : (
          <button onClick={stopPredictor}>
            Stop
          </button>
        )}
      </div>

      {/* Editable textbox */}
      <div className="textbox">
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
      </div>

    </div>
  );
}
