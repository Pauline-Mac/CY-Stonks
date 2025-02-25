CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     name TEXT NOT NULL,
                                     email TEXT UNIQUE NOT NULL
);

INSERT INTO users (name, email) VALUES
                                    ('John Doe', 'johndoe@example.com'),
                                    ('Jane Smith', 'janesmith@example.com'),
                                    ('Alice Johnson', 'alice.johnson@example.com');


