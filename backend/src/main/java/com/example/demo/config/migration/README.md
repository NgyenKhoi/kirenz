# MongoDB Migrations with Mongock

## Troubleshooting

If collections are not being created, check the following:

### 1. Check Application Logs

Look for Mongock startup messages:
```
INFO  io.mongock.runner - Mongock starting...
INFO  io.mongock.runner - Executing migration: 001-create-chat-collections
INFO  io.mongock.runner - Migration 001: Creating conversations collection
INFO  io.mongock.runner - Created conversations collection
```

### 2. Verify MongoDB Connection

```bash
# Connect to MongoDB
mongosh mongodb://mongodb:eqfleqrd1@localhost:27017/my_db?authSource=my_db

# Check collections
use my_db
show collections

# Should see: mongockLock, mongockChangeLog (even if migrations haven't run yet)
```

### 3. Check Mongock Configuration

Ensure `@EnableMongock` annotation is present in `MongockConfig.java`

### 4. Verify Dependencies

Check that these dependencies are in `pom.xml`:
- `io.mongock:mongock-springboot-v3:5.4.4`
- `io.mongock:mongodb-springdata-v4-driver:5.4.4`

### 5. Manual Verification

After application starts, check MongoDB:

```javascript
use my_db

// Check if Mongock ran
db.mongockChangeLog.find().pretty()

// Check collections
show collections

// Check indexes
db.conversations.getIndexes()
db.messages.getIndexes()
```

### 6. Force Migration

If migrations don't run automatically, check:
- `mongock.enabled=true` in application.yml
- MongoDB is running and accessible
- No errors in application logs

### 7. Common Issues

**Issue**: "Cannot obtain lock"
**Solution**: Delete `mongockLock` collection and restart

```javascript
db.mongockLock.drop()
```

**Issue**: Migrations already ran but collections not visible
**Solution**: Check you're connected to the correct database

```javascript
db.getName()  // Should show: my_db
```

**Issue**: No Mongock logs in application output
**Solution**: 
1. Verify `@EnableMongock` annotation exists
2. Check `mongock.enabled=true` in application.yml
3. Ensure migration classes are in correct package

## Expected Behavior

When application starts successfully:
1. Mongock logs appear showing migration execution
2. `mongockChangeLog` collection is created
3. `mongockLock` collection is created
4. `conversations` collection is created with indexes
5. `messages` collection is created with indexes

## Verification Commands

```javascript
// Connect to MongoDB
use my_db

// 1. Check migration history
db.mongockChangeLog.find().pretty()
// Should show 2 entries (001 and 002)

// 2. List all collections
show collections
// Should show: conversations, messages, mongockChangeLog, mongockLock, posts, comments

// 3. Verify conversations indexes
db.conversations.getIndexes()
// Should show: _id_, idx_conversations_participantIds, idx_conversations_status_updatedAt

// 4. Verify messages indexes
db.messages.getIndexes()
// Should show: _id_, idx_messages_conversationId_sentAt, idx_messages_senderId_sentAt, idx_messages_status
```
