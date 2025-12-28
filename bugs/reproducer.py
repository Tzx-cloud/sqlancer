#!/usr/bin/env python3
import mysql.connector
import re
import os

# --- 配置 ---
# 请根据您的 MySQL 设置修改
MYSQL_CONFIG = {
    'host': '127.0.0.1',
    'user': 'root',
    'password': 'root' # <-- 修改为您的 MySQL root 密码
}
DATABASE_NAME = 'database0'
LOG_FILE_PATH = 'database0.log'

def execute_sql_from_log(log_path):
    """
    从指定的日志文件中读取 SQL 并执行。
    """
    if not os.path.exists(log_path):
        print(f"错误: 日志文件 '{log_path}' 不存在。")
        return

    # 1. 读取并解析 SQL 语句
    sql_statements = []
    try:
        with open(log_path, 'r', encoding='utf-8') as f:
            in_sql_block = False
            for line in f:
                # 从第一条 CREATE TABLE 语句开始作为 SQL 块的起点
                if line.strip().upper().startswith('CREATE TABLE'):
                    in_sql_block = True

                # 忽略注释和空行
                if in_sql_block and not line.strip().startswith('--') and line.strip():
                    sql_statements.append(line.strip())

    except Exception as e:
        print(f"读取或解析日志文件时出错: {e}")
        return

    if not sql_statements:
        print("未在日志文件中找到可执行的 SQL 语句。")
        return

    # 2. 连接数据库并执行 SQL
    cnx = None
    try:
        print("正在连接到 MySQL 服务器...")
        cnx = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = cnx.cursor()
        print("连接成功。")

        print(f"正在准备数据库 '{DATABASE_NAME}'...")
        cursor.execute(f"DROP DATABASE IF EXISTS {DATABASE_NAME}")
        cursor.execute(f"CREATE DATABASE {DATABASE_NAME}")
        cursor.execute(f"USE {DATABASE_NAME}")
        print("数据库已重置。")

        print(f"\n开始执行 {len(sql_statements)} 条 SQL 语句...")
        for i, sql in enumerate(sql_statements):
            try:
                # mysql-connector 不支持一次执行多条语句，但我们的日志是一行一条
                cursor.execute(sql)
            except mysql.connector.Error as err:
                print(f"--- [失败] 第 {i+1} 条语句 ---")
                print(f"   SQL: {sql}")
                print(f"   错误: {err}")
                print("--------------------------")

        cnx.commit()
        print("\n所有 SQL 语句执行完毕。")

    except mysql.connector.Error as err:
        print(f"数据库操作失败: {err}")
    finally:
        if cnx and cnx.is_connected():
            cursor.close()
            cnx.close()
            print("数据库连接已关闭。")

if __name__ == '__main__':
    execute_sql_from_log(LOG_FILE_PATH)
