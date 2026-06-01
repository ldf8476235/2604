import React from "react";
import ReactDOM from "react-dom/client";
import { AdminApp } from "./pages/AdminApp";
import "@delta/ui/tokens.css";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AdminApp />
  </React.StrictMode>,
);

