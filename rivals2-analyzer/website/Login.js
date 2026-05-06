import React, { useState } from "react";

export default function Login({ setUser }) {
  const [username, setUsername] = useState("");

  const login = () => {
    setUser({
      id: 1,
      username: username
    });
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>Login</h2>

      <input
        type="text"
        placeholder="Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
      />

      <button
        onClick={login}
        style={{ marginLeft: "10px" }}
      >
        Login
      </button>
    </div>
  );
}
