import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import "./App.css";

const fallbackImage =
  "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=80";

const API_BASE = "http://localhost:8080/api/properties";

function PropertyDetails() {
  const { propertyId } = useParams();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedImage, setSelectedImage] = useState(null);
  const [showVideo, setShowVideo] = useState(false);

  useEffect(() => {
    fetch(`${API_BASE}/${propertyId}/details`)
      .then((res) => {
        if (!res.ok) throw new Error("Property not found");
        return res.json();
      })
      .then((json) => {
        setData(json);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [propertyId]);

  if (loading) return <div className="app"><p style={{ padding: 40 }}>Loading...</p></div>;
  if (error) return <div className="app"><p style={{ padding: 40 }}>Error: {error}</p></div>;
  if (!data) return null;

  const { property, images, amenities, tour, similar } = data;

  const galleryImages = images.length > 0 ? images.map((img) => img.imageUrl) : [fallbackImage];

  const openImage = (index) => setSelectedImage(index);
  const closeImage = () => setSelectedImage(null);
  const nextImage = () =>
    setSelectedImage((current) => (current === null ? 0 : (current + 1) % galleryImages.length));
  const previousImage = () =>
    setSelectedImage((current) =>
      current === null ? 0 : (current - 1 + galleryImages.length) % galleryImages.length
    );

  return (
    <div className="app">
      {/* NAVBAR */}
      <header className="navbar">
        <div className="nav-inner">
          <div className="brand">
            <div className="brand-mark">RE</div>
            <div>
              <h2>REAL ESTATE</h2>
              <span>PREMIUM PROPERTIES</span>
            </div>
          </div>
          <nav className="nav-links">
            <a href="#overview">Overview</a>
            <a href="#gallery">Gallery</a>
            <a href="#amenities">Amenities</a>
          </nav>
        </div>
      </header>

      <main>
        {/* HERO */}
        <section className="hero-section" id="overview">
          <div className="hero-content">
            <div className="hero-label"><span></span>FEATURED PROPERTY</div>
            <h1>{property.title}</h1>
            <p className="hero-location">📍 {property.city || property.address || "Location not set"}</p>
            <p className="hero-description">{property.description}</p>
            <div className="hero-price-row">
              <div>
                <span className="price-label">PROPERTY PRICE</span>
                <strong>₹{property.price}</strong>
              </div>
              <div className="hero-status">{property.status}</div>
            </div>
            <div className="hero-stats">
              <div><strong>{property.bedrooms ?? "-"}</strong><span>Bedrooms</span></div>
              <div><strong>{property.bathrooms ?? "-"}</strong><span>Bathrooms</span></div>
              <div><strong>{property.area ?? "-"}</strong><span>Built-up Area</span></div>
            </div>
          </div>
          <div className="hero-accent">
            <div><span>PROPERTY ID</span><strong>{property.id}</strong></div>
            <div><span>PROPERTY TYPE</span><strong>{property.propertyType}</strong></div>
          </div>
        </section>

        {/* SHOWCASE */}
        <section className="showcase-section">
          <div className="section-heading">
            <div>
              <span className="section-label">PROPERTY VIEW</span>
              <h2>Explore The Residence</h2>
            </div>
          </div>
          <div className="main-property-showcase">
            <div className="showcase-main" onClick={() => openImage(0)}>
              <img src={galleryImages[0]} alt={`${property.title} main`} />
              <span className="property-badge">FOR SALE</span>
              <span className="image-count">📷 {galleryImages.length} Photos</span>
              <div className="showcase-view">🔍 View Gallery</div>
            </div>
            <div className="showcase-side">
              {galleryImages.slice(1, 5).map((image, index) => (
                <div className="showcase-small" key={index} onClick={() => openImage(index + 1)}>
                  <img src={image} alt={`${property.title} ${index + 2}`} />
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* GALLERY */}
        <section className="gallery-section" id="gallery">
          <div className="section-heading centered">
            <span className="section-label">VISUAL TOUR</span>
            <h2>Property Gallery</h2>
            <p>Browse all {galleryImages.length} images of this beautiful property.</p>
          </div>
          <div className="property-gallery">
            {galleryImages.map((image, index) => (
              <div className="gallery-card" key={index} onClick={() => openImage(index)}>
                <img src={image} alt={`${property.title} ${index + 1}`} />
                <div className="gallery-number">{String(index + 1).padStart(2, "0")}</div>
                <div className="gallery-overlay"><span>🔍 View Image</span></div>
              </div>
            ))}
          </div>
        </section>

        {/* VIDEO */}
        {tour?.videoUrl && (
          <section className="video-section">
            <div className="video-content">
              <span className="section-label">PROPERTY VIDEO</span>
              <h2>See The Property In Motion</h2>
              <button className="gold-button" onClick={() => setShowVideo(true)}>▶ Watch Property Video</button>
            </div>
            <div className="video-preview" onClick={() => setShowVideo(true)}>
              <img src={galleryImages[1] || galleryImages[0]} alt="Property video preview" />
              <div className="play-button">▶</div>
            </div>
          </section>
        )}

        {/* AMENITIES */}
        <section className="amenities-section" id="amenities">
          <div className="section-heading centered">
            <span className="section-label">LIFESTYLE</span>
            <h2>Premium Amenities</h2>
          </div>
          <div className="amenities-image-grid">
            {amenities.map((amenity) => (
              <div className="amenity-image-card" key={amenity.id}>
                <div className="amenity-image">
                  <img src={fallbackImage} alt={amenity.amenityName} />
                </div>
                <div className="amenity-image-content">
                  <div>
                    <span>PREMIUM FACILITY</span>
                    <h3>{amenity.amenityName}</h3>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* VIRTUAL TOUR */}
        {tour?.virtualTourUrl && (
          <section className="virtual-tour-section">
            <div className="virtual-tour-header">
              <div>
                <span className="section-label">VIRTUAL EXPERIENCE</span>
                <h2>Take A Virtual Tour</h2>
              </div>
            </div>
            <div className="virtual-tour-card">
              <div className="virtual-tour-icon">360°</div>
              <div>
                <h3>Immersive Property Tour</h3>
              </div>
              <a href={tour.virtualTourUrl} target="_blank" rel="noreferrer" className="outline-button">
                Open Virtual Tour
              </a>
            </div>
          </section>
        )}

        {/* SIMILAR */}
        <section className="similar-section">
          <div className="section-heading">
            <div>
              <span className="section-label">YOU MAY ALSO LIKE</span>
              <h2>Similar Properties</h2>
            </div>
          </div>
          <div className="similar-grid">
            {similar.map((p) => (
              <div className="similar-card" key={p.id}>
                <div className="similar-image">
                  <img src={fallbackImage} alt={p.title} />
                  <span>{p.propertyType}</span>
                </div>
                <div className="similar-content">
                  <span className="similar-location">📍 {p.city || "Bangalore"}</span>
                  <h3>{p.title}</h3>
                  <strong>₹{p.price}</strong>
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* IMAGE MODAL */}
      {selectedImage !== null && (
        <div className="image-modal" onClick={closeImage}>
          <button className="modal-close" onClick={closeImage}>×</button>
          <button className="modal-prev" onClick={(e) => { e.stopPropagation(); previousImage(); }}>‹</button>
          <div className="modal-image-container" onClick={(e) => e.stopPropagation()}>
            <img src={galleryImages[selectedImage]} alt="Property enlarged" />
            <div className="modal-counter">{selectedImage + 1} / {galleryImages.length}</div>
          </div>
          <button className="modal-next" onClick={(e) => { e.stopPropagation(); nextImage(); }}>›</button>
        </div>
      )}

      {/* VIDEO MODAL */}
      {showVideo && (
        <div className="video-modal" onClick={() => setShowVideo(false)}>
          <button className="modal-close" onClick={() => setShowVideo(false)}>×</button>
          <div className="video-modal-container" onClick={(e) => e.stopPropagation()}>
            <iframe src={tour.videoUrl} title="Property Video" allowFullScreen></iframe>
          </div>
        </div>
      )}
    </div>
  );
}

export default PropertyDetails;