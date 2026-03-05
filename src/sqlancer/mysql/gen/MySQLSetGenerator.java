package sqlancer.mysql.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import sqlancer.BaseConfigurationGenerator;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.MySQLBugs;
import sqlancer.mysql.MySQLGlobalState;

public class MySQLSetGenerator extends BaseConfigurationGenerator {

    private static volatile MySQLSetGenerator INSTANCE;

    private final StringBuilder sb = new StringBuilder();

    // currently, global options are only generated when a single thread is executed
    private boolean isSingleThreaded;

    public MySQLSetGenerator(Randomly r, MainOptions options) {
        super(r, options);
    }

    @Override
    protected String getDatabaseType() {
        return "mysql";
    }

    public static SQLQueryAdapter set(MySQLGlobalState globalState) {
        return new MySQLSetGenerator(globalState.getRandomly(), globalState.getOptions()).get();
    }
    @Override
    protected String getActionName(Object action) {
        return ((MySQLSetGenerator.Action) action).name();
    }

    @Override
    public  ConfigurationAction[] getAllActions() {
        return MySQLSetGenerator.Action.values();
    }


    @Override
    public SQLQueryAdapter generateConfigForParameter(ConfigurationAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET ");

        // 选择作用域
        Scope[] scopes = action.getScopes();
        sb.append(action.getScopes()[0]);

        sb.append(" ");
        sb.append(action.getName());
        sb.append(" = ");
        sb.append(action.generateValue(r));

        return new SQLQueryAdapter(sb.toString());
    }

    private SQLQueryAdapter get() {
        sb.append("SET ");
        Action a;
        if (isSingleThreaded) {
            a = Randomly.fromOptions(Action.values());
            Scope[] scopes = a.getScopes();
            Scope randomScope = Randomly.fromOptions(scopes);
            switch (randomScope) {
                case GLOBAL:
                    sb.append("GLOBAL");
                    break;
                case SESSION:
                    sb.append("SESSION");
                    break;
                default:
                    throw new AssertionError(randomScope);
            }

        } else {
            do {
                a = Randomly.fromOptions(Action.values());
            } while (!a.canBeUsedInScope(Scope.SESSION));
            sb.append("SESSION");
        }
        sb.append(" ");
        sb.append(a.getName());
        sb.append(" = ");
        sb.append(a.generateValue(r));
        return new SQLQueryAdapter(sb.toString());
    }

    @Override
    public SQLQueryAdapter generateDefaultConfigForParameter(ConfigurationAction action) {
        if (Objects.equals(action.getName(), "optimizer_switch")) {
            return resetOptimizer();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SET ");

        // 选择作用域
        Scope[] scopes = action.getScopes();

        sb.append(scopes[0]);

        sb.append(" ");
        sb.append(action.getName());
        sb.append(" = DEFAULT");

        return new SQLQueryAdapter(sb.toString());
    }

    public static BaseConfigurationGenerator getInstance(Randomly r, MainOptions options) {
        if (INSTANCE == null) {
            synchronized (MySQLSetGenerator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MySQLSetGenerator(r, options);
                }
            }
        }
        return INSTANCE;
    }
    public static SQLQueryAdapter resetOptimizer() {
        return new SQLQueryAdapter("SET optimizer_switch='default'");
    }

