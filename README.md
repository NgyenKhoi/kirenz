# Kirenz - Dự án tự học Full Stack

Dự án mạng xã hội để học và thực hành các công nghệ hiện đại:
- **Backend**: Spring Boot + PostgreSQL + MongoDB + RabbitMQ + WebSocket
- **Frontend**: React + TypeScript + Tailwind CSS
- **Features**: Authentication, Posts/Comments, Real-time Chat, File Upload

## Quick Setup

### 1. Prerequisites
```bash
# Java 21
java -version

# Node.js 18+
node --version

# Docker
docker --version
```

### 2. Setup Databases

**PostgreSQL:**
```bash
# Install & start PostgreSQL
# Windows: choco install postgresql
# macOS: brew install postgresql && brew services start postgresql
# Ubuntu: sudo apt install postgresql postgresql-contrib

# Create database (Liquibase sẽ tự động tạo tables và import data)
psql -U postgres -c "CREATE DATABASE kirenz_db;"
```

**MongoDB:**
```bash
# Install & start MongoDB
# Windows: choco install mongodb
# macOS: brew install mongodb-community && brew services start mongodb-community
# Ubuntu: sudo apt install mongodb-org

# Tạo database và user root
mongosh
use kirenz_db
db.createUser({
  user: "mongodb",
  pwd: "your_mongodb_password",
  roles: [{ role: "dbOwner", db: "kirenz_db" }]
})
exit

# Import data (Mongock sẽ tự động import khi chạy ứng dụng)
# Hoặc import thủ công:
mongoimport --db kirenz_db --collection posts --file backend/src/main/resources/mongo/posts.json --jsonArray
mongoimport --db kirenz_db --collection comments --file backend/src/main/resources/mongo/comments.json --jsonArray
```

### 3. Configure Environment
```bash
cp .env.example .env
# Edit .env với password thực tế của database
```

**Cập nhật file .env:**
```env
DATABASE_PASSWORD=your_postgres_password
MONGODB_PASSWORD=your_mongodb_password
```

### 4. Run Application

**Option A: Docker Compose (Recommended)**
```bash
docker-compose up --build
```

**Option B: Development Mode**
```bash
# Terminal 1: RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management

# Terminal 2: Backend
source ./scripts/load-env.sh  # Linux/macOS
# .\scripts\load-env.ps1      # Windows
cd backend && ./mvnw spring-boot:run

# Terminal 3: Frontend
cd frontend && npm install && npm run dev
```

## Access

- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080/api
- **RabbitMQ**: http://localhost:15672 (guest/guest)

## Test Accounts

Tất cả accounts có password: `password123`

| Email | Password |
|-------|----------|
| sarah.johnson@example.com | password123 |
| mike.chen@example.com | password123 |
| emma.wilson@example.com | password123 |

## Lưu ý

- **Liquibase** tự động tạo tables và seed data cho PostgreSQL khi chạy lần đầu
- **Mongock** tự động import data cho MongoDB khi chạy lần đầu
- Chỉ cần tạo empty database, migration sẽ lo phần còn lại
- File `DATABASE.sql` và `SEED_DATA.sql` chỉ để tham khảo, không cần chạy thủ công

## Chạy development mode (không dùng Docker)

### 1. RabbitMQ (Docker):
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

### 2. Backend:

#### Windows (PowerShell):
```powershell
cd backend

# Set environment variables
$env:SERVER_PORT="8080"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/kirenz_db"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your_postgres_password"
$env:MONGODB_HOST="localhost"
$env:MONGODB_PORT="27017"
$env:MONGODB_DATABASE="kirenz_db"
$env:MONGODB_USERNAME="your_mongodb_username"
$env:MONGODB_PASSWORD="your_mongodb_password"
$env:MONGODB_AUTH_DATABASE="kirenz_db"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:WEBSOCKET_ALLOWED_ORIGINS="http://localhost:3000"
$env:JWT_SECRET="kirenz-super-secret-jwt-key-for-development-only-change-in-production"
$env:JWT_ACCESS_TOKEN_EXPIRATION="900000"
$env:JWT_REFRESH_TOKEN_EXPIRATION="604800000"
$env:CLOUDINARY_CLOUD_NAME="demo_cloud"
$env:CLOUDINARY_API_KEY="demo_key"
$env:CLOUDINARY_API_SECRET="demo_secret"

# Chạy Spring Boot
./mvnw spring-boot:run
```

