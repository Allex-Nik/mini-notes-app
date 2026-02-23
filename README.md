# Mini notes app
An aplication for taking notes written in Kotlin using Swing for the GUI. Supports data storage via JDBC or Hibernate (MySQL).

## Requirements
- JDK 17 or higher  
- MySQL Server

## Database Setup
Before running the application, you need to create a database named `notesapp`.
The `notes` table will be created automatically on the first run.

## Environment Variables
Set up the following environment variables:
- `MYSQL_USER`
- `MYSQL_PASSWORD`

For tests set up also:
- `MYSQL_TEST_USER`
- `MYSQL_TEST_PASSWORD`

## Hibernate configuration
Add database username and password to the `hibernate.cfg.xml` file, adjust other settings in the file if needed.

For testing, do the same in the `hibernate_test.cfg.xml` file.
