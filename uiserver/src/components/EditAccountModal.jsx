import { useState, useRef } from "react";
import { updateUserInfo } from "../api/auth";
import { useAuth } from "../context/AuthContext";
import { useAsyncError } from "react-router-dom";

function EditAccountModal({ onClose, accentColor }) {
  const { user, saveUser } = useAuth();

  //prefill form with current values
  const [form, setForm] = useState({
    firstName: user?.firstName || "",
    lastName: user?.lastName || "",
    email: user?.email || "",
    username: user?.username || "",
    profilePicture: user?.profilePicture || "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [imagePreview, setImagePreview] = useState(
    user?.profilePicture || null,
  );
  const fileInputRef = useRef(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  //convert & store image to Base64
  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Only allow image files
    if (!file.type.startsWith("image/")) {
      setError("Please select a valid image file.");
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setForm((prev) => ({ ...prev, profilePicture: reader.result }));
      setImagePreview(reader.result);
    };
    reader.readAsDataURL(file);
  };

  const handleSave = async () => {
    setError("");
    if (!form.firstName.trim() || !form.lastName.trim()) {
      setError("First and last name are required.");
      return;
    }
    if (!form.username.trim()) {
      setError("Username is required.");
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(form.email.trim())) {
      setError("Please enter a valid email address.");
      return;
    }

    setSaving(true);
    try {
      const updatedUser = await updateUserInfo({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        username: form.username.trim(),
        profilePicture: form.profilePicture,
      });

      saveUser({ ...user, ...updatedUser });
      onClose();
    } catch (err) {
      setError(err.message || "Failed to save changes.");
    } finally {
      setSaving(false);
    }
  };

  //close modal
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  const inputStyle = {
    widht: "100%",
    padding: "8px 0",
    border: "none",
    borderBottom: `2px solid ${accentColor}`,
    backgroundColor: "transparent",
    fontSize: "15px",
    color: "#222",
    outline: "none",
    boxSizing: "border-box",
    marginTop: "4px",
  };

  const labelStyle = {
    fontSize: "11px",
    fontWeight: "700",
    letterSpacing: "0.07em",
    textTransform: "uppercase",
    color: accentColor,
    display: "block",
  };
  return (
    <div style={styles.backdrop} onClick={handleBackdropClick}>
      <div style={styles.modal}>
        {/* Header */}
        <div style={styles.header}>
          <h2 style={styles.title}>Edit Account</h2>
          <button style={styles.closeBtn} onClick={onClose} aria-label="Close">
            x
          </button>
        </div>

        {/* Error banner */}
        {error && <div style={styles.error}>{error}</div>}

        {/* Profile picture */}
        <div style={styles.avatarRow}>
          <div style={{ ...styles.avatarPreview, borderColor: accentColor }}>
            {imagePreview ? (
              <img src={imagePreview} alt="Profile" style={styles.avatarImg} />
            ) : (
              <span style={styles.avatarInitials}>
                {(form.firstName?.charAt(0) || "") +
                  (form.lastName?.charAt(0) || "")}
              </span>
            )}
          </div>
          <div>
            <button
              style={{
                ...styles.uploadBtn,
                borderColor: accentColor,
                color: accentColor,
              }}
              onClick={() => fileInputRef.current.click()}
              disabled={saving}
            >
              Change Photo
            </button>
            {imagePreview && (
              <button
                style={styles.removeBtn}
                onClick={() => {
                  setForm((prev) => ({ ...prev, profilePicture: null }));
                  setImagePreview(null);
                }}
                disabled={saving}
              >
                Remove
              </button>
            )}
            <input
              type="file"
              accept="image/*"
              ref={fileInputRef}
              style={{ display: "none" }}
              onChange={handleImageChange}
            />
          </div>
        </div>

        {/* Name row — side by side */}
        <div style={styles.nameRow}>
          <div style={{ flex: 1 }}>
            <label style={labelStyle}>First Name</label>
            <input
              name="firstName"
              value={form.firstName}
              onChange={handleChange}
              placeholder="First name"
              style={inputStyle}
              disabled={saving}
            />
          </div>
          <div style={{ flex: 1 }}>
            {" "}
            <label style={labelStyle}>Last Name</label>
            <input
              name="lastName"
              value={form.lastName}
              onChange={handleChange}
              placeholder="Last name"
              style={inputStyle}
              disabled={saving}
            />
          </div>
        </div>

        {/* Username */}
        <div style={styles.fieldGroup}>
          <label style={labelStyle}>Username</label>
          <input
            name="username"
            value={form.username}
            onChange={handleChange}
            placeholder="username"
            style={inputStyle}
            disabled={saving}
          />
        </div>

        {/* Email */}
        <div style={styles.fieldGroup}>
          <label style={labelStyle}>Email</label>
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            placeholder="email@example.com"
            style={inputStyle}
            disabled={saving}
          />
        </div>

        {/* Action buttons */}
        <div style={styles.buttonRow}>
          <button style={styles.cancelBtn} onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button
            style={{
              ...styles.saveBtn,
              backgroundColor: accentColor,
              opacity: saving ? 0.7 : 1,
            }}
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? "Saving…" : "Save Changes"}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  backdrop: {
    position: "fixed",
    inset: 0,
    backgroundColor: "rgba(0,0,0,0.45)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
  },
  modal: {
    backgroundColor: "white",
    borderRadius: "12px",
    padding: "32px",
    width: "100%",
    maxWidth: "480px",
    boxShadow: "0 8px 32px rgba(0,0,0,0.18)",
    margin: "0 16px",
    maxHeight: "90vh",
    overflowY: "auto",
  },
  header: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: "20px",
  },
  title: {
    fontSize: "20px",
    fontWeight: "700",
    color: "#222",
    margin: 0,
  },
  closeBtn: {
    background: "none",
    border: "none",
    fontSize: "18px",
    color: "#888",
    cursor: "pointer",
    padding: "4px 8px",
  },
  error: {
    backgroundColor: "#fee",
    color: "#c00",
    padding: "10px 14px",
    borderRadius: "6px",
    marginBottom: "20px",
    fontSize: "14px",
  },
  avatarRow: {
    display: "flex",
    alignItems: "center",
    gap: "20px",
    marginBottom: "24px",
  },
  avatarPreview: {
    width: "72px",
    height: "72px",
    borderRadius: "50%",
    border: "3px solid",
    overflow: "hidden",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f0f0f0",
    flexShrink: 0,
  },
  avatarImg: {
    width: "100%",
    height: "100%",
    objectFit: "cover",
  },
  avatarInitials: {
    fontSize: "24px",
    fontWeight: "700",
    color: "#888",
  },
  uploadBtn: {
    background: "none",
    border: "2px solid",
    borderRadius: "6px",
    padding: "6px 14px",
    fontSize: "13px",
    fontWeight: "600",
    cursor: "pointer",
    display: "block",
    marginBottom: "8px",
  },
  removeBtn: {
    background: "none",
    border: "none",
    color: "#c00",
    fontSize: "13px",
    cursor: "pointer",
    padding: 0,
  },
  nameRow: {
    display: "flex",
    gap: "20px",
    marginBottom: "24px",
  },
  fieldGroup: {
    marginBottom: "24px",
  },
  buttonRow: {
    display: "flex",
    justifyContent: "flex-end",
    gap: "12px",
    marginTop: "8px",
  },
  cancelBtn: {
    padding: "10px 22px",
    backgroundColor: "white",
    color: "#555",
    border: "2px solid #ddd",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  saveBtn: {
    padding: "10px 22px",
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
};

export default EditAccountModal;
