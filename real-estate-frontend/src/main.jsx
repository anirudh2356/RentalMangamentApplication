import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import "./index.css";

import App from "./App.jsx";
import PropertyDetails from "./PropertyDetails.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>

        {/* Main application */}
        <Route path="/" element={<App />} />

        {/* Property details page */}
        <Route
          path="/property/:propertyId"
          element={<PropertyDetails />}
        />

      </Routes>
    </BrowserRouter>
  </StrictMode>
);