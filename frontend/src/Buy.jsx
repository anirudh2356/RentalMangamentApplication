import { useState } from "react";
import Navbar from "./components/Navbar";

const properties = [
  {
    id: 1,
    title: "Luxury Villa",
    location: "Hyderabad",
    category: "Villa",
    price: "₹85 Lakhs",
    beds: 4,
    baths: 3,
    area: "2,400 sq.ft",
    image:
      "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 2,
    title: "Modern Apartment",
    location: "Hyderabad",
    category: "Apartment",
    price: "₹75 Lakhs",
    beds: 3,
    baths: 2,
    area: "1,650 sq.ft",
    image:
      "https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 3,
    title: "Family Apartment",
    location: "Pune",
    category: "Apartment",
    price: "₹68 Lakhs",
    beds: 3,
    baths: 2,
    area: "1,450 sq.ft",
    image:
      "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 4,
    title: "Independent House",
    location: "Bengaluru",
    category: "Independent House",
    price: "₹1.2 Crores",
    beds: 4,
    baths: 3,
    area: "2,100 sq.ft",
    image:
      "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 5,
    title: "Premium Villa",
    location: "Pune",
    category: "Villa",
    price: "₹1.1 Crores",
    beds: 4,
    baths: 4,
    area: "2,800 sq.ft",
    image:
      "https://images.unsplash.com/photo-1613490493576-7fde63acd811?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 6,
    title: "Residential Plot",
    location: "Hyderabad",
    category: "Plot",
    price: "₹45 Lakhs",
    beds: 0,
    baths: 0,
    area: "2,000 sq.ft",
    image:
      "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=900&q=80",
  },
];

function Buy() {
  const [location, setLocation] = useState("");
  const [category, setCategory] = useState("All");
  const [budget, setBudget] = useState("All");

  const filteredProperties = properties.filter((property) => {
    const locationMatch =
      location === "" ||
      property.location.toLowerCase().includes(location.toLowerCase());

    const categoryMatch =
      category === "All" || property.category === category;

    let budgetMatch = true;

    if (budget === "Under 50 Lakhs") {
      budgetMatch =
        property.price.includes("45") ||
        property.price.includes("40") ||
        property.price.includes("35");
    }

    if (budget === "50 Lakhs - 1 Crore") {
      budgetMatch =
        property.price.includes("68") ||
        property.price.includes("75") ||
        property.price.includes("85");
    }

    if (budget === "Above 1 Crore") {
      budgetMatch =
        property.price.includes("1.1") ||
        property.price.includes("1.2");
    }

    return locationMatch && categoryMatch && budgetMatch;
  });

  return (
    <>
      <Navbar />

      <main className="buy-page">

        {/* HERO */}

        <section className="buy-hero">
          <div className="buy-hero-content">
            <span className="section-label">BUY PROPERTY</span>

            <h1>Find Your Perfect Property</h1>

            <p>
              Explore apartments, villas, houses and plots available for sale.
            </p>
          </div>
        </section>

        {/* FILTERS */}

        <section className="buy-filter-section">
          <div className="buy-filters">

            <div className="buy-filter">
              <label>Location</label>
              <input
                type="text"
                placeholder="Search city"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
              />
            </div>

            <div className="buy-filter">
              <label>Property Type</label>

              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                <option value="All">All Types</option>
                <option value="Apartment">Apartment</option>
                <option value="Villa">Villa</option>
                <option value="Independent House">
                  Independent House
                </option>
                <option value="Plot">Plot</option>
              </select>
            </div>

            <div className="buy-filter">
              <label>Budget</label>

              <select
                value={budget}
                onChange={(e) => setBudget(e.target.value)}
              >
                <option value="All">Any Budget</option>
                <option value="Under 50 Lakhs">
                  Under ₹50 Lakhs
                </option>
                <option value="50 Lakhs - 1 Crore">
                  ₹50 Lakhs - ₹1 Crore
                </option>
                <option value="Above 1 Crore">
                  Above ₹1 Crore
                </option>
              </select>
            </div>

          </div>
        </section>

        {/* PROPERTIES */}

        <section className="buy-properties">

          <div className="buy-section-heading">
            <div>
              <span className="section-label">PROPERTY LISTINGS</span>

              <h2>Properties for Sale</h2>
            </div>

            <p>
              {filteredProperties.length} properties found
            </p>
          </div>

          {filteredProperties.length > 0 ? (

            <div className="buy-property-grid">

              {filteredProperties.map((property) => (

                <div className="buy-property-card" key={property.id}>

                  <div className="buy-property-image">

                    <img
                      src={property.image}
                      alt={property.title}
                    />

                    <span className="buy-sale-badge">
                      For Sale
                    </span>

                    <button className="buy-heart">
                      ♡
                    </button>

                  </div>

                  <div className="buy-property-content">

                    <div className="buy-price">
                      {property.price}
                    </div>

                    <h3>{property.title}</h3>

                    <p className="buy-location">
                      📍 {property.location}
                    </p>

                    <div className="buy-details">

                      <span>
                        🛏 {property.beds} Beds
                      </span>

                      <span>
                        🛁 {property.baths} Baths
                      </span>

                      <span>
                        📐 {property.area}
                      </span>

                    </div>

                    <button className="buy-view-button">
                      View Details
                    </button>

                  </div>

                </div>

              ))}

            </div>

          ) : (

            <div className="buy-no-results">

              <h3>No properties found</h3>

              <p>
                Try changing your location, property type or budget.
              </p>

            </div>

          )}

        </section>

      </main>
    </>
  );
}

export default Buy;