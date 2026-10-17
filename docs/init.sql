-- AI 智能求职招聘平台 MySQL 8 初始化脚本
-- Day 3 仅定义数据库结构，不包含业务接口或测试数据。

CREATE DATABASE IF NOT EXISTS ai_job_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ai_job_platform;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_match_result;
DROP TABLE IF EXISTS job_application;
DROP TABLE IF EXISTS job_posting;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS platform_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE platform_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) NULL COMMENT '手机号',
    real_name VARCHAR(50) NULL COMMENT '真实姓名',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE、DISABLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_user_username (username),
    UNIQUE KEY uk_platform_user_email (email),
    UNIQUE KEY uk_platform_user_phone (phone),
    CONSTRAINT chk_platform_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB COMMENT = '平台用户';

CREATE TABLE role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色主键',
    code VARCHAR(30) NOT NULL COMMENT '角色编码：STUDENT、HR、ADMIN',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '角色说明',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code),
    CONSTRAINT chk_role_code CHECK (code IN ('STUDENT', 'HR', 'ADMIN'))
) ENGINE = InnoDB COMMENT = '角色';

CREATE TABLE user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户角色关联主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户主键',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色主键',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role_user_role (user_id, role_id),
    KEY idx_user_role_role_id (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES platform_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE RESTRICT
) ENGINE = InnoDB COMMENT = '用户角色关联';

CREATE TABLE resume (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '简历主键',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户主键',
    title VARCHAR(100) NOT NULL COMMENT '简历标题',
    education TEXT NULL COMMENT '教育经历',
    work_experience TEXT NULL COMMENT '实习或工作经历',
    project_experience TEXT NULL COMMENT '项目经历',
    skills TEXT NULL COMMENT '技能描述',
    self_evaluation TEXT NULL COMMENT '自我评价',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT、PUBLISHED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_id_student (id, student_id),
    KEY idx_resume_student_status (student_id, status),
    CONSTRAINT fk_resume_student FOREIGN KEY (student_id) REFERENCES platform_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_resume_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
) ENGINE = InnoDB COMMENT = '学生简历';

CREATE TABLE job_posting (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位主键',
    hr_id BIGINT UNSIGNED NOT NULL COMMENT '发布岗位的 HR 用户主键',
    title VARCHAR(100) NOT NULL COMMENT '岗位名称',
    company_name VARCHAR(100) NOT NULL COMMENT '公司名称',
    city VARCHAR(50) NOT NULL COMMENT '工作城市',
    employment_type VARCHAR(30) NOT NULL COMMENT '用工类型：FULL_TIME、PART_TIME、INTERNSHIP',
    salary_min DECIMAL(10, 2) NULL COMMENT '最低月薪',
    salary_max DECIMAL(10, 2) NULL COMMENT '最高月薪',
    description TEXT NOT NULL COMMENT '岗位描述',
    requirements TEXT NOT NULL COMMENT '任职要求',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT、PUBLISHED、CLOSED',
    published_at DATETIME(3) NULL COMMENT '发布时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_job_posting_hr_status (hr_id, status),
    KEY idx_job_posting_status_city (status, city),
    KEY idx_job_posting_published_at (published_at),
    CONSTRAINT fk_job_posting_hr FOREIGN KEY (hr_id) REFERENCES platform_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_job_posting_employment_type CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'INTERNSHIP')),
    CONSTRAINT chk_job_posting_salary CHECK (
        (salary_min IS NULL OR salary_min >= 0)
        AND (salary_max IS NULL OR salary_max >= 0)
        AND (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
    ),
    CONSTRAINT chk_job_posting_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'))
) ENGINE = InnoDB COMMENT = '招聘岗位';

CREATE TABLE job_application (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '投递主键',
    job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位主键',
    student_id BIGINT UNSIGNED NOT NULL COMMENT '学生用户主键',
    resume_id BIGINT UNSIGNED NOT NULL COMMENT '投递使用的简历主键',
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED、REVIEWING、INTERVIEW、OFFERED、REJECTED、WITHDRAWN',
    applied_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '投递时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_application_job_student (job_id, student_id),
    KEY idx_job_application_student_status (student_id, status),
    KEY idx_job_application_resume_student (resume_id, student_id),
    KEY idx_job_application_applied_at (applied_at),
    CONSTRAINT fk_job_application_job FOREIGN KEY (job_id) REFERENCES job_posting (id) ON DELETE RESTRICT,
    CONSTRAINT fk_job_application_student FOREIGN KEY (student_id) REFERENCES platform_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_job_application_resume_owner FOREIGN KEY (resume_id, student_id) REFERENCES resume (id, student_id) ON DELETE RESTRICT,
    CONSTRAINT chk_job_application_status CHECK (status IN ('SUBMITTED', 'REVIEWING', 'INTERVIEW', 'OFFERED', 'REJECTED', 'WITHDRAWN'))
) ENGINE = InnoDB COMMENT = '岗位投递';

CREATE TABLE ai_match_result (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '匹配结果主键',
    resume_id BIGINT UNSIGNED NOT NULL COMMENT '简历主键',
    job_id BIGINT UNSIGNED NOT NULL COMMENT '岗位主键',
    score DECIMAL(5, 2) NOT NULL COMMENT '匹配分数，范围 0 至 100',
    analysis TEXT NOT NULL COMMENT 'AI 匹配分析',
    model_name VARCHAR(100) NOT NULL COMMENT '生成结果的模型名称',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_match_result_resume_job (resume_id, job_id),
    KEY idx_ai_match_result_job_score (job_id, score),
    CONSTRAINT fk_ai_match_result_resume FOREIGN KEY (resume_id) REFERENCES resume (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_match_result_job FOREIGN KEY (job_id) REFERENCES job_posting (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_match_result_score CHECK (score >= 0 AND score <= 100)
) ENGINE = InnoDB COMMENT = 'AI 简历岗位匹配结果';

INSERT INTO role (code, name, description)
VALUES ('STUDENT', '学生', '维护简历并投递岗位'),
       ('HR', '招聘人员', '发布岗位并管理投递'),
       ('ADMIN', '管理员', '管理平台用户和统计数据');
