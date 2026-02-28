module.exports = {
  // For local MongoDB
  HOST: "localhost",
  PORT: 27017,
  DB: "app",

  // For MongoDB Atlas - set via environment variable (see ../.env.example)
  // Keep empty by default to avoid committing credentials.
  ATLAS_URI: ""
};