    public static List<SQLQueryAdapter> getAllOptimizer(MySQLGlobalState globalState) {
        List<SQLQueryAdapter> result = new ArrayList<>();
        String[] options = { "index_merge", "index_merge_union", "index_merge_sort_union", "index_merge_intersection",
                "engine_condition_pushdown", "index_condition_pushdown", "mrr", "mrr_cost_based", "block_nested_loop",
                "batched_key_access", "materialization", "semijoin", "loosescan", "firstmatch", "duplicateweedout",
                "subquery_materialization_cost_based", "use_index_extensions", "condition_fanout_filter",
                "derived_merge", "use_invisible_indexes", "skip_scan", "hash_join", "subquery_to_derived",
                "prefer_ordering_index", "derived_condition_pushdown" };

        List<String> availableOptions = new ArrayList<>(Arrays.asList(options));
//        if (MySQLBugs.bug112242) {
//            availableOptions.remove("use_invisible_indexes");
//        }
//        if (MySQLBugs.bug112243) {
//            availableOptions.remove("subquery_to_derived");
//        }
//        if (MySQLBugs.bug112264) {
//            availableOptions.remove("block_nested_loop");
//        }

        StringBuilder sb = new StringBuilder();
        sb.append("SET ");
        if (globalState.getOptions().getNumberConcurrentThreads() == 1 && Randomly.getBoolean()) {
            sb.append("GLOBAL");
        } else {
            sb.append("SESSION");
        }
        sb.append(" optimizer_switch = '%s'");

        for (String option : availableOptions) {
            result.add(new SQLQueryAdapter(String.format(sb.toString(), option + "=on")));
            result.add(new SQLQueryAdapter(String.format(sb.toString(), option + "=off")));
            result.add(new SQLQueryAdapter(String.format(sb.toString(), option + "=default")));
        }

        return result;
    }
    private enum Action implements ConfigurationAction{
        ACTIVATE_ALL_ROLES_ON_LOGIN("activate_all_roles_on_login", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        ADMIN_TLS_CIPHERSUITES("admin_tls_ciphersuites", (r) -> Randomly.fromOptions("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384","TLS_CHACHA20_POLY1305_SHA256"), Scope.GLOBAL), //
        ADMIN_TLS_VERSION("admin_tls_version", (r) -> Randomly.fromOptions("'TLSv1.2'", "'TLSv1.3'"), Scope.GLOBAL), //
        //AUTHENTICATION_POLICY("authentication_policy", (r) -> r.getString(), Scope.GLOBAL), //
        AUTOCOMMIT("autocommit", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        AUTOMATIC_SP_PRIVILEGES("automatic_sp_privileges", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        AUTO_INCREMENT_INCREMENT("auto_increment_increment", (r) -> r.getLongWithBoundaryBias(1, 65535), Scope.GLOBAL, Scope.SESSION), //
        AUTO_INCREMENT_OFFSET("auto_increment_offset", (r) -> r.getLongWithBoundaryBias(1, 65535), Scope.GLOBAL, Scope.SESSION), //
        BIG_TABLES("big_tables", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_CACHE_SIZE("binlog_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        BINLOG_CHECKSUM("binlog_checksum", (r) -> Randomly.fromOptions("NONE", "CRC32"), Scope.GLOBAL), //
        BINLOG_DIRECT_NON_TRANSACTIONAL_UPDATES("binlog_direct_non_transactional_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        // BINLOG_ENCRYPTION("binlog_encryption", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        BINLOG_ERROR_ACTION("binlog_error_action", (r) -> Randomly.fromOptions("IGNORE_ERROR", "ABORT_SERVER"), Scope.GLOBAL), //
        BINLOG_EXPIRE_LOGS_AUTO_PURGE("binlog_expire_logs_auto_purge", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        BINLOG_EXPIRE_LOGS_SECONDS("binlog_expire_logs_seconds", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        BINLOG_FORMAT("binlog_format", (r) -> Randomly.fromOptions("ROW", "STATEMENT", "MIXED"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_GROUP_COMMIT_SYNC_DELAY("binlog_group_commit_sync_delay", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        BINLOG_GROUP_COMMIT_SYNC_NO_DELAY_COUNT("binlog_group_commit_sync_no_delay_count", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        BINLOG_MAX_FLUSH_QUEUE_TIME("binlog_max_flush_queue_time", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        BINLOG_ORDER_COMMITS("binlog_order_commits", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        BINLOG_ROWS_QUERY_LOG_EVENTS("binlog_rows_query_log_events", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_ROW_IMAGE("binlog_row_image", (r) -> Randomly.fromOptions("FULL", "MINIMAL", "NOBLOB"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_ROW_METADATA("binlog_row_metadata", (r) -> Randomly.fromOptions("MINIMAL", "FULL"), Scope.GLOBAL), //
        BINLOG_ROW_VALUE_OPTIONS("binlog_row_value_options", (r) -> Randomly.fromOptions("PARTIAL_JSON"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_STMT_CACHE_SIZE("binlog_stmt_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        BINLOG_TRANSACTION_COMPRESSION("binlog_transaction_compression", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_TRANSACTION_COMPRESSION_LEVEL_ZSTD("binlog_transaction_compression_level_zstd", (r) -> r.getLongWithBoundaryBias(1, 22), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_TRANSACTION_DEPENDENCY_HISTORY_SIZE("binlog_transaction_dependency_history_size", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        BLOCK_ENCRYPTION_MODE("block_encryption_mode", (r) -> Randomly.fromOptions("'aes-128-ecb'", "'aes-256-cbc'"), Scope.GLOBAL, Scope.SESSION), //
        BULK_INSERT_BUFFER_SIZE("bulk_insert_buffer_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        //        CHARACTER_SET_CLIENT("character_set_client", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_CONNECTION("character_set_connection", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_DATABASE("character_set_database", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        CHARACTER_SET_FILESYSTEM("character_set_filesystem", (r) -> Randomly.fromOptions("utf8mb4", "binary", "latin1"), Scope.GLOBAL, Scope.SESSION), //
        //        CHARACTER_SET_RESULTS("character_set_results", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_SERVER("character_set_server", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        CHECK_PROXY_USERS("check_proxy_users", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //CLONE_DDL_TIMEOUT("clone_ddl_timeout", (r) -> r.getLongWithBoundaryBias(0, 3600), Scope.GLOBAL), //
        // CLONE_DONOR_TIMEOUT_AFTER_NETWORK_FAILURE("clone_donor_timeout_after_network_failure", (r) -> r.getLongWithBoundaryBias(0, 30), Scope.GLOBAL), //
//        COLLATION_CONNECTION("collation_connection", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        COLLATION_SERVER("collation_server", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        COMPLETION_TYPE("completion_type", (r) -> Randomly.fromOptions("'NO_CHAIN'", "'CHAIN'", "'RELEASE'", "0", "1", "2"), Scope.GLOBAL, Scope.SESSION), //
        CONNECTION_MEMORY_CHUNK_SIZE("connection_memory_chunk_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        CONNECTION_MEMORY_LIMIT("connection_memory_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        CONNECT_TIMEOUT("connect_timeout", (r) -> r.getLongWithBoundaryBias(2, 31536000), Scope.GLOBAL), //
        CTE_MAX_RECURSION_DEPTH("cte_max_recursion_depth", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        //        DEBUG("debug", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        DEBUG_SYNC("debug_sync", (r) -> Randomly.fromOptions("ON", "OFF"), Scope.SESSION), //
        DEFAULT_COLLATION_FOR_UTF8MB4("default_collation_for_utf8mb4", (r) -> Randomly.fromOptions("utf8mb4_0900_ai_ci", "utf8mb4_general_ci"), Scope.GLOBAL, Scope.SESSION), //
        DEFAULT_PASSWORD_LIFETIME("default_password_lifetime", (r) -> r.getLongWithBoundaryBias(0, 32767), Scope.GLOBAL), //
        DEFAULT_TABLE_ENCRYPTION("default_table_encryption", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        DEFAULT_WEEK_FORMAT("default_week_format", (r) -> r.getLongWithBoundaryBias(0, 7), Scope.GLOBAL, Scope.SESSION), //
        DELAYED_INSERT_LIMIT("delayed_insert_limit", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL), //
        DELAYED_INSERT_TIMEOUT("delayed_insert_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL), //
        DELAYED_QUEUE_SIZE("delayed_queue_size", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL), //
        DELAY_KEY_WRITE("delay_key_write", (r) -> Randomly.fromOptions("ON", "OFF", "ALL"), Scope.GLOBAL), //
        DIV_PRECISION_INCREMENT("div_precision_increment", (r) -> r.getLongWithBoundaryBias(0, 30), Scope.GLOBAL, Scope.SESSION), //
        END_MARKERS_IN_JSON("end_markers_in_json", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        ENFORCE_GTID_CONSISTENCY("enforce_gtid_consistency", (r) -> Randomly.fromOptions("OFF", "ON", "WARN"), Scope.GLOBAL), //
        EQ_RANGE_INDEX_DIVE_LIMIT("eq_range_index_dive_limit", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        EVENT_SCHEDULER("event_scheduler", (r) -> Randomly.fromOptions("ON", "OFF"), Scope.GLOBAL), //
        EXPLAIN_FORMAT("explain_format", (r) -> Randomly.fromOptions("TRADITIONAL", "JSON", "TREE"), Scope.GLOBAL, Scope.SESSION), //
        EXPLICIT_DEFAULTS_FOR_TIMESTAMP("explicit_defaults_for_timestamp", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        FLUSH("flush", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        FLUSH_TIME("flush_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        FOREIGN_KEY_CHECKS("foreign_key_checks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        FT_BOOLEAN_SYNTAX("ft_boolean_syntax", (r) -> Randomly.fromOptions("'+ -><()~*:\"\"&|'"), Scope.GLOBAL), //
        GENERAL_LOG("general_log", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //        GENERAL_LOG_FILE("general_log_file", (r) -> r.getString(), Scope.GLOBAL), //
        GENERATED_RANDOM_PASSWORD_LENGTH("generated_random_password_length", (r) -> r.getLongWithBoundaryBias(5, 255), Scope.GLOBAL), //
        GLOBAL_CONNECTION_MEMORY_LIMIT("global_connection_memory_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        GLOBAL_CONNECTION_MEMORY_TRACKING("global_connection_memory_tracking", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        GROUP_CONCAT_MAX_LEN("group_concat_max_len", (r) -> r.getLongWithBoundaryBias(4, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        GROUP_REPLICATION_CONSISTENCY("group_replication_consistency", (r) -> Randomly.fromOptions("'EVENTUAL'", "'BEFORE_ON_PRIMARY_FAILOVER'", "'BEFORE'", "'AFTER'", "'BEFORE_AND_AFTER'"), Scope.GLOBAL, Scope.SESSION), //
        //        GROUP_REPLICATION_VIEW_CHANGE_UUID("group_replication_view_change_uuid", (r) -> r.getString(), Scope.GLOBAL), //
        GTID_EXECUTED_COMPRESSION_PERIOD("gtid_executed_compression_period", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        //GTID_MODE("gtid_mode", (r) -> Randomly.fromOptions("OFF", "OFF_PERMISSIVE", "ON_PERMISSIVE", "ON"), Scope.GLOBAL), //
        //GTID_NEXT("gtid_next", (r) -> Randomly.fromOptions("AUTOMATIC", "ANONYMOUS"), Scope.SESSION), //
//        GTID_PURGED("gtid_purged", (r) -> r.getString(), Scope.GLOBAL), //
        HISTOGRAM_GENERATION_MAX_MEM_SIZE("histogram_generation_max_mem_size", (r) -> r.getLongWithBoundaryBias(1000000, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        HOST_CACHE_SIZE("host_cache_size", (r) -> r.getLongWithBoundaryBias(0, 65536), Scope.GLOBAL), //
        //IDENTITY("identity", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        IMMEDIATE_SERVER_VERSION("immediate_server_version", (r) -> r.getLongWithBoundaryBias(0, 999999), Scope.SESSION), //
        INFORMATION_SCHEMA_STATS_EXPIRY("information_schema_stats_expiry", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        //        INIT_CONNECT("init_connect", (r) -> r.getString(), Scope.GLOBAL), //
//        INIT_REPLICA("init_replica", (r) -> r.getString(), Scope.GLOBAL), //
//        INIT_SLAVE("init_slave", (r) -> r.getString(), Scope.GLOBAL), //
        INNODB_ADAPTIVE_FLUSHING("innodb_adaptive_flushing", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_ADAPTIVE_FLUSHING_LWM("innodb_adaptive_flushing_lwm", (r) -> r.getLongWithBoundaryBias(0, 70), Scope.GLOBAL), //
        INNODB_ADAPTIVE_MAX_SLEEP_DELAY("innodb_adaptive_max_sleep_delay", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_DUMP_AT_SHUTDOWN("innodb_buffer_pool_dump_at_shutdown", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_DUMP_PCT("innodb_buffer_pool_dump_pct", (r) -> r.getLongWithBoundaryBias(1, 100), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_IN_CORE_FILE("innodb_buffer_pool_in_core_file", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_SIZE("innodb_buffer_pool_size", (r) -> r.getLongWithBoundaryBias(5242880, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_CHANGE_BUFFERING("innodb_change_buffering", (r) -> Randomly.fromOptions("none", "inserts", "deletes", "changes", "purges", "all"), Scope.GLOBAL), //
        INNODB_CHANGE_BUFFER_MAX_SIZE("innodb_change_buffer_max_size", (r) -> r.getLongWithBoundaryBias(0, 50), Scope.GLOBAL), //
        INNODB_CHECKPOINT_DISABLED("innodb_checkpoint_disabled", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_CHECKSUM_ALGORITHM("innodb_checksum_algorithm", (r) -> Randomly.fromOptions("crc32", "strict_crc32", "innodb", "strict_innodb", "none", "strict_none"), Scope.GLOBAL), //
        INNODB_CMP_PER_INDEX_ENABLED("innodb_cmp_per_index_enabled", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_DDL_LOG_CRASH_RESET_DEBUG("innodb_ddl_log_crash_reset_debug", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_DEFAULT_ROW_FORMAT("innodb_default_row_format", (r) -> Randomly.fromOptions("redundant", "compact", "dynamic"), Scope.GLOBAL), //
        INNODB_DISABLE_SORT_FILE_CACHE("innodb_disable_sort_file_cache", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_FAST_SHUTDOWN("innodb_fast_shutdown", (r) -> Randomly.fromOptions("0", "1", "2"), Scope.GLOBAL), //
        INNODB_FILE_PER_TABLE("innodb_file_per_table", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_FLUSHING_AVG_LOOPS("innodb_flushing_avg_loops", (r) -> r.getLongWithBoundaryBias(1, 1000), Scope.GLOBAL), //
        INNODB_FLUSH_LOG_AT_TIMEOUT("innodb_flush_log_at_timeout", (r) -> r.getLongWithBoundaryBias(0, 2700), Scope.GLOBAL), //
        INNODB_FLUSH_LOG_AT_TRX_COMMIT("innodb_flush_log_at_trx_commit", (r) -> Randomly.fromOptions("0", "1", "2"), Scope.GLOBAL), //
        INNODB_FLUSH_NEIGHBORS("innodb_flush_neighbors", (r) -> Randomly.fromOptions("0", "1", "2"), Scope.GLOBAL), //
        INNODB_FLUSH_SYNC("innodb_flush_sync", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_IDLE_FLUSH_PCT("innodb_idle_flush_pct", (r) -> r.getLongWithBoundaryBias(0, 100), Scope.GLOBAL), //
        INNODB_IO_CAPACITY("innodb_io_capacity", (r) -> r.getLongWithBoundaryBias(100, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_LOG_BUFFER_SIZE("innodb_log_buffer_size", (r) -> r.getLongWithBoundaryBias(1048576, 4294967295L), Scope.GLOBAL), //
        INNODB_LOG_CHECKSUMS("innodb_log_checksums", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_LOG_SPIN_CPU_ABS_LWM("innodb_log_spin_cpu_abs_lwm", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_LOG_SPIN_CPU_PCT_HWM("innodb_log_spin_cpu_pct_hwm", (r) -> r.getLongWithBoundaryBias(0, 100), Scope.GLOBAL), //
        INNODB_LOG_WAIT_FOR_FLUSH_SPIN_HWM("innodb_log_wait_for_flush_spin_hwm", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_LOG_WRITER_THREADS("innodb_log_writer_threads", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_LOG_WRITE_AHEAD_SIZE("innodb_log_write_ahead_size", (r) -> r.getLongWithBoundaryBias(512, 16384), Scope.GLOBAL), //
        INNODB_LRU_SCAN_DEPTH("innodb_lru_scan_depth", (r) -> r.getLongWithBoundaryBias(100, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_MAX_DIRTY_PAGES_PCT_LWM("innodb_max_dirty_pages_pct_lwm", (r) -> r.getLongWithBoundaryBias(0, 99), Scope.GLOBAL), //
        INNODB_MAX_PURGE_LAG("innodb_max_purge_lag", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        INNODB_MAX_PURGE_LAG_DELAY("innodb_max_purge_lag_delay", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        INNODB_MERGE_THRESHOLD_SET_ALL_DEBUG("innodb_merge_threshold_set_all_debug", (r) -> r.getLongWithBoundaryBias(1, 50), Scope.GLOBAL), //
        INNODB_MONITOR_ENABLE("innodb_monitor_enable", (r) -> Randomly.fromOptions(
                "'module_metadata'",
                "'module_lock'",
                "'module_buffer'",
                "'module_trx'",
                "'module_os'",
                "'module_log'",
                "'module_file'",
                "'module_index'",
                "'module_adaptive_hash'",
                "'module_ibuf_system'",
                "'module_srv'",
                "'module_ddl'",
                "'module_dml'",
                "'module_compress'",
                "'module_icp'",
                "'dml_reads'",
                "'dml_inserts'",
                "'dml_deletes'",
                "'dml_updates'",
                "'%lock%'",  // 通配符模式
                "'%dml%'",   // 通配符模式
                "'all'"), Scope.GLOBAL), //
        //INNODB_OPEN_FILES("innodb_open_files", (r) -> r.getLongWithBoundaryBias(10, 2147483647), Scope.GLOBAL), //
        INNODB_OPTIMIZE_FULLTEXT_ONLY("innodb_optimize_fulltext_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_PRINT_ALL_DEADLOCKS("innodb_print_all_deadlocks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_PRINT_DDL_LOGS("innodb_print_ddl_logs", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_PURGE_BATCH_SIZE("innodb_purge_batch_size", (r) -> r.getLongWithBoundaryBias(1, 5000), Scope.GLOBAL), //
        INNODB_PURGE_RSEG_TRUNCATE_FREQUENCY("innodb_purge_rseg_truncate_frequency", (r) -> r.getLongWithBoundaryBias(1, 128), Scope.GLOBAL), //
        INNODB_RANDOM_READ_AHEAD("innodb_random_read_ahead", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_READ_AHEAD_THRESHOLD("innodb_read_ahead_threshold", (r) -> r.getLongWithBoundaryBias(0, 64), Scope.GLOBAL), //
        INNODB_REDO_LOG_CAPACITY("innodb_redo_log_capacity", (r) -> r.getLongWithBoundaryBias(8388608, 137438953472L), Scope.GLOBAL), //
        //INNODB_REDO_LOG_ENCRYPT("innodb_redo_log_encrypt", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_REPLICATION_DELAY("innodb_replication_delay", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        INNODB_ROLLBACK_SEGMENTS("innodb_rollback_segments", (r) -> r.getLongWithBoundaryBias(1, 128), Scope.GLOBAL), //
        INNODB_SAVED_PAGE_NUMBER_DEBUG("innodb_saved_page_number_debug", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_SPIN_WAIT_DELAY("innodb_spin_wait_delay", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        INNODB_STATS_AUTO_RECALC("innodb_stats_auto_recalc", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_STATS_INCLUDE_DELETE_MARKED("innodb_stats_include_delete_marked", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_STATS_METHOD("innodb_stats_method", (r) -> Randomly.fromOptions("nulls_equal", "nulls_unequal", "nulls_ignored"), Scope.GLOBAL), //
        INNODB_STATS_PERSISTENT("innodb_stats_persistent", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_STATS_PERSISTENT_SAMPLE_PAGES("innodb_stats_persistent_sample_pages", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_STATS_TRANSIENT_SAMPLE_PAGES("innodb_stats_transient_sample_pages", (r) -> r.getLongWithBoundaryBias(1, 1000), Scope.GLOBAL), //
        INNODB_THREAD_CONCURRENCY("innodb_thread_concurrency", (r) -> r.getLongWithBoundaryBias(0, 1000), Scope.GLOBAL), //
        INNODB_THREAD_SLEEP_DELAY("innodb_thread_sleep_delay", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        //INNODB_TMPDIR("innodb_tmpdir", (r) -> r.getString(), Scope.GLOBAL), //
        //INNODB_UNDO_LOG_ENCRYPT("innodb_undo_log_encrypt", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_UNDO_LOG_TRUNCATE("innodb_undo_log_truncate", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_UNDO_TABLESPACES("innodb_undo_tablespaces", (r) -> r.getLongWithBoundaryBias(0, 127), Scope.GLOBAL), //
        INNODB_USE_FDATASYNC("innodb_use_fdatasync", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INSERT_ID("insert_id", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        INTERACTIVE_TIMEOUT("interactive_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        //INTERNAL_TMP_MEM_STORAGE_ENGINE("internal_tmp_mem_storage_engine", (r) -> Randomly.fromOptions("TempTable", "Memory"), Scope.GLOBAL, Scope.SESSION), //
        JOIN_BUFFER_SIZE("join_buffer_size", (r) -> r.getLongWithBoundaryBias(128, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        KEEP_FILES_ON_CREATE("keep_files_on_create", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        KEYRING_OPERATIONS("keyring_operations", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        KEY_BUFFER_SIZE("key_buffer_size", (r) -> r.getLongWithBoundaryBias(8, Long.MAX_VALUE), Scope.GLOBAL), //
        KEY_CACHE_AGE_THRESHOLD("key_cache_age_threshold", (r) -> r.getLongWithBoundaryBias(100, Long.MAX_VALUE), Scope.GLOBAL), //
        KEY_CACHE_BLOCK_SIZE("key_cache_block_size", (r) -> r.getLongWithBoundaryBias(512, 16384), Scope.GLOBAL), //
        KEY_CACHE_DIVISION_LIMIT("key_cache_division_limit", (r) -> r.getLongWithBoundaryBias(1, 100), Scope.GLOBAL), //
        //LAST_INSERT_ID("last_insert_id", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
//        LC_MESSAGES("lc_messages", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        LC_TIME_NAMES("lc_time_names", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        LOCAL_INFILE("local_infile", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOCK_WAIT_TIMEOUT("lock_wait_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        LOG_BIN_TRUST_FUNCTION_CREATORS("log_bin_trust_function_creators", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_ERROR_SERVICES("log_error_services",(r) -> Randomly.fromOptions(
                "'log_filter_internal; log_sink_internal'",
                "'log_filter_internal; log_sink_json'",
                "'log_filter_internal; log_sink_test'",
                "'log_filter_dragnet; log_sink_internal'",
                "'log_filter_dragnet; log_sink_json'"), Scope.GLOBAL), //

        LOG_ERROR_VERBOSITY("log_error_verbosity", (r) -> Randomly.fromOptions("1", "2", "3"), Scope.GLOBAL), //
        LOG_OUTPUT("log_output", (r) -> Randomly.fromOptions("'TABLE'", "'FILE'", "'NONE'"), Scope.GLOBAL), //
        LOG_QUERIES_NOT_USING_INDEXES("log_queries_not_using_indexes", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_RAW("log_raw", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_SLOW_ADMIN_STATEMENTS("log_slow_admin_statements", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_SLOW_EXTRA("log_slow_extra", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_SLOW_REPLICA_STATEMENTS("log_slow_replica_statements", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_SLOW_SLAVE_STATEMENTS("log_slow_slave_statements", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        LOG_STATEMENTS_UNSAFE_FOR_BINLOG("log_statements_unsafe_for_binlog", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_THROTTLE_QUERIES_NOT_USING_INDEXES("log_throttle_queries_not_using_indexes", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        LOG_TIMESTAMPS("log_timestamps", (r) -> Randomly.fromOptions("UTC", "SYSTEM"), Scope.GLOBAL), //
        LONG_QUERY_TIME("long_query_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        LOW_PRIORITY_UPDATES("low_priority_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        //MANDATORY_ROLES("mandatory_roles", (r) -> r.getString(), Scope.GLOBAL), //
        MASTER_VERIFY_CHECKSUM("master_verify_checksum", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        MAX_ALLOWED_PACKET("max_allowed_packet", (r) -> r.getLongWithBoundaryBias(1024, 1073741824), Scope.GLOBAL), //
        MAX_BINLOG_CACHE_SIZE("max_binlog_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        MAX_BINLOG_SIZE("max_binlog_size", (r) -> r.getLongWithBoundaryBias(4096, 1073741824), Scope.GLOBAL), //
        MAX_BINLOG_STMT_CACHE_SIZE("max_binlog_stmt_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        MAX_CONNECTIONS("max_connections", (r) -> r.getLongWithBoundaryBias(1, 100000), Scope.GLOBAL), //
        MAX_CONNECT_ERRORS("max_connect_errors", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        MAX_DELAYED_THREADS("max_delayed_threads", (r) -> r.getLongWithBoundaryBias(0, 16384), Scope.GLOBAL), //
        MAX_ERROR_COUNT("max_error_count", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL, Scope.SESSION), //
        MAX_EXECUTION_TIME("max_execution_time", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_HEAP_TABLE_SIZE("max_heap_table_size", (r) -> r.getLongWithBoundaryBias(16384, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_INSERT_DELAYED_THREADS("max_insert_delayed_threads", (r) -> r.getLongWithBoundaryBias(0, 16384), Scope.GLOBAL), //
        MAX_JOIN_SIZE("max_join_size", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_LENGTH_FOR_SORT_DATA("max_length_for_sort_data", (r) -> r.getLongWithBoundaryBias(4, 8388608), Scope.GLOBAL, Scope.SESSION), //
        MAX_POINTS_IN_GEOMETRY("max_points_in_geometry", (r) -> r.getLongWithBoundaryBias(3, 1048576), Scope.GLOBAL, Scope.SESSION), //
        MAX_PREPARED_STMT_COUNT("max_prepared_stmt_count", (r) -> r.getLongWithBoundaryBias(0, 1048576), Scope.GLOBAL), //
        MAX_RELAY_LOG_SIZE("max_relay_log_size", (r) -> r.getLongWithBoundaryBias(0, 1073741824), Scope.GLOBAL), //
        MAX_SEEKS_FOR_KEY("max_seeks_for_key", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_SORT_LENGTH("max_sort_length", (r) -> r.getLongWithBoundaryBias(4, 8388608), Scope.GLOBAL, Scope.SESSION), //
        MAX_SP_RECURSION_DEPTH("max_sp_recursion_depth", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL, Scope.SESSION), //
        MAX_USER_CONNECTIONS("max_user_connections", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        MAX_WRITE_LOCK_COUNT("max_write_lock_count", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL), //
        MIN_EXAMINED_ROW_LIMIT("min_examined_row_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MYSQL_NATIVE_PASSWORD_PROXY_USERS("mysql_native_password_proxy_users", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        NET_BUFFER_LENGTH("net_buffer_length", (r) -> r.getLongWithBoundaryBias(1024, 1048576), Scope.GLOBAL), //
        NET_READ_TIMEOUT("net_read_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        NET_RETRY_COUNT("net_retry_count", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        NET_WRITE_TIMEOUT("net_write_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        OFFLINE_MODE("offline_mode", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        OLD_ALTER_TABLE("old_alter_table", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_PRUNE_LEVEL("optimizer_prune_level", (r) -> Randomly.fromOptions("0", "1"), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_SEARCH_DEPTH("optimizer_search_depth", (r) -> r.getLongWithBoundaryBias(0, 62), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_SWITCH("optimizer_switch",  (r) -> getOptimizerSwitchConfiguration(r), Scope.GLOBAL, Scope.SESSION), //
        // OPTIMIZER_TRACE("optimizer_trace", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        // OPTIMIZER_TRACE_FEATURES("optimizer_trace_features", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_TRACE_LIMIT("optimizer_trace_limit", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_TRACE_MAX_MEM_SIZE("optimizer_trace_max_mem_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_TRACE_OFFSET("optimizer_trace_offset", (r) -> r.getLongWithBoundaryBias(-2147483648, 2147483647), Scope.GLOBAL, Scope.SESSION), //
        ORIGINAL_COMMIT_TIMESTAMP("original_commit_timestamp", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        PARSER_MAX_MEM_SIZE("parser_max_mem_size", (r) -> r.getLongWithBoundaryBias(1000000, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        PARTIAL_REVOKES("partial_revokes", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        PASSWORD_HISTORY("password_history", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        PASSWORD_REQUIRE_CURRENT("password_require_current", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        PASSWORD_REUSE_INTERVAL("password_reuse_interval", (r) -> r.getLongWithBoundaryBias(0, 32767), Scope.GLOBAL), //
        PERFORMANCE_SCHEMA_MAX_DIGEST_SAMPLE_AGE("performance_schema_max_digest_sample_age", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        PERFORMANCE_SCHEMA_SHOW_PROCESSLIST("performance_schema_show_processlist", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        PRELOAD_BUFFER_SIZE("preload_buffer_size", (r) -> r.getLongWithBoundaryBias(1024, 1073741824), Scope.GLOBAL, Scope.SESSION), //
        PRINT_IDENTIFIED_WITH_AS_HEX("print_identified_with_as_hex", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        PROFILING("profiling", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        PROFILING_HISTORY_SIZE("profiling_history_size", (r) -> r.getLongWithBoundaryBias(0, 100), Scope.GLOBAL, Scope.SESSION), //
        PROTOCOL_COMPRESSION_ALGORITHMS("protocol_compression_algorithms", (r) -> Randomly.fromOptions("zlib", "zstd", "uncompressed"), Scope.GLOBAL), //
        PSEUDO_REPLICA_MODE("pseudo_replica_mode", (r) -> Randomly.fromOptions("OFF", "ON"),  Scope.SESSION), //
        PSEUDO_SLAVE_MODE("pseudo_slave_mode", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        PSEUDO_THREAD_ID("pseudo_thread_id", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        QUERY_ALLOC_BLOCK_SIZE("query_alloc_block_size", (r) -> r.getLongWithBoundaryBias(1024, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        QUERY_PREALLOC_SIZE("query_prealloc_size", (r) -> r.getLongWithBoundaryBias(8192, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        RANGE_ALLOC_BLOCK_SIZE("range_alloc_block_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        RANGE_OPTIMIZER_MAX_MEM_SIZE("range_optimizer_max_mem_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        // RBR_EXEC_MODE("rbr_exec_mode", (r) -> Randomly.fromOptions("STRICT", "IDEMPOTENT"), Scope.GLOBAL, Scope.SESSION), //
        READ_BUFFER_SIZE("read_buffer_size", (r) -> r.getLongWithBoundaryBias(8192, 2147479552), Scope.GLOBAL, Scope.SESSION), //
        READ_ONLY("read_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        READ_RND_BUFFER_SIZE("read_rnd_buffer_size", (r) -> r.getLongWithBoundaryBias(1, 2147483647), Scope.GLOBAL, Scope.SESSION), //
        REGEXP_STACK_LIMIT("regexp_stack_limit", (r) -> r.getLongWithBoundaryBias(0, 2147483647), Scope.GLOBAL), //
        REGEXP_TIME_LIMIT("regexp_time_limit", (r) -> r.getLongWithBoundaryBias(0, 2147483647), Scope.GLOBAL), //
        RELAY_LOG_PURGE("relay_log_purge", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        REQUIRE_ROW_FORMAT("require_row_format", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        REQUIRE_SECURE_TRANSPORT("require_secure_transport", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //RESULTSET_METADATA("resultset_metadata", (r) -> Randomly.fromOptions("FULL", "NONE"),Scope.SESSION), //
        RPL_READ_SIZE("rpl_read_size", (r) -> r.getLongWithBoundaryBias(8192, 16777216), Scope.GLOBAL), //
        RPL_STOP_REPLICA_TIMEOUT("rpl_stop_replica_timeout", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        RPL_STOP_SLAVE_TIMEOUT("rpl_stop_slave_timeout", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        SCHEMA_DEFINITION_CACHE("schema_definition_cache", (r) -> r.getLongWithBoundaryBias(256, 524288), Scope.GLOBAL), //
        //SECONDARY_ENGINE_COST_THRESHOLD("secondary_engine_cost_threshold", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        SELECT_INTO_BUFFER_SIZE("select_into_buffer_size", (r) -> r.getLongWithBoundaryBias(8192, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        SELECT_INTO_DISK_SYNC("select_into_disk_sync", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SELECT_INTO_DISK_SYNC_DELAY("select_into_disk_sync_delay", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        SERVER_ID("server_id", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        SESSION_TRACK_GTIDS("session_track_gtids", (r) -> Randomly.fromOptions("OFF", "OWN_GTID", "ALL_GTIDS"), Scope.GLOBAL, Scope.SESSION), //
        SESSION_TRACK_SCHEMA("session_track_schema", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SESSION_TRACK_STATE_CHANGE("session_track_state_change", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SESSION_TRACK_SYSTEM_VARIABLES("session_track_system_variables", (r) -> Randomly.fromOptions("time_zone", "autocommit", "character_set_client", "character_set_results", "character_set_connection"), Scope.GLOBAL, Scope.SESSION), //
        SESSION_TRACK_TRANSACTION_INFO("session_track_transaction_info", (r) -> Randomly.fromOptions("OFF", "STATE", "CHARACTERISTICS"), Scope.GLOBAL, Scope.SESSION), //
        SHA256_PASSWORD_PROXY_USERS("sha256_password_proxy_users", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //SHOW_CREATE_TABLE_SKIP_SECONDARY_ENGINE("show_create_table_skip_secondary_engine", (r) -> Randomly.fromOptions("OFF", "ON"),  Scope.SESSION), //
        SHOW_CREATE_TABLE_VERBOSITY("show_create_table_verbosity", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        SHOW_GIPK_IN_CREATE_TABLE_AND_INFORMATION_SCHEMA("show_gipk_in_create_table_and_information_schema", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SLOW_LAUNCH_TIME("slow_launch_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        SLOW_QUERY_LOG("slow_query_log", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //SLOW_QUERY_LOG_FILE("slow_query_log_file", (r) -> r.getString(), Scope.GLOBAL), //
        SORT_BUFFER_SIZE("sort_buffer_size", (r) -> r.getLongWithBoundaryBias(32768, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        SOURCE_VERIFY_CHECKSUM("source_verify_checksum", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        SQL_AUTO_IS_NULL("sql_auto_is_null", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_BIG_SELECTS("sql_big_selects", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_BUFFER_RESULT("sql_buffer_result", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_GENERATE_INVISIBLE_PRIMARY_KEY("sql_generate_invisible_primary_key", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_LOG_BIN("sql_log_bin", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        SQL_LOG_OFF("sql_log_off", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        SQL_MODE("sql_mode", (r) -> {
            List<String> modes = Arrays.asList(
                    "ANSI_QUOTES", "ERROR_FOR_DIVISION_BY_ZERO",
                    "HIGH_NOT_PRECEDENCE",
                    "IGNORE_SPACE",
                    "NO_AUTO_VALUE_ON_ZERO",
                    "NO_BACKSLASH_ESCAPES",
                    "NO_DIR_IN_CREATE",
                    "NO_ENGINE_SUBSTITUTION",
                    "NO_UNSIGNED_SUBTRACTION",
                    "ONLY_FULL_GROUP_BY",
                    "PAD_CHAR_TO_FULL_LENGTH",
                    "PIPES_AS_CONCAT",
                    "REAL_AS_FLOAT",
                    "STRICT_ALL_TABLES",
                    "STRICT_TRANS_TABLES",
                    "TIME_TRUNCATE_FRACTIONAL"
            );
            return "'"+String.join(",", Randomly.nonEmptySubset(modes))+"'";
        }, Scope.GLOBAL, Scope.SESSION), //
        SQL_NOTES("sql_notes", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_QUOTE_SHOW_CREATE("sql_quote_show_create", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_REPLICA_SKIP_COUNTER("sql_replica_skip_counter", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SQL_REQUIRE_PRIMARY_KEY("sql_require_primary_key", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_SAFE_UPDATES("sql_safe_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_SELECT_LIMIT("sql_select_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        SQL_SLAVE_SKIP_COUNTER("sql_slave_skip_counter", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SQL_WARNINGS("sql_warnings", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        STORED_PROGRAM_CACHE("stored_program_cache", (r) -> r.getLongWithBoundaryBias(16, 524288), Scope.GLOBAL), //
        STORED_PROGRAM_DEFINITION_CACHE("stored_program_definition_cache", (r) -> r.getLongWithBoundaryBias(256, 524288), Scope.GLOBAL), //
        SUPER_READ_ONLY("super_read_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        SYNC_BINLOG("sync_binlog", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_MASTER_INFO("sync_master_info", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_RELAY_LOG("sync_relay_log", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_RELAY_LOG_INFO("sync_relay_log_info", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_SOURCE_INFO("sync_source_info", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        TABLESPACE_DEFINITION_CACHE("tablespace_definition_cache", (r) -> r.getLongWithBoundaryBias(256, 524288), Scope.GLOBAL), //
        TABLE_DEFINITION_CACHE("table_definition_cache", (r) -> r.getLongWithBoundaryBias(400, 524288), Scope.GLOBAL), //
        TABLE_ENCRYPTION_PRIVILEGE_CHECK("table_encryption_privilege_check", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        TABLE_OPEN_CACHE("table_open_cache", (r) -> r.getLongWithBoundaryBias(1, 524288), Scope.GLOBAL), //
        TEMPTABLE_MAX_MMAP("temptable_max_mmap", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        TEMPTABLE_MAX_RAM("temptable_max_ram", (r) -> r.getLongWithBoundaryBias(2097152, Long.MAX_VALUE), Scope.GLOBAL), //
        TEMPTABLE_USE_MMAP("temptable_use_mmap", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        TERMINOLOGY_USE_PREVIOUS("terminology_use_previous", (r) -> Randomly.fromOptions("NONE", "BEFORE_8_0_26"), Scope.GLOBAL, Scope.SESSION), //
        THREAD_CACHE_SIZE("thread_cache_size", (r) -> r.getLongWithBoundaryBias(0, 16384), Scope.GLOBAL), //
        TIMESTAMP("timestamp", (r) -> r.getLongWithBoundaryBias(0, 2147483647), Scope.SESSION), //
        //TIME_ZONE("time_zone", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        //TLS_CIPHERSUITES("tls_ciphersuites", (r) -> r.getString(), Scope.GLOBAL), //
        TLS_VERSION("tls_version", (r) -> Randomly.fromOptions("'TLSv1.2'", "'TLSv1.3'"), Scope.GLOBAL), //
        TMP_TABLE_SIZE("tmp_table_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        TRANSACTION_ALLOC_BLOCK_SIZE("transaction_alloc_block_size", (r) -> r.getLongWithBoundaryBias(1024, 131072), Scope.GLOBAL, Scope.SESSION), //
        TRANSACTION_ALLOW_BATCHING("transaction_allow_batching", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        TRANSACTION_ISOLATION("transaction_isolation", (r) -> Randomly.fromOptions("'READ-UNCOMMITTED'", "'READ-COMMITTED'", "'REPEATABLE-READ'", "'SERIALIZABLE'"), Scope.GLOBAL, Scope.SESSION), //
        TRANSACTION_PREALLOC_SIZE("transaction_prealloc_size", (r) -> r.getLongWithBoundaryBias(1024, 131072), Scope.GLOBAL, Scope.SESSION), //
        TRANSACTION_READ_ONLY("transaction_read_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        UNIQUE_CHECKS("unique_checks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        UPDATABLE_VIEWS_WITH_LIMIT("updatable_views_with_limit", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL, Scope.SESSION), //
        //USE_SECONDARY_ENGINE("use_secondary_engine", (r) -> Randomly.fromOptions("OFF", "ON", "FORCED"), Scope.GLOBAL, Scope.SESSION), //
        WAIT_TIMEOUT("wait_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        WINDOWING_USE_HIGH_PRECISION("windowing_use_high_precision", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        XA_DETACH_ON_PREPARE("xa_detach_on_prepare", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION);//
     // TODO: https://dev.mysql.com/doc/refman/8.0/en/switchable-optimizations.html

        private final GenericAction delegate;

        Action(String name, Function<Randomly, Object> prod, Scope... scopes) {
            this.delegate = new GenericAction(name, prod, scopes);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object generateValue(Randomly r) {
            return delegate.generateValue(r);
        }

        @Override
        public Scope[] getScopes() {
            return delegate.getScopes();
        }

        @Override
        public boolean canBeUsedInScope(Scope scope) {
            return delegate.canBeUsedInScope(scope);
        }

        /*
         * @see https://dev.mysql.com/doc/refman/8.0/en/switchable-optimizations.html
         */
        private static String getOptimizerSwitchConfiguration(Randomly r) {
            StringBuilder sb = new StringBuilder();
            sb.append("'");
            String[] options = { "index_merge", "index_merge_union", "index_merge_sort_union",
                    "index_merge_intersection", "index_condition_pushdown", "mrr", "mrr_cost_based",
                    "block_nested_loop", "batched_key_access", "materialization", "semijoin", "loosescan", "firstmatch",
                    "duplicateweedout", "subquery_materialization_cost_based", "use_index_extensions",
                    "condition_fanout_filter", "derived_merge", "use_invisible_indexes", "skip_scan", "hash_join",
                    "subquery_to_derived", "prefer_ordering_index", "derived_condition_pushdown" };
            List<String> optionSubset = Randomly.nonEmptySubset(options);
            sb.append(optionSubset.stream().map(s -> s + "=" + Randomly.fromOptions("on", "off"))
                    .collect(Collectors.joining(",")));
            sb.append("'");
            return sb.toString();
        }
    }


}
