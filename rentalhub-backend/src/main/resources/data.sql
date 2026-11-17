
INSERT INTO roles (name)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

INSERT INTO roles (name)
SELECT 'ROLE_OWNER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_OWNER');

INSERT INTO roles (name)
SELECT 'ROLE_TENANT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_TENANT');

INSERT INTO users (full_name, email, password, phone, is_active, is_email_verified, created_at, updated_at)
SELECT 'System Administrator', 'admin@rentalhub.com',
       '$2a$10$EblZqNptyYvcLm/VwDCVAOWD2D1Q5p9j6.6KGSiXWXGCp.AdL3VKa',
       '9999999999', true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@rentalhub.com');

-- Link the admin user to the ROLE_ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@rentalhub.com'
  AND r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );