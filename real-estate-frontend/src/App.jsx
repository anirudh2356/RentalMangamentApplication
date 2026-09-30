

import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

/* =========================================================
   AMENITY IMAGES
   EXACT FOLDER:
   src/assets/Aminities
========================================================= */

import gymImage from "./assets/Aminities/gym.jpg";
import swimmingPoolImage from "./assets/Aminities/swimming pool.jpg";
import gardenImage from "./assets/Aminities/garden.jpg";
import clubHouseImage from "./assets/Aminities/club house.jpg";
import parkingImage from "./assets/Aminities/covered parking image.jpg";
import securityImage from "./assets/Aminities/security image.jpg";
import powerBackupImage from "./assets/Aminities/power back up image.jpg";

/* =========================================================
   LOAD ALL PROPERTY IMAGES
========================================================= */

const allImages = import.meta.glob(
  "./assets/**/*.{png,jpg,jpeg,webp}",
  {
    eager: true,
    import: "default",
    caseSensitive: false,
  }
);

/* =========================================================
   HELPERS
========================================================= */

const normalizeName = (name) =>
  name
    .toLowerCase()
    .replace(/[\s_-]+/g, "");

/* =========================================================
   GET ALL IMAGES FROM PROPERTY FOLDER
========================================================= */

const getPropertyImages = (propertyNumber) => {
  const possibleNames = [
    `property${propertyNumber}`,
    `propertyimages${propertyNumber}`,
  ];

  return Object.entries(allImages)
    .filter(([path]) => {
      const cleanPath = path.replaceAll("\\", "/");

      const parts = cleanPath.split("/");

      const folderName =
        parts[parts.length - 2] || "";

      const normalizedFolder =
        normalizeName(folderName);

      return possibleNames.includes(
        normalizedFolder
      );
    })
    .map(([, image]) => image);
};

/* =========================================================
   FALLBACK IMAGE
========================================================= */

const fallbackImage =
  "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=80";

/* =========================================================
   AMENITY IMAGE MAP
========================================================= */

const amenityImageMap = {
  Gym: gymImage,
  "Swimming Pool": swimmingPoolImage,
  Garden: gardenImage,
  "Club House": clubHouseImage,
  Parking: parkingImage,
  "24/7 Security": securityImage,
  Security: securityImage,
  "Power Backup": powerBackupImage,
};

/* =========================================================
   GET AMENITY IMAGE
========================================================= */

const getAmenityImage = (amenityName) => {
  return amenityImageMap[amenityName] || fallbackImage;
};

/* =========================================================
   PROPERTY DATA
========================================================= */

