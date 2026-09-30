import { useLocation, useNavigate } from "react-router-dom";
import Navbar from "./components/Navbar";

function SearchResults() {

  const location = useLocation();
  const navigate = useNavigate();

  const {
    results = [],
    location: searchLocation = "",
    propertyType = "Buy",
  } = location.state || {};

  return (
    <div className="search-results-page">

      <Navbar />

      <section className="search-results-section">

        <div className="section-container">

          <div className="section-heading">

            <div>

              <span className="section-label">
                Search Results
              </span>

              <h1>
                {propertyType} Properties
                {searchLocation && ` in ${searchLocation}`}
              </h1>

              <p>
                {results.length} properties found
              </p>

            </div>

          </div>


          {/* PROPERTY RESULTS */}

          {results.length > 0 ? (

            <div className="property-grid">

              {results.map((property) => (

                <PropertyCard
                  key={property.id}
                  property={property}
                />

              ))}

            </div>

          ) : (

            <div className="no-results">

              <h2>No properties found</h2>

              <p>
                Try searching for another location or property type.
              </p>

              <button
                className="search-button"
                onClick={() => navigate("/")}
              >
                ← Back to Search
              </button>

            </div>

          )}

        </div>

      </section>

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

        <h3>
          {property.title}
        </h3>

        <p className="property-location">
          ⌖ {property.location}
        </p>

        <div className="property-details">

          <span>
            🛏 {property.beds} Beds
          </span>

          <span>
            ♨ {property.baths} Baths
          </span>

          <span>
            □ {property.area}
          </span>

        </div>

      </div>

    </div>
  );
}

export default SearchResults;