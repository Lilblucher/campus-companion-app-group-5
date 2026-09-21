markdown
# Campus Companion App

**ICT361 Mobile Application Development — Group Lab**
Mulungushi University · School of Engineering and Technology · Department of Computer Science and IT

An Android app that collects `student_name`, `student_number`, `program_of_study` and `Lab_group`.
Students register, view their group and edit permitted details. Lecturers add, view, edit, delete,
search and filter student records. Data is saved **locally on the phone** (Room) and **remotely in
MySQL** through a **Node.js/Express** server.

---

## 1. Tech Stack

| Layer | Technology |
|---|---|
| IDE | Android Studio |
| Language | Java + XML Views |
| State | ViewModel / LiveData |
| Local DB | Room |
| Network | Retrofit / OkHttp |
| Background sync | WorkManager |
| Backend | Node.js + Express |
| Database | MySQL 8.x / InnoDB |
| Auth | JWT + bcrypt |

Dependency versions are recorded in:
- `frontend/gradle/libs.versions.toml`
- `backend/package.json`

All data used in this project is **fictitious**.

---

## 2. Project Structure
campus-companion-app/
├── backend/
│ ├── config/
│ │ └── db.js # MySQL connection pool + transaction helper
│ ├── controllers/
│ │ └── authController.js # login / register logic
│ ├── middleware/
│ │ └── auth.js # JWT check on protected routes
│ ├── routes/
│ │ ├── auth.js # /api/auth/*
│ │ ├── students.js # /api/students/*
│ │ ├── groups.js # /api/groups/*
│ │ └── sync.js # /api/sync/*
│ ├── database/
│ │ └── schema.sql # tables, triggers, views
│ ├── .env # DB password, port, JWT secret (never commit)
│ ├── .env.example # blank template
│ ├── .gitignore
│ ├── package.json
│ └── server.js # entry point
│
├── frontend/
│ ├── app/
│ │ ├── src/main/java/com/mulungushi/campuscompanionapp/
│ │ │ ├── SplashActivity.java
│ │ │ ├── LoginActivity.java
│ │ │ └── RegisterActivity.java
│ │ ├── src/main/res/
│ │ │ ├── layout/ # activity_splash, activity_login, activity_register, activity_main
│ │ │ ├── drawable/ # button + input backgrounds
│ │ │ ├── values/ # colors, strings, themes
│ │ │ └── xml/ # backup + data extraction rules
│ │ └── build.gradle.kts
│ ├── gradle/libs.versions.toml
│ ├── build.gradle.kts
│ └── settings.gradle.kts
│
├── docs/
├── README.md
└── .gitignore

text

---

## 3. Database Setup

### 3.1 Install MySQL 8.x
- Windows: https://dev.mysql.com/downloads/installer/
- macOS: `brew install mysql && brew services start mysql`
- Linux: `sudo apt install mysql-server && sudo systemctl start mysql`

### 3.2 Create database and app user

