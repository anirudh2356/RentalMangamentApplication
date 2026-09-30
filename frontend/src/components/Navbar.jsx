import { NavLink, useNavigate } from "react-router-dom";
import "../App.css";

function Navbar() {
  const navigate = useNavigate();

  return (
    <header className="navbar">
      <div className="navbar-container">

        {/* LOGO */}
        <div
          className="navbar-logo"
          onClick={() => navigate("/")}
        >
          <div className="logo-icon">
            🏠
          </div>

          <div className="logo-text">
            <strong>RealEstate</strong>
            <span>Find Your Perfect Place</span>
          </div>
        </div>

        {/* NAVIGATION */}
        <nav className="navbar-links">

          {/* HOME */}
          <NavLink
            to="/"
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            Home
          </NavLink>

          {/* BUY */}
          <NavLink
            to="/buy"
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            Buy
          </NavLink>

          {/* RENT */}
          <NavLink
            to="/rent"
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            Rent
          </NavLink>

          {/* SELL */}
          <NavLink
            to="/sell"
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            Sell
          </NavLink>

          {/* CONTACT */}
          <NavLink
            to="/contact"
            className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }
          >
            Contact
          </NavLink>

        </nav>

        {/* RIGHT SIDE */}
        <div className="navbar-actions">

          {/* FAVORITES */}
          <button
            className="favorite-nav-button"
            onClick={() => navigate("/favorites")}
            title="Favorites"
          >
            ♡
            <span className="favorite-count">0</span>
          </button>

          {/* LOGIN */}
          <button
            className="login-button"
            onClick={() => navigate("/login")}
          >
            Login
          </button>

          {/* REGISTER */}
          <button
            className="register-button"
            onClick={() => navigate("/register")}
          >
            Register
          </button>

        </div>

      </div>
    </header>
  );
}

export default Navbar;