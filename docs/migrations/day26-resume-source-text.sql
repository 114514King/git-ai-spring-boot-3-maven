-- MySQL 8: select the existing application database before running this script.
-- Non-destructive and repeatable. Do not run docs/init.sql on an existing database.
SET @day26_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resume' AND COLUMN_NAME = 'source_text'
);
SET @day26_sql = IF(@day26_column_exists = 0,
    'ALTER TABLE resume ADD COLUMN source_text TEXT NULL COMMENT ''经学生确认的导入简历文本，最多20000字符'' AFTER self_evaluation',
    'SELECT ''source_text already exists'' AS migration_status');
PREPARE day26_statement FROM @day26_sql;
EXECUTE day26_statement;
DEALLOCATE PREPARE day26_statement;
