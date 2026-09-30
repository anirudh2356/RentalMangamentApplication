import { BrowserRouter, Routes, Route } from "react-router-dom";

import Home from "./Home";
import SearchResults from "./SearchResults";
import Buy from "./Buy";
import Navbar from "./components/Navbar";

import "./index.css";


/* =========================================================
   SIMPLE PAGE COMPONENT
========================================================= */

function SimplePage({ title, description }) {
  return (
    <div className="simple-page-wrapper">

      <Navbar />

      <main className="simple-page">

        <div className="simple-page-content">

          <span className="section-label">
            RealEstate
          </span>

          <h1>{title}</h1>

          <p>{description}</p>

          <button
            className="simple-page-button"
            onClick={() => {
              window.location.href = "/";
            }}
          >
            ← Back to Home
          </button>

        </div>

      </main>

    </div>
  );
}


/* =========================================================
   RENT
========================================================= */

function Rent() {
  return (
    <SimplePage
      title="Rent Properties"
      description="Find apartments, villas, houses and other properties available for rent."
    />
  );
}


/* =========================================================
   SELL
========================================================= */

function Sell() {
  return (
    <SimplePage
      title="Sell Your Property"
      description="List your property and connect with potential buyers through RealEstate."
    />
  );
}


/* =========================================================
   LOGIN
========================================================= */

function Login() {
  return (
    <SimplePage
      title="Login"
      description="Login functionality will be connected to the authentication module."
    />
  );
}


/* =========================================================
   REGISTER
========================================================= */

function Register() {
  return (
    <SimplePage
      title="Create Your Account"
      description="Registration functionality will be connected to the authentication module."
    />
  );
}


/* =========================================================
   CONTACT
========================================================= */

function Contact() {
  return (
    <SimplePage
      title="Contact Us"
      description="Get in touch with the RealEstate team for property-related assistance."
    />
  );
}


/* =========================================================
   APP
========================================================= */

function App() {

  return (

    <BrowserRouter>

      <Routes>

        {/* HOME */}

        <Route
          path="/"
          element={<Home />}
        />


        {/* SEARCH RESULTS */}

        <Route
          path="/search-results"
          element={<SearchResults />}
        />


        {/* BUY */}

        <Route
          path="/buy"
          element={<Buy />}
        />


        {/* RENT */}

        <Route
          path="/rent"
          element={<Rent />}
        />


        {/* SELL */}

        <Route
          path="/sell"
          element={<Sell />}
        />


        {/* LOGIN */}

        <Route
          path="/login"
          element={<Login />}
        />


        {/* REGISTER */}

        <Route
          path="/register"
          element={<Register />}
        />


        {/* CONTACT */}

        <Route
          path="/contact"
          element={<Contact />}
        />

      </Routes>

    </BrowserRouter>

  );
}


export default App;