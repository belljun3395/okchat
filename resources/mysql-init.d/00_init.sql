CREATE
    USER 'okchat-local'@'localhost' IDENTIFIED BY 'okchat-local';
CREATE
    USER 'okchat-local'@'%' IDENTIFIED BY 'okchat-local';

GRANT ALL PRIVILEGES ON *.* TO
    'okchat-local'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO
    'okchat-local'@'%';

CREATE
    DATABASE okchat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