const properties = [
  {
    id: "PE-3025",
    propertyNumber: 1,
    title: "Luxury 3 BHK Apartment",
    location: "Whitefield, Bangalore",
    price: "₹1.85 Cr",
    type: "Apartment",
    bedrooms: 3,
    bathrooms: 3,
    area: "2,150 sq.ft",
    status: "Ready to Move",
    description:
      "A beautifully designed luxury apartment offering spacious interiors, modern facilities and a premium lifestyle in one of Bangalore's most desirable locations.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
      "Power Backup",
    ],
    agent: {
      name: "Riya Sharma",
      role: "Senior Property Consultant",
      phone: "+91 98765 43210",
      email: "riya@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-4182",
    propertyNumber: 2,
    title: "Premium 2 BHK Residence",
    location: "Electronic City, Bangalore",
    price: "₹92 Lakh",
    type: "Apartment",
    bedrooms: 2,
    bathrooms: 2,
    area: "1,280 sq.ft",
    status: "Ready to Move",
    description:
      "A modern 2 BHK residence with elegant interiors, excellent connectivity and thoughtfully planned living spaces.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
    ],
    agent: {
      name: "Arjun Mehta",
      role: "Property Advisor",
      phone: "+91 98765 12345",
      email: "arjun@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-5290",
    propertyNumber: 3,
    title: "Modern Family Villa",
    location: "Sarjapur Road, Bangalore",
    price: "₹2.45 Cr",
    type: "Villa",
    bedrooms: 4,
    bathrooms: 4,
    area: "3,400 sq.ft",
    status: "Ready to Move",
    description:
      "A spacious family villa combining contemporary architecture, beautiful surroundings and premium lifestyle amenities.",
    amenities: [
      "Gym",
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
      "Power Backup",
    ],
    agent: {
      name: "Neha Patel",
      role: "Luxury Property Consultant",
      phone: "+91 99887 66554",
      email: "neha@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-6314",
    propertyNumber: 4,
    title: "Elegant 3 BHK Home",
    location: "HSR Layout, Bangalore",
    price: "₹1.72 Cr",
    type: "Apartment",
    bedrooms: 3,
    bathrooms: 3,
    area: "1,980 sq.ft",
    status: "Available",
    description:
      "An elegant home designed for comfortable family living with modern interiors and excellent neighborhood connectivity.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Parking",
      "24/7 Security",
    ],
    agent: {
      name: "Karan Shah",
      role: "Real Estate Consultant",
      phone: "+91 99887 11223",
      email: "karan@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-7421",
    propertyNumber: 5,
    title: "Contemporary 2 BHK Apartment",
    location: "Marathahalli, Bangalore",
    price: "₹86 Lakh",
    type: "Apartment",
    bedrooms: 2,
    bathrooms: 2,
    area: "1,190 sq.ft",
    status: "Ready to Move",
    description:
      "A contemporary apartment offering efficient spaces, modern amenities and convenient access to major IT corridors.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Parking",
      "Security",
    ],
    agent: {
      name: "Priya Desai",
      role: "Property Specialist",
      phone: "+91 99001 23456",
      email: "priya@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-8536",
    propertyNumber: 6,
    title: "Premium 4 BHK Villa",
    location: "Devanahalli, Bangalore",
    price: "₹3.25 Cr",
    type: "Villa",
    bedrooms: 4,
    bathrooms: 5,
    area: "4,200 sq.ft",
    status: "Under Construction",
    description:
      "A premium villa offering generous living spaces, private surroundings and an upscale lifestyle near North Bangalore.",
    amenities: [
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
      "Power Backup",
    ],
    agent: {
      name: "Rahul Kapoor",
      role: "Luxury Property Advisor",
      phone: "+91 98111 22334",
      email: "rahul@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-9642",
    propertyNumber: 7,
    title: "Luxury Urban Apartment",
    location: "Indiranagar, Bangalore",
    price: "₹2.10 Cr",
    type: "Apartment",
    bedrooms: 3,
    bathrooms: 3,
    area: "2,050 sq.ft",
    status: "Available",
    description:
      "A luxury urban residence located close to premium restaurants, shopping destinations and business hubs.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Club House",
      "Parking",
      "Security",
    ],
    agent: {
      name: "Meera Rao",
      role: "Senior Property Consultant",
      phone: "+91 98888 44556",
      email: "meera@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-1078",
    propertyNumber: 8,
    title: "Spacious 3 BHK Residence",
    location: "Koramangala, Bangalore",
    price: "₹1.95 Cr",
    type: "Apartment",
    bedrooms: 3,
    bathrooms: 3,
    area: "2,020 sq.ft",
    status: "Ready to Move",
    description:
      "A spacious residence with a sophisticated layout and premium facilities in a prime Bangalore neighborhood.",
    amenities: [
      "Gym",
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
    ],
    agent: {
      name: "Aditya Verma",
      role: "Property Consultant",
      phone: "+91 97777 33445",
      email: "aditya@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-2184",
    propertyNumber: 9,
    title: "Modern 2 BHK Home",
    location: "Yelahanka, Bangalore",
    price: "₹78 Lakh",
    type: "Apartment",
    bedrooms: 2,
    bathrooms: 2,
    area: "1,150 sq.ft",
    status: "Available",
    description:
      "A modern and comfortable home ideal for families looking for connectivity, convenience and peaceful surroundings.",
    amenities: [
      "Swimming Pool",
      "Garden",
      "Parking",
      "Security",
      "Power Backup",
    ],
    agent: {
      name: "Sneha Joshi",
      role: "Property Advisor",
      phone: "+91 96666 77889",
      email: "sneha@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-3295",
    propertyNumber: 10,
    title: "Premium 3 BHK Residence",
    location: "Hebbal, Bangalore",
    price: "₹1.48 Cr",
    type: "Apartment",
    bedrooms: 3,
    bathrooms: 3,
    area: "1,870 sq.ft",
    status: "Ready to Move",
    description:
      "A premium residence offering modern design, spacious interiors and excellent access to North Bangalore.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Parking",
      "Security",
    ],
    agent: {
      name: "Vikram Singh",
      role: "Property Consultant",
      phone: "+91 95555 66778",
      email: "vikram@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-4316",
    propertyNumber: 11,
    title: "Luxury Family Villa",
    location: "Kanakapura Road, Bangalore",
    price: "₹2.85 Cr",
    type: "Villa",
    bedrooms: 4,
    bathrooms: 4,
    area: "3,850 sq.ft",
    status: "Available",
    description:
      "A peaceful luxury villa designed for family living with beautiful open spaces and premium facilities.",
    amenities: [
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
      "Power Backup",
    ],
    agent: {
      name: "Aarav Malhotra",
      role: "Luxury Property Consultant",
      phone: "+91 94444 55667",
      email: "aarav@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-5427",
    propertyNumber: 12,
    title: "Elegant 2 BHK Apartment",
    location: "Bellandur, Bangalore",
    price: "₹1.05 Cr",
    type: "Apartment",
    bedrooms: 2,
    bathrooms: 2,
    area: "1,300 sq.ft",
    status: "Ready to Move",
    description:
      "A stylish 2 BHK apartment located near Bangalore's major technology parks and commercial areas.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Club House",
      "Parking",
      "Security",
    ],
    agent: {
      name: "Isha Kapoor",
      role: "Property Specialist",
      phone: "+91 93333 44556",
      email: "isha@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-6538",
    propertyNumber: 13,
    title: "Contemporary 3 BHK Apartment",
    location: "JP Nagar, Bangalore",
    price: "₹1.35 Cr",
    type: "Apartment",
    bedrooms: 3,
    bathrooms: 3,
    area: "1,720 sq.ft",
    status: "Available",
    description:
      "A contemporary home with elegant interiors and thoughtfully planned spaces for modern family living.",
    amenities: [
      "Gym",
      "Garden",
      "Club House",
      "Parking",
      "Power Backup",
    ],
    agent: {
      name: "Rohan Mehta",
      role: "Real Estate Consultant",
      phone: "+91 92222 33445",
      email: "rohan@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-7649",
    propertyNumber: 14,
    title: "Premium 4 BHK Residence",
    location: "Bannerghatta Road, Bangalore",
    price: "₹2.65 Cr",
    type: "Villa",
    bedrooms: 4,
    bathrooms: 5,
    area: "3,650 sq.ft",
    status: "Available",
    description:
      "A premium residence combining luxury, privacy and modern architecture in a well-connected location.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Parking",
      "24/7 Security",
      "Power Backup",
    ],
    agent: {
      name: "Nikhil Sharma",
      role: "Senior Property Consultant",
      phone: "+91 91111 22334",
      email: "nikhil@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },

  {
    id: "PE-8750",
    propertyNumber: 15,
    title: "Luxury Dream Villa",
    location: "Whitefield, Bangalore",
    price: "₹3.75 Cr",
    type: "Villa",
    bedrooms: 5,
    bathrooms: 5,
    area: "4,800 sq.ft",
    status: "Premium Listing",
    description:
      "An impressive luxury villa offering expansive interiors, premium amenities and an exceptional lifestyle experience.",
    amenities: [
      "Gym",
      "Swimming Pool",
      "Garden",
      "Club House",
      "Parking",
      "24/7 Security",
      "Power Backup",
    ],
    agent: {
      name: "Ananya Shah",
      role: "Luxury Property Specialist",
      phone: "+91 90000 11223",
      email: "ananya@realestate.com",
    },
    virtualTour:
      "https://www.youtube.com/embed/ScMzIvxBSi4",
  },
];

/* =========================================================
   APP
========================================================= */

function App() {

  const navigate = useNavigate();

  const [selectedId, setSelectedId] =
    useState(properties[0].id);

  const [selectedImage, setSelectedImage] =
    useState(null);

  const [showVideo, setShowVideo] =
    useState(false);

  const selectedProperty = useMemo(() => {
    return (
      properties.find(
        (property) =>
          property.id === selectedId
      ) || properties[0]
    );
  }, [selectedId]);

  /* =======================================================
     PROPERTY GALLERY
  ======================================================= */

  const galleryImages = useMemo(() => {
    const images = getPropertyImages(
      selectedProperty.propertyNumber
    );

    return images.length > 0
      ? images
      : [fallbackImage];
  }, [selectedProperty]);

  /* =======================================================
     SIMILAR PROPERTIES
  ======================================================= */

  const similarProperties = useMemo(() => {
    return properties
      .filter(
        (property) =>
          property.id !== selectedProperty.id
      )
      .filter(
        (property) =>
          property.type === selectedProperty.type
      )
      .slice(0, 3);
  }, [selectedProperty]);

  /* =======================================================
     IMAGE MODAL
  ======================================================= */

  const openImage = (index) => {
    setSelectedImage(index);
  };

  const closeImage = () => {
    setSelectedImage(null);
  };

  const nextImage = () => {
    setSelectedImage((current) => {
      if (current === null) return 0;

      return (
        (current + 1) %
        galleryImages.length
      );
    });
  };

  const previousImage = () => {
    setSelectedImage((current) => {
      if (current === null) return 0;

      return (
        (current - 1 + galleryImages.length) %
        galleryImages.length
      );
    });
  };

  return (
    <div className="app">

      {/* =================================================
          NAVBAR
      ================================================= */}

      <header className="navbar">
        <div className="nav-inner">

          <div className="brand">
            <div className="brand-mark">
              RE
            </div>

            <div>
              <h2>REAL ESTATE</h2>
              <span>PREMIUM PROPERTIES</span>
            </div>
          </div>

          <nav className="nav-links">
            <a href="#overview">Overview</a>
            <a href="#gallery">Gallery</a>
            <a href="#amenities">Amenities</a>
            <a href="#location">Location</a>
            <a href="#agent">Agent</a>
          </nav>

          <button
            className="contact-nav"
            onClick={() =>
              document
                .getElementById("agent")
                ?.scrollIntoView({
                  behavior: "smooth",
                })
            }
          >
            Contact Agent
          </button>

        </div>
      </header>

      {/* =================================================
          PROPERTY SELECTOR
      ================================================= */}

      <section className="property-selector">
        <div className="selector-inner">

          <div>
            <span className="selector-label">
              EXPLORE PROPERTIES
            </span>

            <h1>
              Find Your Perfect Home
            </h1>
          </div>

          <select
            value={selectedId}
            onChange={(event) =>
              setSelectedId(event.target.value)
            }
          >
            {properties.map((property) => (
              <option
                key={property.id}
                value={property.id}
              >
                {property.id} —{" "}
                {property.title}
              </option>
            ))}
          </select>

        </div>
      </section>

      {/* =================================================
          HERO
      ================================================= */}

      <main>

        <section
          className="hero-section"
          id="overview"
        >

          <div className="hero-content">

            <div className="hero-label">
              <span></span>
              FEATURED PROPERTY
            </div>

            <h1>
              {selectedProperty.title}
            </h1>

            <p className="hero-location">
              📍 {selectedProperty.location}
            </p>

            <p className="hero-description">
              {selectedProperty.description}
            </p>

            <div className="hero-price-row">

              <div>
                <span className="price-label">
                  PROPERTY PRICE
                </span>

                <strong>
                  {selectedProperty.price}
                </strong>
              </div>

              <div className="hero-status">
                {selectedProperty.status}
              </div>

            </div>

            <button
                    className="gold-button"
                    onClick={() =>
                    navigate(`/property/${selectedProperty.propertyNumber}`)
                  }
              >
                  View Full Property Details →
            </button>

            <div className="hero-stats">

              <div>
                <strong>
                  {selectedProperty.bedrooms}
                </strong>
                <span>Bedrooms</span>
              </div>

              <div>
                <strong>
                  {selectedProperty.bathrooms}
                </strong>
                <span>Bathrooms</span>
              </div>

              <div>
                <strong>
                  {selectedProperty.area}
                </strong>
                <span>Built-up Area</span>
              </div>

            </div>

          </div>

          <div className="hero-accent">
            <div>
              <span>PROPERTY ID</span>
              <strong>
                {selectedProperty.id}
              </strong>
            </div>

            <div>
              <span>PROPERTY TYPE</span>
              <strong>
                {selectedProperty.type}
              </strong>
            </div>
          </div>

        </section>

        {/* =================================================
            MAIN IMAGE SHOWCASE
        ================================================= */}

        <section className="showcase-section">

          <div className="section-heading">

            <div>
              <span className="section-label">
                PROPERTY VIEW
              </span>

              <h2>
                Explore The Residence
              </h2>
            </div>

            <p>
              Explore every corner of this
              beautiful property through our
              detailed image collection.
            </p>

          </div>

          <div className="main-property-showcase">

            <div
              className="showcase-main"
              onClick={() => openImage(0)}
            >

              <img
                src={
                  galleryImages[0] ||
                  fallbackImage
                }
                alt={`${selectedProperty.title} main`}
              />

              <span className="property-badge">
                FOR SALE
              </span>

              <span className="image-count">
                📷 {galleryImages.length} Photos
              </span>

              <div className="showcase-view">
                🔍 View Gallery
              </div>

            </div>

            <div className="showcase-side">

              {galleryImages
                .slice(1, 5)
                .map((image, index) => (
                  <div
                    className="showcase-small"
                    key={`${selectedProperty.id}-showcase-${index}`}
                    onClick={() =>
                      openImage(index + 1)
                    }
                  >

                    <img
                      src={image}
                      alt={`${selectedProperty.title} ${
                        index + 2
                      }`}
                    />

                    {index === 3 &&
                      galleryImages.length >
                        5 && (
                        <div className="more-images">
                          <strong>
                            +
                            {galleryImages.length -
                              5}
                          </strong>

                          <span>
                            MORE PHOTOS
                          </span>
                        </div>
                      )}

                  </div>
                ))}

            </div>

          </div>

        </section>

        {/* =================================================
            PROPERTY DETAILS
        ================================================= */}

        <section className="details-section">

          <div className="details-card">

            <div className="section-heading">
              <div>
                <span className="section-label">
                  PROPERTY DETAILS
                </span>

                <h2>
                  Everything You Need To Know
                </h2>
              </div>
            </div>

            <div className="details-grid">

              <div className="detail-box">
                <span>PROPERTY TYPE</span>
                <strong>
                  {selectedProperty.type}
                </strong>
              </div>

              <div className="detail-box">
                <span>BEDROOMS</span>
                <strong>
                  {selectedProperty.bedrooms} BHK
                </strong>
              </div>

              <div className="detail-box">
                <span>BATHROOMS</span>
                <strong>
                  {selectedProperty.bathrooms}
                </strong>
              </div>

              <div className="detail-box">
                <span>AREA</span>
                <strong>
                  {selectedProperty.area}
                </strong>
              </div>

              <div className="detail-box">
                <span>STATUS</span>
                <strong>
                  {selectedProperty.status}
                </strong>
              </div>

              <div className="detail-box">
                <span>PROPERTY ID</span>
                <strong>
                  {selectedProperty.id}
                </strong>
              </div>

            </div>

          </div>

        </section>

        {/* =================================================
            GALLERY
        ================================================= */}

        <section
          className="gallery-section"
          id="gallery"
        >

          <div className="section-heading centered">

            <span className="section-label">
              VISUAL TOUR
            </span>

            <h2>
              Property Gallery
            </h2>

            <p>
              Browse all {galleryImages.length} images
              of this beautiful property.
            </p>

          </div>

          <div className="property-gallery">

            {galleryImages.map(
              (image, index) => (
                <div
                  className="gallery-card"
                  key={`${selectedProperty.id}-gallery-${index}`}
                  onClick={() =>
                    openImage(index)
                  }
                >

                  <img
                    src={image}
                    alt={`${selectedProperty.title} ${
                      index + 1
                    }`}
                  />

                  <div className="gallery-number">
                    {String(index + 1).padStart(
                      2,
                      "0"
                    )}
                  </div>

                  <div className="gallery-overlay">
                    <span>
                      🔍 View Image
                    </span>
                  </div>

                </div>
              )
            )}

          </div>

        </section>

        {/* =================================================
            VIDEO
        ================================================= */}

        <section className="video-section">

          <div className="video-content">

            <span className="section-label">
              PROPERTY VIDEO
            </span>

            <h2>
              See The Property In Motion
            </h2>

            <p>
              Take a closer look at the
              property, its interiors and
              surroundings.
            </p>

            <button
              className="gold-button"
              onClick={() =>
                setShowVideo(true)
              }
            >
              ▶ Watch Property Video
            </button>

          </div>

          <div
            className="video-preview"
            onClick={() =>
              setShowVideo(true)
            }
          >

            <img
              src={
                galleryImages[1] ||
                galleryImages[0] ||
                fallbackImage
              }
              alt="Property video preview"
            />

            <div className="play-button">
              ▶
            </div>

          </div>

        </section>

        {/* =================================================
            AMENITIES
        ================================================= */}

        <section
          className="amenities-section"
          id="amenities"
        >

          <div className="section-heading centered">

            <span className="section-label">
              LIFESTYLE
            </span>

            <h2>
              Premium Amenities
            </h2>

            <p>
              Everything you need for a comfortable,
              luxurious and modern lifestyle.
            </p>

          </div>

          <div className="amenities-image-grid">

            {selectedProperty.amenities.map(
              (amenity, index) => {

                const amenityImage =
                  getAmenityImage(
                    amenity
                  );

                const icons = [
                  "🏋️",
                  "🏊",
                  "🌳",
                  "🏢",
                  "🚗",
                  "🛡️",
                  "⚡",
                ];

                return (
                  <div
                    className="amenity-image-card"
                    key={`${selectedProperty.id}-${amenity}`}
                  >

                    <div className="amenity-image">

                      <img
                        src={amenityImage}
                        alt={amenity}
                      />

                      <div className="amenity-image-overlay">
                        <span>
                          {icons[index]}
                        </span>
                      </div>

                    </div>

                    <div className="amenity-image-content">

                      <div>

                        <span>
                          PREMIUM FACILITY
                        </span>

                        <h3>
                          {amenity}
                        </h3>

                      </div>

                      <span className="amenity-arrow">
                        →
                      </span>

                    </div>

                  </div>
                );
              }
            )}

          </div>

        </section>

        {/* =================================================
            LOCATION
        ================================================= */}

        <section
          className="location-section"
          id="location"
        >

          <div className="location-content">

            <span className="section-label">
              PRIME LOCATION
            </span>

            <h2>
              Located In The Heart Of
              Bangalore
            </h2>

            <p>
              {selectedProperty.location}
            </p>

            <div className="location-points">

              <div>
                <span>01</span>
                <strong>
                  Excellent Connectivity
                </strong>
              </div>

              <div>
                <span>02</span>
                <strong>
                  Nearby Schools & Hospitals
                </strong>
              </div>

              <div>
                <span>03</span>
                <strong>
                  Shopping & Entertainment
                </strong>
              </div>

              <div>
                <span>04</span>
                <strong>
                  Major IT Hubs Nearby
                </strong>
              </div>

            </div>

          </div>

          <div className="map-card">

            <div className="map-background">
              <div className="map-grid"></div>

              <div className="map-pin">
                📍
              </div>

              <div className="map-location">
                <strong>
                  {selectedProperty.location}
                </strong>

                <span>
                  Prime residential location
                </span>
              </div>
            </div>

          </div>

        </section>

        {/* =================================================
            AGENT
        ================================================= */}

        <section
          className="agent-section"
          id="agent"
        >

          <div className="agent-card">

            <div className="agent-avatar">
              {selectedProperty.agent.name
                .split(" ")
                .map((name) =>
                  name[0]
                )
                .join("")
                .slice(0, 2)}
            </div>

            <div className="agent-info">

              <span className="section-label">
                PROPERTY EXPERT
              </span>

              <h2>
                {selectedProperty.agent.name}
              </h2>

              <p className="agent-role">
                {selectedProperty.agent.role}
              </p>

              <p>
                Get professional guidance,
                property details and
                assistance with your
                purchase journey.
              </p>

              <div className="agent-contact">

                <a
                  href={`tel:${selectedProperty.agent.phone}`}
                >
                  📞{" "}
                  {selectedProperty.agent.phone}
                </a>

                <a
                  href={`mailto:${selectedProperty.agent.email}`}
                >
                  ✉️{" "}
                  {selectedProperty.agent.email}
                </a>

              </div>

            </div>

            <div className="agent-action">

              <a
                href={`tel:${selectedProperty.agent.phone}`}
                className="gold-button"
              >
                Contact Agent
              </a>

            </div>

          </div>

        </section>

        {/* =================================================
            VIRTUAL TOUR
        ================================================= */}

        <section className="virtual-tour-section">

          <div className="virtual-tour-header">

            <div>

              <span className="section-label">
                VIRTUAL EXPERIENCE
              </span>

              <h2>
                Take A Virtual Tour
              </h2>

            </div>

            <p>
              Experience the property remotely
              before visiting in person.
            </p>

          </div>

          <div className="virtual-tour-card">

            <div className="virtual-tour-icon">
              360°
            </div>

            <div>

              <h3>
                Immersive Property Tour
              </h3>

              <p>
                Explore the property from the
                comfort of your home.
              </p>

            </div>

            <a
              href={
                selectedProperty.virtualTour
              }
              target="_blank"
              rel="noreferrer"
              className="outline-button"
            >
              Open Virtual Tour
            </a>

          </div>

        </section>

        {/* =================================================
            SIMILAR PROPERTIES
        ================================================= */}

        <section className="similar-section">

          <div className="section-heading">

            <div>

              <span className="section-label">
                YOU MAY ALSO LIKE
              </span>

              <h2>
                Similar Properties
              </h2>

            </div>

            <p>
              Explore more properties that
              match your requirements.
            </p>

          </div>

          <div className="similar-grid">

            {similarProperties.map(
              (property) => {

                const images =
                  getPropertyImages(
                    property.propertyNumber
                  );

                const image =
                  images[0] ||
                  fallbackImage;

                return (
                  <div
                    className="similar-card"
                    key={property.id}
                    onClick={() =>
                      setSelectedId(
                        property.id
                      )
                    }
                  >

                    <div className="similar-image">

                      <img
                        src={image}
                        alt={property.title}
                      />

                      <span>
                        {property.type}
                      </span>

                    </div>

                    <div className="similar-content">

                      <span className="similar-location">
                        📍 {property.location}
                      </span>

                      <h3>
                        {property.title}
                      </h3>

                      <strong>
                        {property.price}
                      </strong>

                      <div className="similar-meta">

                        <span>
                          🛏{" "}
                          {property.bedrooms}
                          Beds
                        </span>

                        <span>
                          🛁{" "}
                          {property.bathrooms}
                          Baths
                        </span>

                        <span>
                          📐 {property.area}
                        </span>

                      </div>

                    </div>

                  </div>
                );
              }
            )}

          </div>

        </section>

      </main>

      {/* =================================================
          FOOTER
      ================================================= */}

      <footer className="footer">

        <div className="footer-main">

          <div className="footer-brand">

            <div className="brand">
              <div className="brand-mark">
                RE
              </div>

              <div>
                <h2>REAL ESTATE</h2>
                <span>
                  PREMIUM PROPERTIES
                </span>
              </div>
            </div>

            <p>
              Discover beautiful homes,
              premium residences and
              luxury properties across
              Bangalore.
            </p>

          </div>

          <div className="footer-column">

            <h4>PROPERTY</h4>

            <a href="#overview">
              Overview
            </a>

            <a href="#gallery">
              Gallery
            </a>

            <a href="#amenities">
              Amenities
            </a>

            <a href="#location">
              Location
            </a>

          </div>

          <div className="footer-column">

            <h4>CONTACT</h4>

            <span>
              Bangalore, India
            </span>

            <span>
              +91 98765 43210
            </span>

            <span>
              hello@realestate.com
            </span>

          </div>

        </div>

        <div className="footer-bottom">
          <span>
            © 2026 Real Estate. All rights
            reserved.
          </span>

          <span>
            Designed for premium living.
          </span>
        </div>

      </footer>

      {/* =================================================
          IMAGE MODAL
      ================================================= */}

      {selectedImage !== null && (
        <div
          className="image-modal"
          onClick={closeImage}
        >

          <button
            className="modal-close"
            onClick={closeImage}
          >
            ×
          </button>

          <button
            className="modal-prev"
            onClick={(event) => {
              event.stopPropagation();
              previousImage();
            }}
          >
            ‹
          </button>

          <div
            className="modal-image-container"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            <img
              src={
                galleryImages[selectedImage]
              }
              alt="Property enlarged"
            />

            <div className="modal-counter">
              {selectedImage + 1} /{" "}
              {galleryImages.length}
            </div>

          </div>

          <button
            className="modal-next"
            onClick={(event) => {
              event.stopPropagation();
              nextImage();
            }}
          >
            ›
          </button>

        </div>
      )}

      {/* =================================================
          VIDEO MODAL
      ================================================= */}

      {showVideo && (
        <div
          className="video-modal"
          onClick={() =>
            setShowVideo(false)
          }
        >

          <button
            className="modal-close"
            onClick={() =>
              setShowVideo(false)
            }
          >
            ×
          </button>

          <div
            className="video-modal-container"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            <iframe
              src={selectedProperty.virtualTour}
              title="Property Video"
              allowFullScreen
            ></iframe>

          </div>

        </div>
      )}

    </div>
  );
}

export default App;