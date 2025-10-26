const dbConfig = require("./app/config/db.config");
const db = require("./app/models");
const Role = db.role;
const User = db.user;
const Domain = db.domain;

// Use MongoDB Atlas connection string
const mongoUri = process.env.MONGODB_ATLAS_URI || dbConfig.ATLAS_URI;

async function clearAndSeed() {
  try {
    await db.mongoose.connect(mongoUri, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log("Successfully connected to MongoDB.");

    // Clear existing data
    console.log("Clearing existing data...");
    await Role.deleteMany({});
    await User.deleteMany({});
    if (Domain) {
      await Domain.deleteMany({});
    }
    console.log("Existing data cleared.");

    // Create roles with correct structure
    console.log("Creating roles...");
    const roles = [
      { name: "ROLE_BUSINESS_OWNER", roleName: "BUSINESS_OWNER" },
      { name: "ROLE_DOMAIN_ADMIN", roleName: "DOMAIN_ADMIN" },
      { name: "ROLE_APP_ADMIN", roleName: "APP_ADMIN" },
      { name: "ROLE_BUSINESS_USER", roleName: "BUSINESS_USER" }
    ];

    for (const roleData of roles) {
      const role = new Role(roleData);
      await role.save();
      console.log(`Added '${roleData.roleName}' to roles collection`);
    }

    // Create global domain
    if (Domain) {
      console.log("Creating global domain...");
      const domain = new Domain({
        name: "global",
        ownerUserId: null,
        createdAt: new Date(),
        metadata: { description: "Default global domain for users without specific domain" }
      });
      await domain.save();
      console.log("Added 'global' domain to domains collection");
    }

    // Create admin user
    console.log("Creating admin user...");
    const bcrypt = require("bcryptjs");
    const appAdminRole = await Role.findOne({ roleName: "APP_ADMIN" });
    
    if (appAdminRole) {
      const adminUser = new User({
        username: "admin",
        email: "admin@example.com",
        password: bcrypt.hashSync("password", 8),
        roles: [appAdminRole._id]
      });
      await adminUser.save();
      console.log("Added 'admin' user with APP_ADMIN role");
    } else {
      console.log("APP_ADMIN role not found, skipping user creation");
    }

    console.log("Database seeding completed successfully!");
    
  } catch (error) {
    console.error("Error:", error);
  } finally {
    await db.mongoose.disconnect();
    console.log("Database connection closed.");
  }
}

clearAndSeed();
