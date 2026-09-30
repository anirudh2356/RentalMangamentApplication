import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "./components/Navbar";

function Home() {

  const navigate = useNavigate();

  const [location, setLocation] = useState("");
  const [propertyType, setPropertyType] = useState("Buy");
  const [budget, setBudget] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searched, setSearched] = useState(false);

  const properties = [
    {
      id: 1,
      title: "Luxury Villa",
      location: "Hyderabad, Telangana",
      price: "₹85 Lakhs",
      type: "Buy",
      beds: 4,
      baths: 4,
      area: "2,500 sqft",
      image:
        "https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=900&q=80",
    },

    {
      id: 2,
      title: "Modern Apartment",
      location: "Hyderabad, Telangana",
      price: "₹25,000 / month",
      type: "Rent",
      beds: 3,
      baths: 2,
      area: "1,200 sqft",
      image:
        "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=900&q=80",
    },

    {
      id: 3,
      title: "Independent House",
      location: "Bengaluru, Karnataka",
      price: "₹1.2 Crores",
      type: "Buy",
      beds: 4,
      baths: 3,
      area: "2,800 sqft",
      image:
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=900&q=80",
    },

    {
      id: 4,
      title: "Premium Apartment",
      location: "Pune, Maharashtra",
      price: "₹30,000 / month",
      type: "Rent",
      beds: 2,
      baths: 2,
      area: "1,100 sqft",
      image:
        "https://images.unsplash.com/photo-1600607688969-a5bfcd646154?auto=format&fit=crop&w=900&q=80",
    },
  ];

  const handleSearch = () => {

    const results = properties.filter((property) => {

      const locationMatch =
        location.trim() === "" ||
        property.location
          .toLowerCase()
          .includes(location.trim().toLowerCase());

      const typeMatch =
        property.type === propertyType;

      return locationMatch && typeMatch;
    });

    setSearchResults(results);
    setSearched(true);

    navigate("/search-results", {
      state: {
        results: results,
        location: location,
        propertyType: propertyType,
      },
    });
  };

  const handleBuy = () => {
    setPropertyType("Buy");
    setSearched(false);
  };

  const handleRent = () => {
    setPropertyType("Rent");
    setSearched(false);
  };

  return (
    <div className="home-page">

      <Navbar />

      {/* HERO SECTION */}

      <section className="hero-section">

        <div className="hero-overlay"></div>

        <div className="hero-container">

          <div className="hero-content">

            <div className="welcome-badge">
              Welcome to RealEstate
            </div>

            <h1>
              Find Your Dream
              <br />
              Property
            </h1>

            <p>
              Buy, rent, or sell properties with confidence.
              Discover the best homes, apartments, and
              investment opportunities in your favorite locations.
            </p>

            {/* STATISTICS */}

            <div className="stats">

              <div className="stat">
                <div className="stat-icon">⌂</div>
                <div>
                  <strong>10K+</strong>
                  <span>Verified Properties</span>
                </div>
              </div>

              <div className="stat">
                <div className="stat-icon">♙</div>
                <div>
                  <strong>5K+</strong>
                  <span>Happy Customers</span>
                </div>
              </div>

              <div className="stat">
                <div className="stat-icon">⌖</div>
                <div>
                  <strong>50+</strong>
                  <span>Cities Covered</span>
                </div>
              </div>

              <div className="stat">
                <div className="stat-icon">✓</div>
                <div>
                  <strong>100%</strong>
                  <span>Secure & Trusted</span>
                </div>
              </div>

            </div>

          </div>

        </div>

        {/* SEARCH PANEL */}

        <div className="search-panel">

          <div className="search-tabs">

            <button
              className={
                propertyType === "Buy"
                  ? "search-tab active"
                  : "search-tab"
              }
              onClick={handleBuy}
            >
              ⌂ &nbsp; Buy
            </button>

            <button
              className={
                propertyType === "Rent"
                  ? "search-tab active"
                  : "search-tab"
              }
              onClick={handleRent}
            >
              ⚿ &nbsp; Rent
            </button>

          </div>

          <div className="search-fields">

            <div className="search-field">

              <span className="field-icon">⌖</span>

              <input
                type="text"
                placeholder="Enter location (e.g., Hyderabad)"
                value={location}
                onChange={(event) =>
                  setLocation(event.target.value)
                }
              />

            </div>

            <div className="search-field select-field">

              <span className="field-icon">⌂</span>

              <select>
                <option value="">Property Type</option>
                <option>Apartment</option>
                <option>Villa</option>
                <option>Independent House</option>
                <option>Plot</option>
              </select>

            </div>

            <div className="search-field select-field">

              <span className="field-icon">₹</span>

              <select
                value={budget}
                onChange={(event) =>
                  setBudget(event.target.value)
                }
              >
                <option value="">Budget Range</option>
                <option>Below ₹50 Lakhs</option>
                <option>₹50 Lakhs - ₹1 Crore</option>
                <option>₹1 Crore - ₹2 Crores</option>
                <option>Above ₹2 Crores</option>
              </select>

            </div>

            <button
              className="search-button"
              onClick={handleSearch}
            >
              🔍 &nbsp; Search Property
            </button>

          </div>

        </div>

      </section>


      {/* SEARCH RESULTS */}

      {searched && (

        <section className="results-section">

          <div className="section-container">

            <div className="section-heading">

              <div>

                <span className="section-label">
                  Search Results
                </span>

                <h2>
                  {propertyType} Properties
                  {location && ` in ${location}`}
                </h2>

              </div>

            </div>

            {searchResults.length > 0 ? (

              <div className="property-grid">

                {searchResults.map((property) => (

                  <PropertyCard
                    key={property.id}
                    property={property}
                  />

                ))}

              </div>

            ) : (

              <div className="no-results">

                <h3>No properties found</h3>

                <p>
                  Try searching for another location.
                </p>

              </div>

            )}

          </div>

        </section>

      )}


      {/* FEATURED PROPERTIES */}

      {!searched && (

        <section className="featured-section">

          <div className="section-container">

            <div className="section-heading">

              <div>

                <span className="section-label">
                  Featured Properties
                </span>

                <h2>
                  Discover Our Best Properties
                </h2>

                <p>
                  Handpicked properties in top locations.
                  Find the perfect home for you.
                </p>

              </div>

              <button
                className="view-all-button"
                onClick={() => navigate("/buy")}
              >
                View All Properties →
              </button>

            </div>


            <div className="property-grid">

              {properties.map((property) => (

                <PropertyCard
                  key={property.id}
                  property={property}
                />

              ))}

            </div>

          </div>

        </section>

      )}


      {/* FOOTER */}

      <footer className="footer">

        <div className="footer-container">

          <div>
            <h3>RealEstate</h3>
            <p>
              Find your perfect place with confidence.
            </p>
          </div>

          <div>
            <h4>Quick Links</h4>
            <p>Buy Property</p>
            <p>Rent Property</p>
            <p>Sell Property</p>
          </div>

          <div>
            <h4>Contact</h4>
            <p>Hyderabad, Telangana</p>
            <p>support@realestate.com</p>
          </div>

        </div>

        <div className="footer-bottom">
          © 2026 RealEstate. All rights reserved.
        </div>

      </footer>

    </div>
  );
}


/* PROPERTY CARD */

function PropertyCard({ property }) {

  return (

    <div className="property-card">

      <div className="property-image-container">

        <img
          src={property.image}
          alt={property.title}
          className="property-image"
        />

        <span
          className={
            property.type === "Buy"
              ? "property-badge sale"
              : "property-badge rent"
          }
        >
          {property.type === "Buy"
            ? "For Sale"
            : "For Rent"}
        </span>

        <button className="favorite-button">
          ♡
        </button>

      </div>

      <div className="property-content">

        <div className="property-price">
          {property.price}
        </div>

        <h3>{property.title}</h3>

        <p className="property-location">
          ⌖ {property.location}
        </p>

        <div className="property-details">

          <span>🛏 {property.beds} Beds</span>

          <span>♨ {property.baths} Baths</span>

          <span>□ {property.area}</span>

        </div>

      </div>

    </div>

  );
}

export default Home;