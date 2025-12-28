import itertools
import random

# 提取所有系统变量名（从提供的代码中提取）
variables = [
    "big_tables", "completion_type", "bulk_insert_buffer_size", "concurrent_insert",
    "cte_max_recursion_depth", "delay_key_write", "eq_range_index_dive_limit", "flush",
    "foreign_key_checks", "histogram_generation_max_mem_size", "host_cache_size",
    "internal_tmp_mem_storage_engine", "join_buffer_size", "max_heap_table_size",
    "max_length_for_sort_data", "max_points_in_geometry", "max_seeks_for_key",
    "max_sort_length", "max_sp_recursion_depth", "myisam_data_pointer_size",
    "myisam_max_sort_file_size", "myisam_sort_buffer_size", "myisam_stats_method",
    "myisam_use_mmap", "old_alter_table", "optimizer_prune_level",
    "optimizer_search_depth", "optimizer_switch", "parser_max_mem_size",
    "preload_buffer_size", "query_alloc_block_size", "query_prealloc_size",
    "range_alloc_block_size", "range_optimizer_max_mem_size", "rbr_exec_mode",
    "read_buffer_size", "read_rnd_buffer_size", "schema_definition_cache",
    "show_create_table_verbosity", "show_old_temporals", "sql_auto_is_null",
    "sql_buffer_result", "sql_log_off", "sql_quote_show_create", "tmp_table_size",
    "unique_checks"
]

# 生成所有二元参数组合
combinations = list(itertools.combinations(variables, 2))

# 生成随机权重（范围在0.1到10.0之间，保留2位小数）
random_weights = [round(random.uniform(1,100)) for _ in range(len(combinations))]

output_file = "mysql_config_weights.txt"
with open(output_file, "w") as f:
    for i, (var1, var2) in enumerate(combinations):
        f.write(f"{var1},{var2}:{random_weights[i]}\n")

print(f"成功生成 {len(combinations)} 个组合，已保存到 {output_file}")