#### Command prompt CMD:
```bash
cd backend

# Set environment variables in command prompt
set SERVER_PORT=8080
set DATABASE_URL=jdbc:postgresql://localhost:5432/kirenz_db
set DATABASE_USERNAME=postgres
set DATABASE_PASSWORD=your_postgres_password
set MONGODB_HOST=localhost
set MONGODB_PORT=27017
set MONGODB_DATABASE=kirenz_db
set MONGODB_USERNAME=your_mongodb_username
set MONGODB_PASSWORD=your_mongodb_password
set MONGODB_AUTH_DATABASE=kirenz_db
set RABBITMQ_HOST=localhost
set RABBITMQ_PORT=5672
set RABBITMQ_USERNAME=guest
set RABBITMQ_PASSWORD=guest
set KAFKA_BOOTSTRAP_SERVERS=localhost:9092
set WEBSOCKET_ALLOWED_ORIGINS=http://localhost:3000
set JWT_SECRET=kirenz-super-secret-jwt-key-for-development-only-change-in-production
set JWT_ACCESS_TOKEN_EXPIRATION=900000
set JWT_REFRESH_TOKEN_EXPIRATION=604800000
set CLOUDINARY_CLOUD_NAME=demo_cloud
set CLOUDINARY_API_KEY=demo_key
set CLOUDINARY_API_SECRET=demo_secret

# Chạy Spring Boot
./mvnw spring-boot:run
```

#### Sử dụng helper scripts (đơn giản nhất):

**Windows (PowerShell)**:
```powershell
# Tạo file .env từ .env.example và cập nhật thông tin
cp .env.example .env

# Load environment variables từ .env
.\scripts\load-env.ps1

# Chạy backend
cd backend
.\mvnw spring-boot:run
```

**macOS/Linux (Bash)**:
```bash
# Tạo file .env từ .env.example và cập nhật thông tin
cp .env.example .env

# Load environment variables từ .env
source ./scripts/load-env.sh

# Chạy backend
cd backend
./mvnw spring-boot:run
```

**Lưu ý**: 
- Cập nhật thông tin database trong file `.env` trước khi chạy
- Scripts sẽ tự động load tất cả environment variables từ file `.env`
- Nếu không muốn dùng scripts, bạn có thể set environment variables thủ công như hướng dẫn ở trên

### 3. Frontend:
```bash
cd frontend
# Cài đặt dependencies
npm install
# Chạy development server
npm run dev
```

## Cấu trúc dự án

```
├── backend/                 # Spring Boot application
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration files
│   ├── Dockerfile          # Backend Docker image
│   └── pom.xml            # Maven dependencies
├── frontend/               # React application
│   ├── src/               # React source code
│   ├── Dockerfile         # Frontend Docker image
│   └── package.json       # NPM dependencies
├── docker-compose.yml     # Docker services configuration
└── README.md             # This file
```

## Tính năng chính

- **Authentication**: JWT-based authentication với refresh tokens
- **Real-time Chat**: WebSocket + RabbitMQ cho messaging
- **Hybrid Database**: PostgreSQL cho user data, MongoDB cho posts/comments
- **File Upload**: Cloudinary integration cho media files
- **Premium Features**: Role-based access control

## Troubleshooting

### 1. Database connection issues:
- Kiểm tra PostgreSQL và MongoDB đã chạy
- Xác nhận thông tin kết nối trong .env
- Kiểm tra firewall settings

### 2. Docker build issues:
```bash
# Clean và rebuild
docker-compose down
docker system prune -f
docker-compose up --build --force-recreate
```

### 3. Port conflicts:
- Đảm bảo ports 3000, 8080, 5672, 15672 không bị sử dụng
- Thay đổi ports trong docker-compose.yml nếu cần

### 4. RabbitMQ connection issues:
```bash
# Restart RabbitMQ container
docker-compose restart rabbitmq
```

## Development

### Thêm dependencies mới:

**Backend (Maven)**:
```bash
cd backend
./mvnw dependency:tree  # Xem dependencies hiện tại
# Thêm vào pom.xml và chạy
./mvnw clean install
```

**Frontend (NPM)**:
```bash
cd frontend
npm install <package-name>
```

### Database migrations:

**PostgreSQL**: Sử dụng Liquibase (đã cấu hình)
**MongoDB**: Sử dụng Mongock (đã cấu hình)

## Production Deployment

1. Cập nhật environment variables cho production
2. Sử dụng external databases thay vì local
3. Cấu hình reverse proxy (Nginx)
4. Setup SSL certificates
5. Configure monitoring và logging
