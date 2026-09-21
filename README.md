# campus-companion-app

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
