import { useState } from "react";
import Login from "./Login";
import Dashboard from "./Dashboard";
import History from "./History";
import "./App.css";

function App() {
  const [user, setUser] = useState(null);
  const [view, setView] = useState("dashboard");

  if (!user) {
    return <Login setUser={setUser} />;
  }

  return (
    <div className="app-shell">

      {/* Top Navigation */}
      <header className="topbar">

        <div>
          <h1 className="title">Rivals 2 Match Analyzer</h1>
          <p className="subtitle">
            Current View: {view.charAt(0).toUpperCase() + view.slice(1)}
          </p>
        </div>

        <div className="nav-buttons">
          <button onClick={() => setView("dashboard")}>
            Dashboard
          </button>

          <button onClick={() => setView("history")}>
            History
          </button>

          <button
            className="logout-btn"
            onClick={() => setUser(null)}>
            Logout
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="main-content">
        {view === "dashboard" && <Dashboard />}
        {view === "history" && <History user={user} />}
      </main>

    </div>
  );
}

export default App;
