const express = require("express");
const cors = require("cors");
const cookieSession = require("cookie-session");

const dbConfig = require("./app/config/db.config");

const app = express();

var corsOptions = {
  origin: "http://localhost:8081"
};

app.use(cors(corsOptions));

// parse requests of content-type - application/json
app.use(express.json());

// parse requests of content-type - application/x-www-form-urlencoded
app.use(express.urlencoded({ extended: true }));

app.use(
  cookieSession({
    name: "bezkoder-session",
    secret: "COOKIE_SECRET", // should use as secret environment variable
    httpOnly: true
  })
);

const db = require("./app/models");
const Role = db.role;
const Domain = db.domain; // Assuming Domain model exists

db.mongoose
  .connect(`mongodb://${dbConfig.HOST}:${dbConfig.PORT}/${dbConfig.DB}`, {
    useNewUrlParser: true,
    useUnifiedTopology: true
  })
  .then(() => {
    console.log("Successfully connect to MongoDB.");
    initial();
  })
  .catch(err => {
    console.error("Connection error", err);
    process.exit();
  });

// simple route
app.get("/", (req, res) => {
  res.json({ message: "Welcome to bezkoder application." });
});

// routes
require("./app/routes/auth.routes")(app);
require("./app/routes/user.routes")(app);

// set port, listen for requests
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}.`);
});

function initial() {
  Role.estimatedDocumentCount((err, count) => {
    if (!err && count === 0) {
      // Canonical roles for Adaptive BP
      new Role({
        name: "BUSINESS_OWNER"
      }).save(err => {
        if (err) {
          console.log("error", err);
        }
        console.log("added 'BUSINESS_OWNER' to roles collection");
      });

      new Role({
        name: "DOMAIN_ADMIN"
      }).save(err => {
        if (err) {
          console.log("error", err);
        }
        console.log("added 'DOMAIN_ADMIN' to roles collection");
      });

      new Role({
        name: "APP_ADMIN"
      }).save(err => {
        if (err) {
          console.log("error", err);
        }
        console.log("added 'APP_ADMIN' to roles collection");
      });

      new Role({
        name: "BUSINESS_USER"
      }).save(err => {
        if (err) {
          console.log("error", err);
        }
        console.log("added 'BUSINESS_USER' to roles collection");
      });
    }
  });

  // Create global domain if it doesn't exist
  if (Domain) {
    Domain.estimatedDocumentCount((err, count) => {
      if (!err && count === 0) {
        new Domain({
          name: "global",
          ownerUserId: null,
          createdAt: new Date(),
          metadata: { description: "Default global domain for users without specific domain" }
        }).save(err => {
          if (err) {
            console.log("error creating global domain", err);
          }
          console.log("added 'global' domain to domains collection");
        });
      }
    });
  }
}