```sql
CREATE USER 'ict361_app'@'localhost' IDENTIFIED BY 'change_me_local_only';
CREATE DATABASE campus_companion
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
GRANT ALL PRIVILEGES ON campus_companion.* TO 'ict361_app'@'localhost';
FLUSH PRIVILEGES;
3.3 Load the schema
bash
mysql -u ict361_app -p campus_companion < backend/database/schema.sql
Verify:

bash
mysql -u ict361_app -p campus_companion -e "SHOW TABLES;"
Expected tables: accounts, group_change_requests, group_members, lab_groups,
lecturers, programmes, students, sync_operations.

4. Backend Setup
bash
cd backend
npm install
Create backend/.env from the template:

bash
cp .env.example .env
Fill in:

env
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=ict361_app
DB_PASSWORD=change_me_local_only
DB_NAME=campus_companion
DB_POOL_SIZE=10

PORT=3000
NODE_ENV=development
JWT_SECRET=replace_with_a_long_random_string
JWT_EXPIRES_IN=2h
Start the server:

bash
npm run dev      # nodemon
# or
npm start
Expected output:

text
[db] Connected to MySQL 8.0.x (db: campus_companion)
[server] Listening on port 3000
5. Frontend Setup
Open frontend/ in Android Studio.

Base URL for Retrofit:

Target	URL
Emulator	http://10.0.2.2:3000
Physical phone on same Wi-Fi	http://<YOUR_PC_LAN_IP>:3000
Never point Retrofit at localhost from the emulator.

Build and run:

bash
cd frontend
./gradlew assembleDebug
6. How to Test the App
Testing is done at three levels: Android frontend, Node.js backend, and MySQL database.

6.1 Android Frontend
Build and run
Click Run in Android Studio to deploy to an emulator or physical device.
Use Apply Changes to push small edits without restarting the app.

Test the local Room database

Run the app.

In Android Studio: View > Tool Windows > App Inspection.

Open the Database Inspector tab.

Browse tables, run SQL, and edit data live to see UI reactions.

Verify offline sync logic
Trigger a sync manually or wait for WorkManager to run.
Use Database Inspector to confirm:

The local record was saved.

The sync_operations table queued the change.

After connectivity returns, the operation moves to synced.

Run automated tests

Unit tests for ViewModel / repository logic with JUnit.

UI tests with Espresso.

Enable AccessibilityChecks to catch small touch targets and missing content descriptions.

6.2 Backend API (Node.js / Express)
Start the server, then test routes with curl.

bash
# Login
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"L001","password":"yourpassword"}'
Repeat for GET / PUT / DELETE on students, groups, and sync routes.

6.3 MySQL Database
bash
mysql -u ict361_app -p campus_companion
sql
SHOW TABLES;
SELECT * FROM students;
SELECT * FROM v_group_summary;
Test the 15-member group cap (Challenge 1)
Send two simultaneous assignment requests from two terminals:

bash
curl -X POST http://localhost:3000/api/groups/assign \
  -H "Content-Type: application/json" \
  -d '{"student_id": 1, "group_id": 1}' &
curl -X POST http://localhost:3000/api/groups/assign \
  -H "Content-Type: application/json" \
  -d '{"student_id": 2, "group_id": 1}' &
wait
Expected: exactly one succeeds, the other receives GROUP_FULL.
Verify in MySQL that group_members for that group has not exceeded 15 active rows.

6.4 End-to-End Checklist
Register a new student on Android → verify the row appears in MySQL.

Turn off the emulator's Wi-Fi → make an edit → confirm it is saved locally in Database Inspector.

Turn Wi-Fi back on → confirm the change syncs to the server.

Test the full login flow and confirm the JWT is received and stored securely.

Capture screenshots / recordings of each step as evidence.

7. Minimum Demonstration Checklist (from the lab brief)
Test	Expected result
Valid registration and lecturer CRUD	Correct records on phone and server; edits and deletion reflected after sync
Duplicate number and invalid fields	Rejected with useful feedback; form input retained
Full group and simultaneous requests	No group exceeds 15; only one client takes the final place
Failed transfer and repeated deletion	Old group retained on failure; deletion releases only one place
Offline save and interrupted sync	Saved work survives restart; retry applies each operation once
Conflicting edit and remote deletion	Conflict shown; accepted edits not silently lost; no resurrection
Roles and account switching	Student cannot access others' records; no data or queue leaks
Search filters and accessibility	Combined filters work; cache scope clear; usable large text and TalkBack
For each test, record:

Starting data

Steps

Expected vs actual result

Evidence (screenshots, MySQL output, Database Inspector)

8. Security Notes
Passwords are hashed with bcrypt on the server. Never store passwords on the phone.

.env is gitignored. Never commit real credentials.

The API checks identity, role, and record ownership on every protected request.

Students cannot access another student's record or the full roster.

Session tokens are cleared on logout; account data is locked.

9. Git Workflow
bash
git pull --rebase
git checkout -b feature/your-feature
# commit small, descriptive changes
git push origin feature/your-feature
Every commit should reference the contributor log entry: name, student number, task,
commit hash, and testing evidence.

10. Submission Package
Android source (frontend/)

Node.js source (backend/)

APK (app-debug.apk)

Database scripts (backend/database/schema.sql, seed.sql)

Test evidence (screenshots, Database Inspector exports, MySQL output)

Architecture diagram and wireframes (docs/)

Setup README (this file)

Contribution log

AI/source log

5–8 minute group demo

Exclude node_modules/, .env, and any real credentials.

11. Known Cleanup Items
The following stray files at the repo root should be removed before submission:

Controllers/authController.js (duplicate of backend/controllers/authController.js)

env (should be backend/.env, gitignored)

backend/.env.bak

backend/package-lock.json.bak

12. Reflection
Each member should add a brief reflection on:

One concept learned

One bug solved

