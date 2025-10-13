/**
 * Migration script to normalize roles and add domainId to users
 * Uses the generator's existing mongoose connection and models
 * 
 * Usage:
 * - Dry run: node migration-normalize-roles.js --dry-run
 * - Apply:   node migration-normalize-roles.js --apply
 */

const dbConfig = require("./app/config/db.config");
const db = require("./app/models");
const Role = db.role;
const User = db.user;

// Role mapping from old names to canonical names
const ROLE_MAPPING = {
  'user': 'BUSINESS_USER',
  'moderator': 'DOMAIN_ADMIN', 
  'admin': 'APP_ADMIN'
};

const CANONICAL_ROLES = [
  'BUSINESS_OWNER',
  'DOMAIN_ADMIN', 
  'APP_ADMIN',
  'BUSINESS_USER'
];

async function main() {
  const isDryRun = process.argv.includes('--dry-run');
  const isApply = process.argv.includes('--apply');
  
  if (!isDryRun && !isApply) {
    console.log('Usage: node migration-normalize-roles.js [--dry-run|--apply]');
    process.exit(1);
  }

  console.log(`\n=== Role Normalization Migration ${isDryRun ? '(DRY RUN)' : '(APPLY)'} ===\n`);

  try {
    // Connect to MongoDB using generator's config
    await db.mongoose.connect(`mongodb://${dbConfig.HOST}:${dbConfig.PORT}/${dbConfig.DB}`, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log("Successfully connected to MongoDB.");
    
    if (isDryRun) {
      await dryRun();
    } else {
      await backup();
      await apply();
    }
    
  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  } finally {
    await db.mongoose.disconnect();
    console.log('Database connection closed.');
  }
}

async function dryRun() {
  console.log('📊 ANALYSIS PHASE\n');
  
  // Analyze roles collection
  const roles = await Role.find({});
  console.log(`Current roles: ${roles.length}`);
  roles.forEach(role => {
    console.log(`  - ${role.name} (id: ${role._id})`);
  });
  
  // Analyze users collection
  const users = await User.find({});
  console.log(`\nCurrent users: ${users.length}`);
  
  let usersWithoutDomain = 0;
  let userRoleCounts = {};
  
  for (const user of users) {
    if (!user.domainId) usersWithoutDomain++;
    
    if (user.roles && user.roles.length > 0) {
      for (const roleId of user.roles) {
        const role = roles.find(r => r._id.toString() === roleId.toString());
        if (role) {
          const roleName = role.name;
          userRoleCounts[roleName] = (userRoleCounts[roleName] || 0) + 1;
        }
      }
    }
  }
  
  console.log(`\nUsers without domainId: ${usersWithoutDomain}`);
  console.log('User role distribution:');
  Object.entries(userRoleCounts).forEach(([role, count]) => {
    const mapped = ROLE_MAPPING[role] || role;
    console.log(`  - ${role} -> ${mapped}: ${count} users`);
  });
  
  // Check if we have a domains collection (may not exist in current generator)
  let globalDomainExists = false;
  try {
    const collections = await db.mongoose.connection.db.listCollections().toArray();
    const hasDomainsCollection = collections.some(col => col.name === 'domains');
    if (hasDomainsCollection) {
      const globalDomain = await db.mongoose.connection.db.collection('domains').findOne({ name: 'global' });
      globalDomainExists = !!globalDomain;
    }
  } catch (e) {
    // domains collection doesn't exist
  }
  
  console.log(`\nGlobal domain exists: ${globalDomainExists ? 'Yes' : 'No'}`);
  
  console.log('\n📋 PLANNED CHANGES:');
  console.log('1. Create global domain if missing');
  console.log('2. Create/update canonical role documents');
  console.log('3. Update user role references to canonical roles');
  console.log('4. Set domainId for users without one');
  console.log('\n✅ Dry run complete. Run with --apply to execute changes.');
}

async function backup() {
  console.log('💾 Creating backup verification...');
  
  const roleCount = await Role.countDocuments();
  const userCount = await User.countDocuments();
  
  console.log(`Backup verification - Roles: ${roleCount}, Users: ${userCount}`);
  console.log('💾 Backup noted (use mongodump for production backups)');
}

async function apply() {
  console.log('🚀 APPLYING CHANGES\n');
  
  // 1. Create global domain (basic document in domains collection)
  console.log('1. Creating global domain...');
  let globalDomainId;
  try {
    const domainsCollection = db.mongoose.connection.db.collection('domains');
    
    const existingGlobal = await domainsCollection.findOne({ name: 'global' });
    if (existingGlobal) {
      globalDomainId = existingGlobal._id.toString();
      console.log(`✅ Global domain already exists: ${globalDomainId}`);
    } else {
      const result = await domainsCollection.insertOne({
        name: 'global',
        ownerUserId: null,
        createdAt: new Date(),
        metadata: { description: 'Default global domain for users without specific domain' }
      });
      globalDomainId = result.insertedId.toString();
      console.log(`✅ Created global domain: ${globalDomainId}`);
    }
  } catch (error) {
    console.error('Error creating global domain:', error);
    // Continue without domain feature if domains collection doesn't exist
    globalDomainId = 'global'; // fallback string
  }
  
  // 2. Create canonical role documents
  console.log('\n2. Creating canonical roles...');
  for (const roleName of CANONICAL_ROLES) {
    const existingRole = await Role.findOne({ name: roleName });
    if (!existingRole) {
      const role = new Role({ name: roleName });
      await role.save();
      console.log(`✅ Created role: ${roleName}`);
    } else {
      console.log(`✅ Role already exists: ${roleName}`);
    }
  }
  
  // 3. Get role mapping for ObjectId references
  const roleMap = {};
  const allRoles = await Role.find({});
  
  // Map old role names to new canonical role ObjectIds
  for (const [oldName, newName] of Object.entries(ROLE_MAPPING)) {
    const canonicalRole = allRoles.find(r => r.name === newName);
    if (canonicalRole) {
      const oldRole = allRoles.find(r => r.name === oldName);
      if (oldRole) {
        roleMap[oldRole._id.toString()] = canonicalRole._id;
        console.log(`📝 Role mapping: ${oldName} (${oldRole._id}) -> ${newName} (${canonicalRole._id})`);
      }
    }
  }
  
  // 4. Update users
  console.log('\n3. Updating users...');
  const users = await User.find({});
  let updatedUsers = 0;
  
  for (const user of users) {
    let needsUpdate = false;
    const updates = {};
    
    // Set domainId if missing
    if (!user.domainId) {
      updates.domainId = globalDomainId;
      needsUpdate = true;
    }
    
    // Update role references
    if (user.roles && user.roles.length > 0) {
      const newRoles = user.roles.map(roleId => {
        const roleIdStr = roleId.toString();
        if (roleMap[roleIdStr]) {
          needsUpdate = true;
          return roleMap[roleIdStr];
        }
        return roleId;
      });
      
      if (needsUpdate) {
        updates.roles = newRoles;
      }
    }
    
    if (needsUpdate) {
      await User.updateOne({ _id: user._id }, { $set: updates });
      updatedUsers++;
      console.log(`✅ Updated user: ${user.username || user._id}`);
    }
  }
  
  console.log(`\n✅ Updated ${updatedUsers} users`);
  
  // 5. Clean up old roles (optional)
  console.log('\n4. Cleaning up old roles...');
  const oldRoleNames = Object.keys(ROLE_MAPPING);
  const deleteResult = await Role.deleteMany({ name: { $in: oldRoleNames } });
  console.log(`✅ Deleted ${deleteResult.deletedCount} old role documents`);
  
  console.log('\n🎉 Migration completed successfully!');
  
  // Show final counts
  const finalRoleCount = await Role.countDocuments();
  const finalUserCount = await User.countDocuments();
  console.log(`\nFinal counts - Roles: ${finalRoleCount}, Users: ${finalUserCount}`);
}

if (require.main === module) {
  main();
}

module.exports = { main, dryRun, apply };